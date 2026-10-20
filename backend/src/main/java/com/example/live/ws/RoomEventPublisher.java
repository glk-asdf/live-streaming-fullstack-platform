package com.example.live.ws;

import com.example.live.service.impl.ViewerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoomEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ViewerService viewerService;

    public RoomEventPublisher(SimpMessagingTemplate messagingTemplate, ViewerService viewerService) {
        this.messagingTemplate = messagingTemplate;
        this.viewerService = viewerService;
    }

    public void roomEnded(Long roomId) {
        viewerService.clear(roomId);
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId,
                    WsMessage.of("ROOM_END", new RoomEndData(roomId, "ended")));
        } catch (Exception ex) {
            log.warn("broadcast room end failed, roomId={}", roomId, ex);
        }
    }
}
