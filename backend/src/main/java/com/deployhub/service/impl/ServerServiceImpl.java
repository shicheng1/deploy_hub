package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.deployhub.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ServerDTO;
import com.deployhub.entity.Server;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.ServerService;
import com.deployhub.ssh.SshClient;
import com.deployhub.ssh.SshExecuteResult;
import com.deployhub.vo.ServerVO;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServerServiceImpl implements ServerService {

    @Autowired
    private ServerMapper serverMapper;

    @Override
    public Result<PageResult<ServerVO>> list(int pageNum, int pageSize, String name, String groupName) {
        Page<Server> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Server::getName, name);
        }
        if (groupName != null && !groupName.isEmpty()) {
            wrapper.eq(Server::getGroupName, groupName);
        }
        wrapper.orderByDesc(Server::getCreateTime);
        Page<Server> result = serverMapper.selectPage(page, wrapper);

        PageResult<ServerVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(BeanUtil.copyToList(result.getRecords(), ServerVO.class));
        return Result.success(pageResult);
    }

    @Override
    public Result<ServerVO> getById(Long id) {
        Server server = serverMapper.selectById(id);
        if (server == null) {
            return Result.error("服务器不存在");
        }
        return Result.success(BeanUtil.copyProperties(server, ServerVO.class));
    }

    @Override
    public Result<Void> add(ServerDTO dto) {
        Server server = BeanUtil.copyProperties(dto, Server.class);
        if (server.getPort() == null) {
            server.setPort(22);
        }
        if (server.getPassword() != null && !server.getPassword().isEmpty()) {
            server.setPassword(PasswordUtil.encrypt(server.getPassword()));
        }
        serverMapper.insert(server);
        return Result.success();
    }

    @Override
    public Result<Void> update(ServerDTO dto) {
        Server server = serverMapper.selectById(dto.getId());
        if (server == null) {
            return Result.error("服务器不存在");
        }
        BeanUtil.copyProperties(dto, server, "id", "createTime", "updateTime", "deleted");
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            server.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        }
        serverMapper.updateById(server);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        serverMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<Boolean> testConnection(Long id) {
        Server server = serverMapper.selectById(id);
        if (server == null) {
            return Result.error("服务器不存在");
        }
        try {
            Session session = SshClient.createSession(
                    server.getHost(),
                    server.getPort() != null ? server.getPort() : 22,
                    server.getUsername(),
                    PasswordUtil.decrypt(server.getPassword()),
                    server.getPrivateKey()
            );
            session.setTimeout(5000);
            session.connect();
            SshExecuteResult result = SshClient.executeCommand(session, "echo 'connection_test_ok'");
            session.disconnect();
            if (result.isSuccess()) {
                server.setStatus("ONLINE");
                serverMapper.updateById(server);
                return Result.success(true);
            } else {
                server.setStatus("OFFLINE");
                serverMapper.updateById(server);
                return Result.success(false);
            }
        } catch (Exception e) {
            log.error("服务器连通性测试失败: {}", id, e);
            server.setStatus("OFFLINE");
            serverMapper.updateById(server);
            return Result.success(false);
        }
    }
}
