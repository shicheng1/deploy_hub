package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_deploy_config")
public class DeployConfig extends BaseEntity {

    @TableField("app_id")
    private Long appId;

    @TableField("server_ids")
    private String serverIds;  // JSON array of server IDs like "[1,2,3]"

    @TableField("version")
    private String version;

    @TableField("service_name")
    private String serviceName;

    @TableField("deploy_mode")
    private String deployMode;

    @TableField("script_template_id")
    private Long scriptTemplateId;

    @TableField("deploy_script")
    private String deployScript;
}
