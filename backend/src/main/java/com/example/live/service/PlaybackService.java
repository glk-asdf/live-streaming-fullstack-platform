package com.example.live.service;

import com.example.live.vo.PlayInfoVO;

public interface PlaybackService {

    PlayInfoVO playInfo(Long roomId);

    boolean canRead(Long roomId, String token);
}
