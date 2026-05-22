package com.deployhub.service.impl;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deployhub.common.Result;
import com.deployhub.entity.ScriptVariable;
import com.deployhub.mapper.ScriptVariableMapper;
import com.deployhub.service.ScriptVariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScriptVariableServiceImpl implements ScriptVariableService {

    @Autowired
    private ScriptVariableMapper scriptVariableMapper;

    @Override
    public Result<Map<String, Object>> listAll() {
        List<ScriptVariable> variables = scriptVariableMapper.selectAll();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> variableList = new ArrayList<>();

        for (ScriptVariable variable : variables) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", variable.getName());
            item.put("source", variable.getSource());
            item.put("description", variable.getDescription());
            
            if (variable.getOptionsJson() != null && !variable.getOptionsJson().isEmpty()) {
                try {
                    JSONArray options = JSONUtil.parseArray(variable.getOptionsJson());
                    List<String> optionList = new ArrayList<>();
                    for (Object opt : options) {
                        optionList.add(opt.toString());
                    }
                    item.put("options", optionList);
                } catch (Exception e) {
                    item.put("options", new ArrayList<>());
                }
            } else {
                item.put("options", new ArrayList<>());
            }
            variableList.add(item);
        }
        result.put("variables", variableList);
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> getByName(String name) {
        ScriptVariable variable = scriptVariableMapper.selectOne(
            Wrappers.<ScriptVariable>lambdaQuery()
                .eq(ScriptVariable::getName, name)
                .eq(ScriptVariable::getDeleted, 0)
        );
        
        if (variable == null) {
            return Result.error("变量不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", variable.getName());
        result.put("source", variable.getSource());
        result.put("description", variable.getDescription());

        if (variable.getOptionsJson() != null && !variable.getOptionsJson().isEmpty()) {
            try {
                JSONArray options = JSONUtil.parseArray(variable.getOptionsJson());
                List<String> optionList = new ArrayList<>();
                for (Object opt : options) {
                    optionList.add(opt.toString());
                }
                result.put("options", optionList);
            } catch (Exception e) {
                result.put("options", new ArrayList<>());
            }
        } else {
            result.put("options", new ArrayList<>());
        }

        return Result.success(result);
    }
}