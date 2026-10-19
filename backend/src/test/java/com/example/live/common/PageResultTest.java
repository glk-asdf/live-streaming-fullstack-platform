package com.example.live.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.live.common.result.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void mapsRecordsAndKeepsPaginationMetadata() {
        Page<String> source = new Page<>(2, 5, 11);
        source.setRecords(List.of("a", "b"));

        PageResult<String> result = PageResult.of(source, value -> value.toUpperCase());

        assertThat(result.list()).containsExactly("A", "B");
        assertThat(result.total()).isEqualTo(11);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
    }
}
