package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.DeployStepResult;
import com.deployhub.dto.NginxConfigDTO;
import com.deployhub.dto.OneClickDeployDTO;
import com.deployhub.entity.Server;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.NginxCommandService;
import com.deployhub.service.NginxConfigService;
import com.deployhub.service.NginxDeployService;
import com.deployhub.service.NginxStatusService;
import com.deployhub.vo.NginxConfigVO;
import com.deployhub.vo.NginxServerConfigVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nginx-config")
public class NginxConfigController {

    @Autowired
    private NginxConfigService nginxConfigService;

    @Autowired
    private NginxDeployService nginxDeployService;

    @Autowired
    private NginxStatusService nginxStatusService;

    @Autowired
    private NginxCommandService nginxCommandService;

    @Autowired
    private ServerMapper serverMapper;

    @GetMapping("/list")
    public Result<PageResult<NginxConfigVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) String name,
                                                   @RequestParam(required = false) Long serverId) {
        return nginxConfigService.list(pageNum, pageSize, name, serverId);
    }

    @GetMapping("/{id}")
    public Result<NginxConfigVO> getById(@PathVariable Long id) {
        return nginxConfigService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody NginxConfigDTO dto) {
        return nginxConfigService.add(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NginxConfigDTO dto) {
        dto.setId(id);
        return nginxConfigService.update(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return nginxConfigService.delete(id);
    }

    @PostMapping("/{id}/test")
    public Result<String> testConfig(@PathVariable Long id) {
        return nginxConfigService.testConfig(id);
    }

    @PostMapping("/{id}/deploy")
    public Result<Void> deployConfig(@PathVariable Long id) {
        return nginxConfigService.deployConfig(id);
    }

    @PostMapping("/{id}/save-as-template")
    public Result<NginxConfigVO> saveAsTemplate(@PathVariable Long id,
                                                 @RequestParam String templateName,
                                                 @RequestParam(required = false) String templateDesc) {
        return nginxConfigService.saveAsTemplate(id, templateName, templateDesc);
    }

    /**
     * 一键部署Nginx
     * 从模板部署Nginx到目标服务器，包含安装、配置写入、语法测试、服务重载
     *
     * @param dto 一键部署请求参数
     * @return 部署步骤结果
     */
    @PostMapping("/one-click-deploy")
    public Result<DeployStepResult> oneClickDeploy(@Valid @RequestBody OneClickDeployDTO dto) {
        String configPath = dto.getConfigPath();
        if (configPath == null || configPath.isEmpty()) {
            configPath = "/usr/local/nginx/conf/nginx.conf";
        }
        return nginxDeployService.oneClickDeploy(dto.getTemplateId(), dto.getServerId(), dto.getPackageId(), dto.getConfigName(), configPath);
    }

    /**
     * 获取配置对应的后端服务状态
     * 解析Nginx配置中的upstream和proxy_pass，检查后端服务健康状态
     *
     * @param id 配置ID
     * @return 服务状态列表
     */
    @GetMapping("/{id}/service-status")
    public Result<List<NginxStatusService.ServiceStatus>> getServiceStatus(@PathVariable Long id) {
        NginxConfigVO config = nginxConfigService.getById(id).getData();
        if (config == null) {
            return Result.error("配置不存在");
        }
        List<NginxStatusService.ServiceStatus> statuses = nginxStatusService.checkAllServices(
                config.getConfigContent(), config.getServerHost());
        return Result.success(statuses);
    }

    /**
     * 读取服务器上的Nginx配置
     * 检测Nginx安装状态、版本号，读取主配置和conf.d目录下的配置文件
     *
     * @param serverId 服务器ID
     * @return 服务器Nginx配置信息
     */
    @GetMapping("/server-config")
    public Result<NginxServerConfigVO> getServerNginxConfig(@RequestParam Long serverId) {
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            return Result.error("服务器不存在");
        }
        NginxServerConfigVO vo = nginxCommandService.readNginxConfig(server);
        return Result.success(vo);
    }
}
