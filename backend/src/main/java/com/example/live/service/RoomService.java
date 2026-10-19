package com.example.live.service;

import com.example.live.common.result.PageResult;
import com.example.live.vo.LiveRoomVO;

public interface RoomService {

    PageResult<LiveRoomVO> pageRooms(int page, int size, Long categoryId, String keyword);
}
