package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_app")
public class App extends BaseEntity {

    @TableField("project_id")
    private Long projectId;

    private String name;

    private String type;

    private String projectPath;

    private String buildCommand;

    @TableField("frontend_build_command")
    private String frontendBuildCommand;

    @TableField("backend_build_command")
    private String backendBuildCommand;

    private String outputPath;

    @TableField("remote_deploy_path")
    private String remoteDeployPath;

    @TableField("config_file_path")
    private String configFilePath;

    private String description;
}
