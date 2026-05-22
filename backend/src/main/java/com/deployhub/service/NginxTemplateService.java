package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.NginxTemplateDTO;
import com.deployhub.vo.NginxConfigVO;
import com.deployhub.vo.NginxTemplateVO;

public interface NginxTemplateService {

    Result<PageResult<NginxTemplateVO>> list(int pageNum, int pageSize, String name, String category);

    Result<NginxTemplateVO> getById(Long id);

    Result<Void> add(NginxTemplateDTO dto);

    Result<Void> update(NginxTemplateDTO dto);

    Result<Void> delete(Long id);

    /**
     * 从模板创建配置，替换模板中的服务器IP变量
     *
     * @param templateId 模板ID
     * @param serverId   目标服务器ID
     * @param configName 配置名称
     * @return 创建的配置信息
     */
    Result<NginxConfigVO> createConfigFromTemplate(Long templateId, Long serverId, String configName);
}
