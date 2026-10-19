package com.example.live.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.live.entity.LiveRoom;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MyMetaObjectHandlerTest {

    @Test
    void insertFillsAuditFields() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LiveRoom.class);

        LiveRoom room = new LiveRoom();
        new MyMetaObjectHandler().insertFill(SystemMetaObject.forObject(room));

        assertThat(room.getCreatedAt()).isNotNull();
        assertThat(room.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateAlwaysRefreshesUpdatedAt() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LiveRoom.class);

        LocalDateTime oldValue = LocalDateTime.of(2020, 1, 1, 0, 0);
        LiveRoom room = new LiveRoom();
        room.setUpdatedAt(oldValue);

        new MyMetaObjectHandler().updateFill(SystemMetaObject.forObject(room));

        assertThat(room.getUpdatedAt()).isAfter(oldValue);
    }
}
