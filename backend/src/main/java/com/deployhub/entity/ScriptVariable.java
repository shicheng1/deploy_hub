package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_script_variable")
public class ScriptVariable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String source;

    private String description;

    @TableField("options")
    private String optionsJson;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}