package com.codeai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> records;
    private int page;
    private int size;

    public static <T> PageResult<T> of(long total, List<T> records, int page, int size) {
        return new PageResult<>(total, records, page, size);
    }
}
