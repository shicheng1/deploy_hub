package com.deployhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.deployhub.entity.ScriptVariable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScriptVariableMapper extends BaseMapper<ScriptVariable> {

    @Select("SELECT * FROM t_script_variable WHERE deleted = 0 ORDER BY source, name")
    List<ScriptVariable> selectAll();
}