package com.deployhub.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private long total;
    private List<T> records;

    public PageResult() {}

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
