package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.AppDTO;
import com.deployhub.vo.AppVO;

public interface AppService {

    Result<PageResult<AppVO>> list(int pageNum, int pageSize, String name, String type, Long projectId);

    Result<AppVO> getById(Long id);

    Result<Void> add(AppDTO dto);

    Result<Void> update(AppDTO dto);

    Result<Void> delete(Long id);
}
