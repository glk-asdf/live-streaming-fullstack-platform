package com.example.live.service;

import com.example.live.common.result.PageResult;
import com.example.live.dto.RoomSaveRequest;
import com.example.live.vo.LiveRoomVO;
import com.example.live.vo.RoomStreamVO;

public interface RoomService {

    PageResult<LiveRoomVO> pageRooms(int page, int size, Long categoryId, String keyword, Integer status);

    LiveRoomVO myRoom();

    LiveRoomVO detail(Long id);

    LiveRoomVO create(RoomSaveRequest request);

    LiveRoomVO update(Long id, RoomSaveRequest request);

    RoomStreamVO start(Long id);

    RoomStreamVO resetStreamKey(Long id);

    RoomStreamVO streamInfo(Long id);

    void stop(Long id);

    boolean authenticatePublish(Long roomId, String streamKey);
}
