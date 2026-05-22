package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ProjectDTO;
import com.deployhub.vo.ProjectVO;

public interface ProjectService {

    Result<PageResult<ProjectVO>> list(int pageNum, int pageSize, String name);

    Result<ProjectVO> getById(Long id);

    Result<Void> add(ProjectDTO dto);

    Result<Void> update(Long id, ProjectDTO dto);

    Result<Void> delete(Long id);
}
