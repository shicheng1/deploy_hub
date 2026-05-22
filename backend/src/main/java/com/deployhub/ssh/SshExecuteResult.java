package com.deployhub.ssh;

import lombok.Data;

@Data
public class SshExecuteResult {

    private int exitCode;
    private String output;
    private String error;

    public SshExecuteResult() {}

    public SshExecuteResult(int exitCode, String output, String error) {
        this.exitCode = exitCode;
        this.output = output;
        this.error = error;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
