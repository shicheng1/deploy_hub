package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_script_template")
public class ScriptTemplate extends BaseEntity {

    private String name;

    private Long appId;

    private String content;

    private String description;

    private String category;
}