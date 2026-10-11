package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.service.ChatTitleService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//增强型智能体实现
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai",name = "chat-type",havingValue = "ENHANCE")
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final StringRedisTemplate redisTemplate;

   // private static final ConcurrentHashMap<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private static final String GENERATE_STATUS_KEY ="GENERATE_STATUS";
    private final VectorStore vectorStore;
    private final ChatSessionService chatSessionService;
    private final ChatTitleService chatTitleService;

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        String requestId = IdUtil.simpleUUID();
        Long userId = UserContext.getUser();

        //定义RAG增强顾问
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)
                        .topK(6)
                        .build())
                .build();

        //更新会话标题
        //this.chatSessionService.update(sessionId,question,userId);
        boolean needsTitle = chatSessionService.needsTitle(sessionId, userId);
        StringBuilder assistantAnswer = new StringBuilder();
// 只更新会话时间，不再把问题作为标题
        chatSessionService.update(sessionId, null, userId);
        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(this.systemPromptConfig.getChatSystemMessage().get()) // 设置系统提示语
                        .params(Map.of("now",DateUtil.now())) // 设置当前时间的参数
                )
                .advisors(advisor -> advisor
                        .advisors(qaAdvisor)//添加RAG增强
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(Constant.REQUEST_ID, requestId,Constant.USER_ID, userId))
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
                    ToolResultHolder.put(messageId,Constant.REQUEST_ID,requestId);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .doOnComplete(() -> {
                    if (needsTitle) {
                        String title = chatTitleService.generateTitle(
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
//                .concatWith(Flux.just(ChatEventVO.builder()  // 标记输出结束
//                        .eventType(ChatEventTypeEnum.STOP.getValue())
//                        .build()));
    }

    @Override
    public void stop(String sessionId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        //让takewhile返回false
        hashOps.delete(sessionId);


    }

    @Override
    public String chatText(String question) {
        return chatClient.prompt()
                .system(this.systemPromptConfig.getTextSystemMessage().get())
                .user(question)
                .call().content();
    }
}
