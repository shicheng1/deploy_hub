package com.deployhub.util;

public class OsUtil {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static final String REMOTE_TEMP_DIR = "/tmp";

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isLinux() {
        return OS_NAME.contains("nux") || OS_NAME.contains("nix");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    public static String convertPathToOsFormat(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.replace("\\", "/");
        if (isWindows()) {
            return normalized.replace("/", "\\");
        }
        return normalized;
    }

    public static String getScriptExtension() {
        if (isWindows()) {
            return ".ps1";
        } else {
            return ".sh";
        }
    }

    public static ProcessBuilder createProcessBuilder(String command) {
        ProcessBuilder pb;
        if (isWindows()) {
            pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-Command", command);
        } else {
            pb = new ProcessBuilder("sh", "-c", command);
        }
        return pb;
    }

    public static String normalizeBuildCommand(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }
        if (isWindows()) {
            command = command.replace(";", "&");
        }
        return command;
    }

    /**
     * 清理脚本内容，将Windows换行符转换为Unix换行符
     * @param scriptContent 原始脚本内容
     * @return 清理后的脚本内容
     */
    public static String normalizeScriptContent(String scriptContent) {
        if (scriptContent == null || scriptContent.isEmpty()) {
            return scriptContent;
        }
        // 将 \r\n 转换为 \n
        return scriptContent.replaceAll("\r\n", "\n");
    }

    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }
}