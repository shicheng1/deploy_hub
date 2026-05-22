package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ScriptTemplateDTO;
import com.deployhub.vo.ScriptTemplateVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ScriptTemplateService {

    Result<PageResult<ScriptTemplateVO>> list(int pageNum, int pageSize, String name, Long appId, String category);

    Result<ScriptTemplateVO> getById(Long id);

    Result<Void> add(ScriptTemplateDTO dto);

    Result<Void> upload(MultipartFile file, String name, String description) throws IOException;

    Result<Void> update(ScriptTemplateDTO dto);

    Result<Void> delete(Long id);
}