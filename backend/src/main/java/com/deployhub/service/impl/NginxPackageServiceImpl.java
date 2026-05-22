package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.entity.NginxPackage;
import com.deployhub.entity.Server;
import com.deployhub.mapper.NginxPackageMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.NginxPackageService;
import com.deployhub.ssh.SshClient;
import com.deployhub.ssh.SshExecuteResult;
import com.deployhub.ssh.SshPool;
import com.deployhub.util.PasswordUtil;
import com.deployhub.vo.NginxPackageVO;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class NginxPackageServiceImpl implements NginxPackageService {

    @Autowired
    private NginxPackageMapper nginxPackageMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private SshPool sshPool;

    @Value("${deploy.upload.path:./uploads}")
    private String uploadBasePath;

    @Override
    public Result<PageResult<NginxPackageVO>> list(int pageNum, int pageSize, String name) {
        Page<NginxPackage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<NginxPackage> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(NginxPackage::getName, name);
        }
        wrapper.orderByDesc(NginxPackage::getCreateTime);
        Page<NginxPackage> result = nginxPackageMapper.selectPage(page, wrapper);

        PageResult<NginxPackageVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(BeanUtil.copyToList(result.getRecords(), NginxPackageVO.class));
        return Result.success(pageResult);
    }

    @Override
    public Result<NginxPackageVO> getById(Long id) {
        NginxPackage pkg = nginxPackageMapper.selectById(id);
        if (pkg == null) {
            return Result.error("Nginx安装包不存在");
        }
        return Result.success(BeanUtil.copyProperties(pkg, NginxPackageVO.class));
    }

    @Override
    public Result<Void> upload(MultipartFile file, String name, String version, String description) {
        try {
            // 保存文件到 uploads/nginx/ 目录，使用项目根目录的绝对路径
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "nginx");
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String fileName = (originalFilename != null && !originalFilename.isEmpty())
                    ? originalFilename : name + "-" + version + ".tar.gz";
            // 清理文件名中的非法字符
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path destPath = uploadPath.resolve(fileName);
            file.transferTo(destPath.toFile());

            // 创建数据库记录
            NginxPackage pkg = new NginxPackage();
            pkg.setName(name);
            pkg.setVersion(version);
            pkg.setFilePath(destPath.toString());
            pkg.setFileSize(file.getSize());
            pkg.setDescription(description);
            nginxPackageMapper.insert(pkg);

            return Result.success();
        } catch (IOException e) {
            log.error("上传Nginx安装包失败", e);
            return Result.error("上传Nginx安装包失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> delete(Long id) {
        NginxPackage pkg = nginxPackageMapper.selectById(id);
        if (pkg != null && pkg.getFilePath() != null) {
            // 删除本地文件
            File file = new File(pkg.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
        nginxPackageMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<String> installToServer(Long packageId, Long serverId) {
        NginxPackage pkg = nginxPackageMapper.selectById(packageId);
        if (pkg == null) {
            return Result.error("Nginx安装包不存在");
        }
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            return Result.error("目标服务器不存在");
        }

        String host = server.getHost();
        int port = server.getPort() != null ? server.getPort() : 22;
        Session session = null;
        try {
            session = sshPool.borrowSession(
                    host, port,
                    server.getUsername(),
                    PasswordUtil.decrypt(server.getPassword()),
                    server.getPrivateKey()
            );

            // 上传安装包到服务器 /tmp/ 目录
            File localFile = new File(pkg.getFilePath());
            if (!localFile.exists()) {
                return Result.error("安装包文件不存在: " + pkg.getFilePath());
            }

            String remoteFileName = localFile.getName();
            String remoteTempPath = "/tmp/" + remoteFileName;

            // 通过SFTP上传
            boolean uploadSuccess = SshClient.uploadFile(session, pkg.getFilePath(), remoteTempPath);
            if (!uploadSuccess) {
                return Result.error("上传安装包到服务器失败");
            }

            // 先安装编译依赖，再解压编译安装
            String installCommand =
                    // 检测包管理器并安装编译依赖
                    "if command -v yum &>/dev/null; then yum install -y gcc make pcre-devel zlib-devel openssl-devel; " +
                    "elif command -v apt-get &>/dev/null; then apt-get update -qq && apt-get install -y build-essential libpcre3-dev zlib1g-dev libssl-dev; " +
                    "elif command -v dnf &>/dev/null; then dnf install -y gcc make pcre-devel zlib-devel openssl-devel; " +
                    "fi && " +
                    // 解压
                    "cd /tmp && tar -xzf " + remoteFileName +
                    " && NGINX_SRC=$(find /tmp -maxdepth 1 -type d -name 'nginx-*' -newer /tmp -print -quit 2>/dev/null)" +
                    " && if [ -z \"$NGINX_SRC\" ]; then NGINX_SRC=$(ls -d /tmp/nginx-*/ 2>/dev/null | head -1); fi" +
                    " && if [ -n \"$NGINX_SRC\" ]; then cd $NGINX_SRC && ./configure --prefix=/usr/local/nginx && make -j$(nproc) && make install; else echo 'ERROR: Cannot find extracted nginx source directory'; exit 1; fi 2>&1";
            SshExecuteResult result = SshClient.executeCommand(session, installCommand);

            // 清理临时文件
            SshClient.executeCommand(session, "rm -f " + remoteTempPath);

            if (result.isSuccess()) {
                return Result.success("Nginx安装成功\n" + result.getOutput());
            } else {
                return Result.error("Nginx安装失败: " + result.getError());
            }
        } catch (Exception e) {
            log.error("安装Nginx到服务器失败, packageId={}, serverId={}", packageId, serverId, e);
            return Result.error("安装Nginx到服务器失败: " + e.getMessage());
        } finally {
            if (session != null) {
                sshPool.returnSession(host, port, session);
            }
        }
    }
}
