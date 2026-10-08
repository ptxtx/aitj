package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final StringRedisTemplate redisTemplate;

   // private static final ConcurrentHashMap<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private static final String GENERATE_STATUS_KEY ="GENERATE_STATUS";



    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(this.systemPromptConfig.getChatSystemMessage().get()) // 设置系统提示语
                        .params(Map.of("now",DateUtil.now())) // 设置当前时间的参数
                )
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(sessionId, "true"))//开始时生成标识
                .doOnComplete(() -> hashOps.delete(sessionId))//正常结束时删除
                .doOnError(throwable -> hashOps.delete(sessionId))//异常结束时删除
                .takeWhile(chatResponse -> hashOps.get(sessionId)!=null)//后续生成的条件，若为true，继续生成，反之。
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()  // 标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    @Override
    public void stop(String sessionId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        //让takewhile返回false
        hashOps.delete(sessionId);


    }
}
