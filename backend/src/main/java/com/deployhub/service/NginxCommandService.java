package com.deployhub.service;

import com.deployhub.entity.Server;
import com.deployhub.ssh.SshClient;
import com.deployhub.ssh.SshExecuteResult;
import com.deployhub.ssh.SshPool;
import com.deployhub.util.PasswordUtil;
import com.deployhub.vo.NginxServerConfigVO;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nginx命令服务类
 * 封装所有Nginx相关的SSH远程命令操作
 *
 * @author MSI-1
 * @date 2026-05-21
 */
@Slf4j
@Service
public class NginxCommandService {

    @Autowired
    private SshPool sshPool;

    /**
     * 测试Nginx配置是否正确
     * 优先使用编译安装路径，回退到系统路径
     *
     * @param server 目标服务器
     * @return 命令执行结果
     */
    public SshExecuteResult testConfig(Server server) {
        Session session = null;
        try {
            session = borrowSession(server);
            // 同时尝试编译安装路径和系统路径
            String command = "/usr/local/nginx/sbin/nginx -t 2>&1 || nginx -t 2>&1";
            return SshClient.executeCommand(session, command);
        } catch (Exception e) {
            log.error("测试Nginx配置失败, server={}", server.getHost(), e);
            return new SshExecuteResult(-1, "", "测试Nginx配置失败: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 重新加载Nginx配置
     * 优先使用编译安装路径，回退到系统路径
     *
     * @param server 目标服务器
     * @return 命令执行结果
     */
    public SshExecuteResult reload(Server server) {
        Session session = null;
        try {
            session = borrowSession(server);
            // 同时尝试编译安装路径和系统路径
            String command = "/usr/local/nginx/sbin/nginx -s reload 2>&1 || nginx -s reload 2>&1";
            return SshClient.executeCommand(session, command);
        } catch (Exception e) {
            log.error("重载Nginx配置失败, server={}", server.getHost(), e);
            return new SshExecuteResult(-1, "", "重载Nginx配置失败: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 重启Nginx服务
     * 优先使用systemctl，若不可用则通过kill+start方式重启
     *
     * @param server 目标服务器
     * @return 命令执行结果
     */
    public SshExecuteResult restart(Server server) {
        Session session = null;
        try {
            session = borrowSession(server);
            // 优先尝试systemctl
            SshExecuteResult result = SshClient.executeCommand(session, "systemctl restart nginx 2>&1");
            if (result.isSuccess()) {
                return result;
            }
            // systemctl不可用时，通过kill+start方式重启
            log.info("systemctl不可用，尝试kill+start方式重启Nginx, server={}", server.getHost());
            String killAndStartCommand =
                    "kill $(cat /usr/local/nginx/logs/nginx.pid 2>/dev/null || pgrep nginx) 2>/dev/null; " +
                    "sleep 1; " +
                    "/usr/local/nginx/sbin/nginx 2>&1 || nginx 2>&1";
            return SshClient.executeCommand(session, killAndStartCommand);
        } catch (Exception e) {
            log.error("重启Nginx失败, server={}", server.getHost(), e);
            return new SshExecuteResult(-1, "", "重启Nginx失败: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 将配置内容写入服务器指定路径
     * 先写入临时文件，备份原配置，再移动到目标路径
     *
     * @param server        目标服务器
     * @param configPath    配置文件在服务器上的绝对路径
     * @param configContent 配置文件内容
     * @return 命令执行结果
     */
    public SshExecuteResult writeConfig(Server server, String configPath, String configContent) {
        Session session = null;
        try {
            session = borrowSession(server);

            // 第一步：将配置内容写入临时文件
            String tempFile = "/tmp/nginx_config_temp";
            String writeCommand = "cat > " + tempFile + " << 'DEPLOYHUB_EOF'\n" + configContent + "\nDEPLOYHUB_EOF";
            SshExecuteResult writeResult = SshClient.executeCommand(session, writeCommand);
            if (!writeResult.isSuccess()) {
                log.error("写入临时配置文件失败, server={}, error={}", server.getHost(), writeResult.getError());
                return writeResult;
            }

            // 第二步：备份已有配置文件（忽略错误，文件可能不存在）
            String backupCommand = "cp " + configPath + " " + configPath + ".bak.$(date +%Y%m%d_%H%M%S) 2>/dev/null; true";
            SshClient.executeCommand(session, backupCommand);

            // 第三步：将临时文件移动到目标路径
            String moveCommand = "mv " + tempFile + " " + configPath;
            SshExecuteResult moveResult = SshClient.executeCommand(session, moveCommand);
            if (!moveResult.isSuccess()) {
                log.error("移动配置文件失败, server={}, error={}", server.getHost(), moveResult.getError());
                return moveResult;
            }

            log.info("配置文件写入成功, server={}, path={}", server.getHost(), configPath);
            return moveResult;
        } catch (Exception e) {
            log.error("写入Nginx配置失败, server={}, path={}", server.getHost(), configPath, e);
            return new SshExecuteResult(-1, "", "写入Nginx配置失败: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 检查Nginx是否已安装
     *
     * @param server 目标服务器
     * @return true表示已安装
     */
    public boolean checkNginxInstalled(Server server) {
        Session session = null;
        try {
            session = borrowSession(server);
            String command = "which nginx 2>/dev/null || test -f /usr/local/nginx/sbin/nginx && echo 'FOUND'";
            SshExecuteResult result = SshClient.executeCommand(session, command);
            String output = result.getOutput();
            return output != null && (output.contains("FOUND") || output.contains("/nginx"));
        } catch (Exception e) {
            log.error("检查Nginx安装状态失败, server={}", server.getHost(), e);
            return false;
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 获取Nginx版本号
     *
     * @param server 目标服务器
     * @return 版本字符串，获取失败返回"unknown"
     */
    public String getNginxVersion(Server server) {
        Session session = null;
        try {
            session = borrowSession(server);
            // nginx -v 输出到stderr，需要重定向
            String command = "nginx -v 2>&1 || /usr/local/nginx/sbin/nginx -v 2>&1";
            SshExecuteResult result = SshClient.executeCommand(session, command);
            if (result.isSuccess() || result.getOutput() != null) {
                String output = result.getOutput().trim();
                // 解析版本号，如 "nginx version: nginx/1.24.0"
                if (output.contains("/")) {
                    int idx = output.lastIndexOf("/");
                    return output.substring(idx + 1).trim();
                }
                return output;
            }
            return "unknown";
        } catch (Exception e) {
            log.error("获取Nginx版本失败, server={}", server.getHost(), e);
            return "unknown";
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 从上传的源码包编译安装Nginx
     *
     * @param server          目标服务器
     * @param packageFileName 已上传到服务器/tmp/目录的压缩包文件名
     * @return 命令执行结果
     */
    public SshExecuteResult installFromPackage(Server server, String packageFileName) {
        Session session = null;
        try {
            session = borrowSession(server);
            // 先安装编译依赖（gcc、make、pcre、zlib、openssl），再解压编译安装
            String command =
                    // 检测包管理器并安装编译依赖
                    "if command -v yum &>/dev/null; then yum install -y gcc make pcre-devel zlib-devel openssl-devel; " +
                    "elif command -v apt-get &>/dev/null; then apt-get update -qq && apt-get install -y build-essential libpcre3-dev zlib1g-dev libssl-dev; " +
                    "elif command -v dnf &>/dev/null; then dnf install -y gcc make pcre-devel zlib-devel openssl-devel; " +
                    "fi && " +
                    // 解压
                    "cd /tmp && tar -xzf " + packageFileName + " && " +
                    // 动态检测解压目录
                    "NGINX_SRC=$(find /tmp -maxdepth 1 -type d -name 'nginx-*' -newer /tmp -print -quit 2>/dev/null) && " +
                    "if [ -z \"$NGINX_SRC\" ]; then NGINX_SRC=$(ls -d /tmp/nginx-*/ 2>/dev/null | head -1); fi && " +
                    // 编译安装
                    "if [ -n \"$NGINX_SRC\" ]; then cd $NGINX_SRC && ./configure --prefix=/usr/local/nginx && make -j$(nproc) && make install; else echo 'ERROR: Cannot find extracted nginx source directory'; exit 1; fi 2>&1";
            SshExecuteResult result = SshClient.executeCommand(session, command);
            if (result.isSuccess()) {
                log.info("Nginx编译安装成功, server={}", server.getHost());
            } else {
                log.error("Nginx编译安装失败, server={}, error={}", server.getHost(), result.getError());
            }
            return result;
        } catch (Exception e) {
            log.error("Nginx编译安装异常, server={}", server.getHost(), e);
            return new SshExecuteResult(-1, "", "Nginx编译安装异常: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 读取服务器上的Nginx配置信息
     * 检测安装状态、版本号，读取主配置和conf.d目录下的配置文件
     *
     * @param server 目标服务器
     * @return Nginx配置信息VO
     */
    public NginxServerConfigVO readNginxConfig(Server server) {
        NginxServerConfigVO vo = new NginxServerConfigVO();

        // 检查是否安装了Nginx
        boolean installed = checkNginxInstalled(server);
        vo.setInstalled(installed);
        if (!installed) {
            return vo;
        }

        // 获取版本号
        vo.setVersion(getNginxVersion(server));

        Session session = null;
        try {
            session = borrowSession(server);

            // 通过 nginx -t 输出获取主配置文件路径
            String configPath = detectConfigFilePath(session);
            vo.setConfigFilePath(configPath);

            // 读取主配置文件内容
            if (configPath != null) {
                SshExecuteResult catResult = SshClient.executeCommand(session, "cat " + configPath + " 2>/dev/null");
                if (catResult.isSuccess() && catResult.getOutput() != null) {
                    vo.setMainConfigContent(catResult.getOutput());
                }
            }

            // 读取conf.d目录下的配置文件
            List<NginxServerConfigVO.ConfigFileInfo> confFiles = readConfDir(session, configPath);
            vo.setConfFiles(confFiles);

        } catch (Exception e) {
            log.error("读取Nginx配置失败, server={}", server.getHost(), e);
        } finally {
            returnSession(server, session);
        }

        return vo;
    }

    /**
     * 通过 nginx -t 输出检测主配置文件路径
     * 输出格式示例：test is successful - configuration file /usr/local/nginx/conf/nginx.conf syntax is ok
     */
    private static final Pattern CONFIG_PATH_PATTERN = Pattern.compile("configuration file (\\S+)");

    private String detectConfigFilePath(Session session) {
        try {
            SshExecuteResult testResult = SshClient.executeCommand(session,
                    "/usr/local/nginx/sbin/nginx -t 2>&1 || nginx -t 2>&1");
            String output = testResult.getOutput();
            if (output != null) {
                Matcher matcher = CONFIG_PATH_PATTERN.matcher(output);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            log.error("检测Nginx配置文件路径失败", e);
        }
        // 回退到默认路径
        return "/usr/local/nginx/conf/nginx.conf";
    }

    /**
     * 读取conf.d目录下的所有.conf文件
     * 同时检查编译安装路径和系统安装路径
     */
    private List<NginxServerConfigVO.ConfigFileInfo> readConfDir(Session session, String mainConfigPath) {
        List<NginxServerConfigVO.ConfigFileInfo> confFiles = new ArrayList<>();

        // 根据主配置路径推断conf.d目录
        String confDir = null;
        if (mainConfigPath != null) {
            int lastSlash = mainConfigPath.lastIndexOf('/');
            if (lastSlash > 0) {
                confDir = mainConfigPath.substring(0, lastSlash) + "/conf.d";
            }
        }

        // 尝试多个可能的conf.d路径
        String[] possibleDirs = {confDir, "/usr/local/nginx/conf/conf.d", "/etc/nginx/conf.d"};
        String validDir = null;

        for (String dir : possibleDirs) {
            if (dir == null) continue;
            SshExecuteResult checkResult = SshClient.executeCommand(session, "test -d " + dir + " && echo 'EXISTS'");
            if (checkResult.isSuccess() && checkResult.getOutput() != null && checkResult.getOutput().contains("EXISTS")) {
                validDir = dir;
                break;
            }
        }

        if (validDir == null) {
            return confFiles;
        }

        // 列出conf.d目录下的所有.conf文件
        SshExecuteResult listResult = SshClient.executeCommand(session,
                "ls -1 " + validDir + "/*.conf 2>/dev/null");
        if (!listResult.isSuccess() || listResult.getOutput() == null || listResult.getOutput().trim().isEmpty()) {
            return confFiles;
        }

        String[] files = listResult.getOutput().trim().split("\n");
        for (String filePath : files) {
            filePath = filePath.trim();
            if (filePath.isEmpty()) continue;

            // 读取文件内容
            SshExecuteResult catResult = SshClient.executeCommand(session, "cat " + filePath + " 2>/dev/null");
            NginxServerConfigVO.ConfigFileInfo fileInfo = new NginxServerConfigVO.ConfigFileInfo();
            fileInfo.setFilePath(filePath);
            // 提取文件名
            int idx = filePath.lastIndexOf('/');
            fileInfo.setFileName(idx >= 0 ? filePath.substring(idx + 1) : filePath);
            fileInfo.setContent(catResult.isSuccess() ? catResult.getOutput() : "");
            confFiles.add(fileInfo);
        }

        return confFiles;
    }

    /**
     * 上传安装包到服务器/tmp/目录
     *
     * @param server         目标服务器
     * @param localFilePath  本地文件路径
     * @param remoteFileName 远程文件名（将上传到/tmp/{remoteFileName}）
     * @return 上传结果
     */
    public SshExecuteResult uploadPackage(Server server, String localFilePath, String remoteFileName) {
        Session session = null;
        try {
            session = borrowSession(server);
            String remotePath = "/tmp/" + remoteFileName;
            boolean success = SshClient.uploadFile(session, localFilePath, remotePath);
            if (success) {
                log.info("安装包上传成功, server={}, remotePath={}", server.getHost(), remotePath);
                return new SshExecuteResult(0, "上传成功: " + remotePath, "");
            } else {
                log.error("安装包上传失败, server={}, remotePath={}", server.getHost(), remotePath);
                return new SshExecuteResult(-1, "", "安装包上传失败");
            }
        } catch (Exception e) {
            log.error("安装包上传异常, server={}, localPath={}", server.getHost(), localFilePath, e);
            return new SshExecuteResult(-1, "", "安装包上传异常: " + e.getMessage());
        } finally {
            returnSession(server, session);
        }
    }

    /**
     * 从服务器借出SSH会话
     *
     * @param server 服务器实体
     * @return SSH Session
     * @throws Exception 借出会话异常
     */
    private Session borrowSession(Server server) throws Exception {
        String password = server.getPassword();
        if (password != null && !password.isEmpty()) {
            password = PasswordUtil.decrypt(password);
        }
        return sshPool.borrowSession(
                server.getHost(),
                server.getPort() != null ? server.getPort() : 22,
                server.getUsername(),
                password,
                server.getPrivateKey()
        );
    }

    /**
     * 归还SSH会话到连接池
     *
     * @param server  服务器实体
     * @param session SSH会话
     */
    private void returnSession(Server server, Session session) {
        if (session != null) {
            try {
                sshPool.returnSession(
                        server.getHost(),
                        server.getPort() != null ? server.getPort() : 22,
                        session
                );
            } catch (Exception e) {
                log.error("归还SSH会话失败, server={}", server.getHost(), e);
            }
        }
    }
}
