package com.tianji.aigc.service.impl;

import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        return chatClient.prompt()
                .user(question)
                .stream()// 开启流式
                .chatResponse()
                .map(chatResponse -> ChatEventVO.builder()
                        .eventData(chatResponse)
                        .eventType(ChatEventTypeEnum.DATA.getValue())
                        .build())
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventData(null)
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));//结束标识


    }
}
