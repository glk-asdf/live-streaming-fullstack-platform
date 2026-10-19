package com.example.live.entity;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveRoomLogicDeleteTest {

    @Test
    void deletedFieldIsRegisteredAsLogicDelete() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, LiveRoom.class);

        assertThat(tableInfo.isWithLogicDelete()).isTrue();
        assertThat(tableInfo.getLogicDeleteFieldInfo()).isNotNull();
        assertThat(tableInfo.getLogicDeleteFieldInfo().getColumn()).isEqualTo("deleted");
        assertThat(tableInfo.getLogicDeleteFieldInfo().getLogicNotDeleteValue()).isEqualTo("0");
        assertThat(tableInfo.getLogicDeleteFieldInfo().getLogicDeleteValue()).isEqualTo("1");
    }
}
