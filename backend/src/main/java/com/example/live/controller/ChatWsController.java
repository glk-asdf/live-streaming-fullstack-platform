package com.example.live.controller;

import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.dto.ChatSendRequest;
import com.example.live.security.LoginUser;
import com.example.live.service.ChatService;
import com.example.live.service.impl.ViewerService;
import com.example.live.ws.WsMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;
    private final ViewerService viewerService;

    public ChatWsController(ChatService chatService, ViewerService viewerService) {
        this.chatService = chatService;
        this.viewerService = viewerService;
    }

    @MessageMapping("/rooms/{roomId}/chat")
    public void chat(@DestinationVariable Long roomId, ChatSendRequest request, Principal principal) {
        LoginUser user = requireUser(principal);
        chatService.sendChat(roomId, user.id(), request == null ? null : request.content());
    }

    @MessageMapping("/rooms/{roomId}/heartbeat")
    public void heartbeat(@DestinationVariable Long roomId, SimpMessageHeaderAccessor accessor) {
        viewerService.heartbeat(roomId, accessor.getSessionId());
    }

    @MessageExceptionHandler(BizException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public WsMessage<String> handleBizException(BizException ex) {
        return WsMessage.of("ERROR", ex.getMessage());
    }

    private LoginUser requireUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        throw new BizException(ResultCode.UNAUTHORIZED, "请先登录后再发言");
    }
}
