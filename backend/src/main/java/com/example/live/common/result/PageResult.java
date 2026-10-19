package com.example.live.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应。
 *
 * @param list  当前页数据
 * @param total 总记录数
 * @param page  页码，从 1 开始
 * @param size  每页条数
 */
public record PageResult<T>(List<T> list, long total, long page, long size) {

    public static <S, T> PageResult<T> of(IPage<S> source, Function<S, T> mapper) {
        List<T> list = source.getRecords().stream().map(mapper).toList();
        return new PageResult<>(list, source.getTotal(), source.getCurrent(), source.getSize());
    }
}
