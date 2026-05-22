package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.BatchScriptExecutionRequestDTO;
import com.deployhub.dto.BatchScriptExecutionResultDTO;
import com.deployhub.dto.ScriptExecutionRequestDTO;
import com.deployhub.vo.ScriptExecutionVO;

import java.util.Map;

public interface ScriptExecutionService {

    Result<PageResult<ScriptExecutionVO>> list(int pageNum, int pageSize, Long serverId, String status);

    Result<ScriptExecutionVO> getById(Long id);

    Result<Map<String, Object>> execute(ScriptExecutionRequestDTO dto);

    Result<BatchScriptExecutionResultDTO> batchExecute(BatchScriptExecutionRequestDTO dto);

    Result<String> getLog(Long id);

    Result<Void> cancel(Long id);
}