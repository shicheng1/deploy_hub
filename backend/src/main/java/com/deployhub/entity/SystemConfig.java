package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_system_config")
public class SystemConfig extends BaseEntity {

    private String configKey;

    private String configValue;

    private String configGroup;

    private String description;
}
