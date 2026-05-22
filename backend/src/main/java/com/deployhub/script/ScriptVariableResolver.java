package com.deployhub.script;

import com.deployhub.entity.App;
import com.deployhub.entity.Server;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScriptVariableResolver {

    public Map<String, String> resolve(App app, Server server, String version) {
        Map<String, String> variables = new HashMap<>();
        if (app != null) {
            variables.put("APP_NAME", sanitizeAppName(app.getName()));
            variables.put("APP_TYPE", app.getType() != null ? app.getType() : "");
            variables.put("PROJECT_PATH", app.getProjectPath() != null ? app.getProjectPath() : "");
            variables.put("BUILD_COMMAND", app.getBuildCommand() != null ? app.getBuildCommand() : "");
            variables.put("OUTPUT_PATH", app.getOutputPath() != null ? app.getOutputPath() : "");
            variables.put("REMOTE_DEPLOY_PATH", app.getRemoteDeployPath() != null ? app.getRemoteDeployPath() : "");
            variables.put("REMOTE_FILE_PATH", "");
        }
        if (server != null) {
            variables.put("SERVER_HOST", server.getHost() != null ? server.getHost() : "");
            variables.put("SERVER_PORT", server.getPort() != null ? server.getPort().toString() : "22");
            variables.put("SERVER_USER", server.getUsername() != null ? server.getUsername() : "");
            variables.put("SERVER_GROUP", server.getGroupName() != null ? server.getGroupName() : "");
        }
        if (version != null) {
            variables.put("VERSION", version);
        }
        variables.put("TIMESTAMP", String.valueOf(System.currentTimeMillis()));
        variables.put("DATE", java.time.LocalDate.now().toString());
        return variables;
    }

    private String sanitizeAppName(String appName) {
        if (appName == null || appName.isEmpty()) {
            return "app";
        }
        String sanitized = appName.replaceAll("[^a-zA-Z0-9_-]", "-");
        sanitized = sanitized.replaceAll("-+", "-");
        sanitized = sanitized.replaceAll("^-|-$", "");
        return sanitized.isEmpty() ? "app" : sanitized;
    }
}
