package com.deployhub.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nginx状态服务类
 * 解析Nginx配置文件，检查后端服务健康状态
 *
 * @author MSI-1
 * @date 2026-05-21
 */
@Slf4j
@Service
public class NginxStatusService {

    /**
     * 匹配upstream块的正则表达式
     * 捕获upstream名称和块内容
     */
    private static final Pattern UPSTREAM_PATTERN = Pattern.compile(
            "upstream\\s+(\\S+)\\s*\\{",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配server指令的正则表达式（用于upstream块内）
     * 如: server 192.168.1.1:8080;
     */
    private static final Pattern UPSTREAM_SERVER_PATTERN = Pattern.compile(
            "server\\s+(\\S+?)(?:\\s+.+?)?\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配server块的正则表达式
     */
    private static final Pattern SERVER_BLOCK_PATTERN = Pattern.compile(
            "server\\s*\\{",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配listen指令
     * 如: listen 80; 或 listen 443 ssl;
     */
    private static final Pattern LISTEN_PATTERN = Pattern.compile(
            "listen\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配server_name指令
     */
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile(
            "server_name\\s+(\\S+)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配proxy_pass指令
     */
    private static final Pattern PROXY_PASS_PATTERN = Pattern.compile(
            "proxy_pass\\s+(\\S+)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 解析Nginx配置中的upstream块
     *
     * @param configContent Nginx配置文件内容
     * @return upstream信息列表
     */
    public List<UpstreamInfo> parseUpstreams(String configContent) {
        List<UpstreamInfo> upstreams = new ArrayList<>();
        if (configContent == null || configContent.isEmpty()) {
            return upstreams;
        }

        try {
            Matcher matcher = UPSTREAM_PATTERN.matcher(configContent);
            while (matcher.find()) {
                String name = matcher.group(1);
                int blockStart = matcher.end();

                // 查找匹配的右大括号，处理嵌套
                String blockContent = extractBlockContent(configContent, blockStart);
                if (blockContent == null) {
                    continue;
                }

                // 解析upstream内的server指令
                List<String> servers = new ArrayList<>();
                Matcher serverMatcher = UPSTREAM_SERVER_PATTERN.matcher(blockContent);
                while (serverMatcher.find()) {
                    String serverAddr = serverMatcher.group(1);
                    // 过滤掉非地址的server指令参数（如backup、down等关键字）
                    if (serverAddr.contains(":") || serverAddr.matches("\\d+\\.\\d+\\.\\d+\\.\\d+.*") ||
                            serverAddr.startsWith("unix:")) {
                        servers.add(serverAddr);
                    }
                }

                UpstreamInfo info = new UpstreamInfo();
                info.setName(name);
                info.setServers(servers);
                upstreams.add(info);
            }
        } catch (Exception e) {
            log.error("解析upstream块失败", e);
        }

        return upstreams;
    }

    /**
     * 解析Nginx配置中的server块
     * 提取listen端口、server_name和proxy_pass
     *
     * @param configContent Nginx配置文件内容
     * @return server块信息列表
     */
    public List<ServerInfo> parseServerBlocks(String configContent) {
        List<ServerInfo> serverBlocks = new ArrayList<>();
        if (configContent == null || configContent.isEmpty()) {
            return serverBlocks;
        }

        try {
            Matcher matcher = SERVER_BLOCK_PATTERN.matcher(configContent);
            while (matcher.find()) {
                int blockStart = matcher.end();

                // 查找匹配的右大括号
                String blockContent = extractBlockContent(configContent, blockStart);
                if (blockContent == null) {
                    continue;
                }

                ServerInfo serverInfo = new ServerInfo();

                // 提取listen端口
                Matcher listenMatcher = LISTEN_PATTERN.matcher(blockContent);
                if (listenMatcher.find()) {
                    serverInfo.setListenPort(Integer.parseInt(listenMatcher.group(1)));
                }

                // 提取server_name
                Matcher nameMatcher = SERVER_NAME_PATTERN.matcher(blockContent);
                if (nameMatcher.find()) {
                    serverInfo.setServerName(nameMatcher.group(1));
                }

                // 提取proxy_pass
                Matcher proxyMatcher = PROXY_PASS_PATTERN.matcher(blockContent);
                if (proxyMatcher.find()) {
                    serverInfo.setProxyPass(proxyMatcher.group(1));
                }

                // 仅当有proxy_pass时才添加（纯静态服务无需健康检查）
                if (serverInfo.getProxyPass() != null && !serverInfo.getProxyPass().isEmpty()) {
                    serverBlocks.add(serverInfo);
                }
            }
        } catch (Exception e) {
            log.error("解析server块失败", e);
        }

        return serverBlocks;
    }

    /**
     * 检查后端服务的健康状态
     * 通过HTTP GET请求检测服务是否在线
     *
     * @param host 目标主机地址
     * @param port 目标端口
     * @param path 请求路径
     * @return 健康检查结果
     */
    public ServiceHealthResult checkServiceHealth(String host, int port, String path) {
        ServiceHealthResult result = new ServiceHealthResult();
        result.setStatus("OFFLINE");

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        HttpURLConnection connection = null;
        try {
            String urlStr = "http://" + host + ":" + port + path;
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false);

            long startTime = System.currentTimeMillis();
            connection.connect();
            int statusCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            result.setStatusCode(statusCode);
            result.setResponseTime(responseTime);

            // 2xx和3xx状态码视为在线
            if (statusCode < 500) {
                result.setStatus("ONLINE");
            } else {
                result.setErrorMessage("服务返回5xx错误: " + statusCode);
            }
        } catch (Exception e) {
            result.setErrorMessage(e.getMessage());
            log.debug("健康检查失败, host={}:{}{}", host, port, path, e);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    /**
     * 检查配置中所有后端服务的健康状态
     * 解析upstream和server块，逐一进行健康检查
     *
     * @param configContent Nginx配置文件内容
     * @param serverHost    Nginx服务器主机地址（用于解析相对地址）
     * @return 所有服务的状态列表
     */
    public List<ServiceStatus> checkAllServices(String configContent, String serverHost) {
        List<ServiceStatus> allStatuses = new ArrayList<>();
        if (configContent == null || configContent.isEmpty()) {
            return allStatuses;
        }

        // 检查upstream中的后端服务
        List<UpstreamInfo> upstreams = parseUpstreams(configContent);
        for (UpstreamInfo upstream : upstreams) {
            for (String serverAddr : upstream.getServers()) {
                ServiceStatus status = checkBackendAddress(upstream.getName(), serverAddr, "UPSTREAM");
                allStatuses.add(status);
            }
        }

        // 检查server块中proxy_pass指向的服务
        List<ServerInfo> serverBlocks = parseServerBlocks(configContent);
        for (ServerInfo serverInfo : serverBlocks) {
            String proxyPass = serverInfo.getProxyPass();
            if (proxyPass != null && !proxyPass.isEmpty()) {
                // 如果proxy_pass指向upstream（http://upstream_name），跳过（已在upstream中检查）
                if (proxyPass.matches("https?://[a-zA-Z_][a-zA-Z0-9_]*.*")) {
                    String upstreamName = extractUpstreamNameFromProxyPass(proxyPass);
                    if (upstreamName != null && isUpstreamName(upstreams, upstreamName)) {
                        continue;
                    }
                }

                ServiceStatus status = checkProxyPassAddress(
                        "server:" + serverInfo.getListenPort(),
                        proxyPass,
                        "SERVER_BLOCK"
                );
                allStatuses.add(status);
            }
        }

        return allStatuses;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 从配置内容中提取大括号包围的块内容
     * 处理嵌套大括号的情况
     *
     * @param content     完整配置内容
     * @param startIndex  左大括号之后的起始位置
     * @return 块内容字符串，解析失败返回null
     */
    private String extractBlockContent(String content, int startIndex) {
        int depth = 1;
        int i = startIndex;
        while (i < content.length() && depth > 0) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        if (depth == 0) {
            return content.substring(startIndex, i - 1);
        }
        return null;
    }

    /**
     * 检查upstream后端地址的健康状态
     *
     * @param name        upstream名称
     * @param serverAddr  服务器地址（如 192.168.1.1:8080）
     * @param type        类型标识
     * @return 服务状态
     */
    private ServiceStatus checkBackendAddress(String name, String serverAddr, String type) {
        ServiceStatus status = new ServiceStatus();
        status.setName(name);
        status.setAddress(serverAddr);
        status.setType(type);

        try {
            String[] parts = parseAddress(serverAddr);
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            ServiceHealthResult healthResult = checkServiceHealth(host, port, "/");
            status.setStatus(healthResult.getStatus());
            status.setResponseTime(healthResult.getResponseTime());
        } catch (Exception e) {
            status.setStatus("OFFLINE");
            log.debug("健康检查地址解析失败: {}", serverAddr, e);
        }

        return status;
    }

    /**
     * 检查proxy_pass地址的健康状态
     *
     * @param name       名称标识
     * @param proxyPass  proxy_pass URL
     * @param type       类型标识
     * @return 服务状态
     */
    private ServiceStatus checkProxyPassAddress(String name, String proxyPass, String type) {
        ServiceStatus status = new ServiceStatus();
        status.setName(name);
        status.setAddress(proxyPass);
        status.setType(type);

        try {
            // 解析proxy_pass URL
            String urlStr = proxyPass;
            if (!urlStr.startsWith("http")) {
                urlStr = "http://" + urlStr;
            }
            URL url = new URL(urlStr);
            String host = url.getHost();
            int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();
            String path = url.getPath() != null ? url.getPath() : "/";

            ServiceHealthResult healthResult = checkServiceHealth(host, port, path);
            status.setStatus(healthResult.getStatus());
            status.setResponseTime(healthResult.getResponseTime());
        } catch (Exception e) {
            status.setStatus("OFFLINE");
            log.debug("proxy_pass地址解析失败: {}", proxyPass, e);
        }

        return status;
    }

    /**
     * 解析地址字符串为host和port
     *
     * @param address 地址字符串，如 192.168.1.1:8080
     * @return [host, port]数组
     */
    private String[] parseAddress(String address) {
        // 移除可能的协议前缀
        address = address.replaceFirst("^https?://", "");
        // 移除尾部的路径
        int slashIdx = address.indexOf('/');
        if (slashIdx > 0) {
            address = address.substring(0, slashIdx);
        }

        String[] parts = address.split(":");
        if (parts.length == 2) {
            return parts;
        } else if (parts.length == 1) {
            // 没有端口，默认80
            return new String[]{parts[0], "80"};
        }
        throw new IllegalArgumentException("无法解析地址: " + address);
    }

    /**
     * 从proxy_pass URL中提取upstream名称
     * 如 http://backend_api/ -> backend_api
     *
     * @param proxyPass proxy_pass URL
     * @return upstream名称，非upstream格式返回null
     */
    private String extractUpstreamNameFromProxyPass(String proxyPass) {
        try {
            String urlStr = proxyPass;
            if (!urlStr.startsWith("http")) {
                urlStr = "http://" + urlStr;
            }
            URL url = new URL(urlStr);
            String host = url.getHost();
            // 如果host不是IP地址，可能是upstream名称
            if (!host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                return host;
            }
        } catch (Exception e) {
            log.debug("解析proxy_pass URL失败: {}", proxyPass);
        }
        return null;
    }

    /**
     * 判断名称是否为已知的upstream名称
     *
     * @param upstreams   upstream列表
     * @param upstreamName 待检查的名称
     * @return true表示是upstream名称
     */
    private boolean isUpstreamName(List<UpstreamInfo> upstreams, String upstreamName) {
        if (upstreamName == null) {
            return false;
        }
        return upstreams.stream()
                .anyMatch(u -> upstreamName.equalsIgnoreCase(u.getName()));
    }

    // ==================== 内部DTO类 ====================

    /**
     * Upstream信息
     */
    @Data
    public static class UpstreamInfo {
        /** upstream名称 */
        private String name;
        /** 后端服务器地址列表，如 192.168.1.1:8080 */
        private List<String> servers;
    }

    /**
     * Server块信息
     */
    @Data
    public static class ServerInfo {
        /** 监听端口 */
        private Integer listenPort;
        /** 代理目标地址 */
        private String proxyPass;
        /** server_name */
        private String serverName;
    }

    /**
     * 服务健康检查结果
     */
    @Data
    public static class ServiceHealthResult {
        /** 服务状态：ONLINE / OFFLINE */
        private String status;
        /** 响应时间（毫秒） */
        private long responseTime;
        /** HTTP状态码 */
        private int statusCode;
        /** 错误信息 */
        private String errorMessage;
    }

    /**
     * 服务状态信息
     */
    @Data
    public static class ServiceStatus {
        /** 服务名称（upstream名称或server标识） */
        private String name;
        /** 服务地址 */
        private String address;
        /** 服务状态：ONLINE / OFFLINE */
        private String status;
        /** 响应时间（毫秒） */
        private long responseTime;
        /** 类型：UPSTREAM / SERVER_BLOCK */
        private String type;
    }
}
