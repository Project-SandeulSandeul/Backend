package com.mountain.backend.chat.controller;

import com.mountain.backend.chat.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate; // 생성자 주입 -> @RequiredArgsConstructor 가 있으므로 해당필드를 포함하고 있는 클래스의 이름으로 대체하지 않음


    @MessageMapping("/chat/message")    // 설정한 url 매핑으로 클라이언트로부터 요청 메세지를 받으면 @SendTo 설정한 구독 클라이언트들에게 메세지를 보낸다.
    public void message(@Payload ChatDto chatDto) throws Exception{
        switch(chatDto.getType()){
            case ENTER -> {
                System.out.println("ENTER");
                ChatDto message = chatMessageService.visitMember(chatDto);
//                chatDto.setMessage(chatDto.getSenderName() + "님이 채팅방에 참여하셨습니다.");
                messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getChatRoomId(), message);
            }

        }


    }
}
