package com.example.live.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：createdAt 仅插入时写，updatedAt 插入与更新都写。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // strictUpdateFill 只在字段为空时填充；更新已有实体时 updatedAt 通常已有旧值，
        // 会导致更新时间不刷新，因此这里显式覆盖。
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
