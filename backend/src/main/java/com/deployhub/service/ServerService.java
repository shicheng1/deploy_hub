package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ServerDTO;
import com.deployhub.vo.ServerVO;

public interface ServerService {

    Result<PageResult<ServerVO>> list(int pageNum, int pageSize, String name, String groupName);

    Result<ServerVO> getById(Long id);

    Result<Void> add(ServerDTO dto);

    Result<Void> update(ServerDTO dto);

    Result<Void> delete(Long id);

    Result<Boolean> testConnection(Long id);
}
