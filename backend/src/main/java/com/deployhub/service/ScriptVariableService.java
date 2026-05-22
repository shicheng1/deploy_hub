package com.deployhub.service;

import com.deployhub.common.Result;

import java.util.Map;

public interface ScriptVariableService {

    Result<Map<String, Object>> listAll();

    Result<Map<String, Object>> getByName(String name);
}