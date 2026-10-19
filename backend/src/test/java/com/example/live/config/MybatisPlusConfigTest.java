package com.example.live.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigTest {

    @Test
    void paginationInterceptorTargetsMysqlWithLimit() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = interceptor.getInterceptors().stream()
                .filter(PaginationInnerInterceptor.class::isInstance)
                .map(PaginationInnerInterceptor.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("未注册分页拦截器"));

        assertThat(pagination.getDbType()).isEqualTo(DbType.MYSQL);
        assertThat(pagination.getMaxLimit()).isEqualTo(50L);
    }
}
