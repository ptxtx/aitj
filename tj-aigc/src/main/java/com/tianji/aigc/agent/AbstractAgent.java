package com.tianji.aigc.agent;


import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.service.ChatTitleService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
public abstract class AbstractAgent implements Agent {
    @Resource
    private ChatClient chatClient;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatTitleService titleService;


    private static final String GENERATE_STATUS_KEY ="GENERATE_STATUS";
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        String requestId = this.generateRequestId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        Long userId = UserContext.getUser();
        boolean needsTitle = chatSessionService.needsTitle(sessionId, userId);
        StringBuilder assistantAnswer = new StringBuilder();
        chatSessionService.update(sessionId, null, userId);// 只更新会话时间，不再把问题作为标题
        return this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId,requestId))
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

                    if (StrUtil.isNotEmpty(text)) {
                        assistantAnswer.append(text);
                    }

                    //获取到消息id
                    String messageId = chatResponse.getMetadata().getId();
                    //将消息id与请求id关联
                    ToolResultHolder.put(messageId, Constant.REQUEST_ID,requestId);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .doOnComplete(() -> {
                    if (needsTitle) {
                        String title = titleService.generateTitle(
                                question,
                                assistantAnswer.toString()
                        );
                        chatSessionService.initTitle(sessionId, title, userId);
                    }
                })
                .concatWith(Flux.defer(()->{
                    Map<String, Object> result = ToolResultHolder.get(requestId);
                    if(ObjectUtil.isNotEmpty(result)){
                        ToolResultHolder.remove(requestId);
                        //工具被调用了，需要向前端传递参数
                        return Flux.just(ChatEventVO.builder()
                                        .eventType(ChatEventTypeEnum.PARAM.getValue())
                                        .eventData(result)
                                        .build(),
                                ChatEventVO.builder()  // 标记输出结束
                                        .eventType(ChatEventTypeEnum.STOP.getValue())
                                        .build()
                        );
                    }
                    return Flux.just(ChatEventVO.builder()  // 标记输出结束
                            .eventType(ChatEventTypeEnum.STOP.getValue())
                            .build());

                }));
    }

    @Override
    public String process(String question, String sessionId) {
        String requestId = this.generateRequestId();
        Long userId = UserContext.getUser();
        boolean needsTitle = chatSessionService.needsTitle(sessionId, userId);

        String answer=this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId,requestId))
                .user(question)
                .call()
                .content();
        if(needsTitle){
            String title = titleService.generateTitle(question, answer);
            chatSessionService.update(sessionId, title, userId);
        }else{
            chatSessionService.update(sessionId, null, userId);
        }
        return answer;
    }

    private String generateRequestId(){
        return IdUtil.fastSimpleUUID();
    }
    @Override
    public void stop(String sessionId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        //让takewhile返回false
        hashOps.delete(sessionId);

    }

    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        String conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }
}
