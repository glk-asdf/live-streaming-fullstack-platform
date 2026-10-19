package com.example.live.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.live.common.result.PageResult;
import com.example.live.converter.LiveRoomConverter;
import com.example.live.entity.LiveRoom;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.service.RoomService;
import com.example.live.vo.LiveRoomVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoomServiceImpl implements RoomService {

    private final LiveRoomMapper liveRoomMapper;

    public RoomServiceImpl(LiveRoomMapper liveRoomMapper) {
        this.liveRoomMapper = liveRoomMapper;
    }

    @Override
    public PageResult<LiveRoomVO> pageRooms(int page, int size, Long categoryId, String keyword) {
        Page<LiveRoom> pageParam = new Page<>(page, size);
        Page<LiveRoom> result = liveRoomMapper.selectPage(pageParam,
                Wrappers.<LiveRoom>lambdaQuery()
                        .eq(categoryId != null, LiveRoom::getCategoryId, categoryId)
                        .like(StringUtils.hasText(keyword), LiveRoom::getTitle, keyword)
                        .orderByDesc(LiveRoom::getCreatedAt)
                        .orderByDesc(LiveRoom::getId));
        return PageResult.of(result, LiveRoomConverter::toVO);
    }
}
