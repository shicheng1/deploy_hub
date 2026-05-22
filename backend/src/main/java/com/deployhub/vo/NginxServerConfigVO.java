package com.deployhub.vo;

import lombok.Data;

import java.util.List;

@Data
public class NginxServerConfigVO {

    /** 是否安装了 Nginx */
    private boolean installed;

    /** Nginx 版本号 */
    private String version;

    /** 主配置文件路径 */
    private String configFilePath;

    /** 主配置文件内容 */
    private String mainConfigContent;

    /** conf.d 下的配置文件列表 */
    private List<ConfigFileInfo> confFiles;

    @Data
    public static class ConfigFileInfo {
        /** 文件名 */
        private String fileName;
        /** 完整路径 */
        private String filePath;
        /** 文件内容 */
        private String content;
    }
}
