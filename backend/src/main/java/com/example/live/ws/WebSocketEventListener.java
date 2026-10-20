package com.example.live.ws;

import com.example.live.service.impl.ViewerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.regex.Pattern;

@Component
public class WebSocketEventListener {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms/(\\d+)$");

    private final ViewerService viewerService;

    public WebSocketEventListener(ViewerService viewerService) {
        this.viewerService = viewerService;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || accessor.getSessionId() == null) {
            return;
        }
        var matcher = ROOM_TOPIC.matcher(destination);
        if (matcher.matches()) {
            viewerService.join(accessor.getSessionId(), Long.valueOf(matcher.group(1)));
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        viewerService.disconnect(event.getSessionId());
    }
}
