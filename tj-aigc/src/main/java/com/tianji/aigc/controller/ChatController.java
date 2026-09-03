package com.tianji.aigc.controller;


import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.annotations.NoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    @NoWrapper//响应的结果不包装
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)//表示返回的是流式数据
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO){//因为是流式输出，返回FLux
        return chatService.chat(chatDTO.getQuestion(),chatDTO.getSessionId());

    }

}
