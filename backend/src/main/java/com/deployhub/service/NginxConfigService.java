package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.NginxConfigDTO;
import com.deployhub.vo.NginxConfigVO;

public interface NginxConfigService {

    Result<PageResult<NginxConfigVO>> list(int pageNum, int pageSize, String name, Long serverId);

    Result<NginxConfigVO> getById(Long id);

    Result<Void> add(NginxConfigDTO dto);

    Result<Void> update(NginxConfigDTO dto);

    Result<Void> delete(Long id);

    /**
     * 测试Nginx配置语法
     *
     * @param id 配置ID
     * @return 测试结果输出
     */
    Result<String> testConfig(Long id);

    /**
     * 部署配置到服务器并重载Nginx
     *
     * @param id 配置ID
     * @return 部署结果
     */
    Result<Void> deployConfig(Long id);

    /**
     * 将配置保存为模板
     *
     * @param id           配置ID
     * @param templateName 模板名称
     * @param templateDesc 模板描述
     * @return 保存后的配置信息
     */
    Result<NginxConfigVO> saveAsTemplate(Long id, String templateName, String templateDesc);
}
