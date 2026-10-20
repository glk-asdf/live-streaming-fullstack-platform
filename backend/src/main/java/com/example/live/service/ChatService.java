package com.example.live.service;

import com.example.live.vo.ChatMessageVO;

import java.util.List;

public interface ChatService {

    void sendChat(Long roomId, Long userId, String content);

    List<ChatMessageVO> history(Long roomId, int limit);
}
