package com.deployhub.ssh;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
public class SshClient {

    public static Session createSession(String host, int port, String username, String password, String privateKey) throws JSchException {
        JSch jsch = new JSch();
        if (privateKey != null && !privateKey.isEmpty()) {
            jsch.addIdentity("key", privateKey.getBytes(), null, null);
        }
        Session session = jsch.getSession(username, host, port);
        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    public static SshExecuteResult executeCommand(Session session, String command) {
        ChannelExec channel = null;
        try {
            if (!session.isConnected()) {
                session.connect();
            }
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getExtInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            while (true) {
                while (inputStream.available() > 0) {
                    int len = inputStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    outputStream.write(buffer, 0, len);
                }
                while (errStream.available() > 0) {
                    int len = errStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    errorStream.write(buffer, 0, len);
                }
                if (channel.isClosed()) {
                    while (inputStream.available() > 0) {
                        int len = inputStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        outputStream.write(buffer, 0, len);
                    }
                    while (errStream.available() > 0) {
                        int len = errStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        errorStream.write(buffer, 0, len);
                    }
                    break;
                }
                Thread.sleep(100);
            }

            return new SshExecuteResult(channel.getExitStatus(),
                    outputStream.toString(StandardCharsets.UTF_8.name()),
                    errorStream.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            log.error("执行SSH命令失败: {}", command, e);
            return new SshExecuteResult(-1, "", e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public static SshExecuteResult executeCommandWithCallback(Session session, String command, Consumer<String> callback) {
        ChannelExec channel = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        try {
            if (!session.isConnected()) {
                session.connect();
            }
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getExtInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            while (true) {
                while (inputStream.available() > 0) {
                    int len = inputStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    String output = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    callback.accept(output);
                    outputStream.write(buffer, 0, len);
                }
                while (errStream.available() > 0) {
                    int len = errStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    String output = "[ERROR] " + new String(buffer, 0, len, StandardCharsets.UTF_8);
                    callback.accept(output);
                    errorStream.write(buffer, 0, len);
                }
                if (channel.isClosed()) {
                    while (inputStream.available() > 0) {
                        int len = inputStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        String output = new String(buffer, 0, len, StandardCharsets.UTF_8);
                        callback.accept(output);
                        outputStream.write(buffer, 0, len);
                    }
                    while (errStream.available() > 0) {
                        int len = errStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        String output = "[ERROR] " + new String(buffer, 0, len, StandardCharsets.UTF_8);
                        callback.accept(output);
                        errorStream.write(buffer, 0, len);
                    }
                    break;
                }
                Thread.sleep(100);
            }

            return new SshExecuteResult(channel.getExitStatus(),
                    outputStream.toString(StandardCharsets.UTF_8.name()),
                    errorStream.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            log.error("执行SSH命令失败(回调模式): {}", command, e);
            String errorMsg = "执行异常: " + e.getMessage();
            callback.accept(errorMsg);
            return new SshExecuteResult(-1, outputStream.toString(), errorMsg);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public static boolean uploadFile(Session session, String localPath, String remotePath) {
        ChannelSftp channel = null;
        try {
            if (!session.isConnected()) {
                session.connect();
            }
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(localPath, remotePath, ChannelSftp.OVERWRITE);
            return true;
        } catch (Exception e) {
            log.error("上传文件失败: localPath={}, remotePath={}", localPath, remotePath, e);
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
