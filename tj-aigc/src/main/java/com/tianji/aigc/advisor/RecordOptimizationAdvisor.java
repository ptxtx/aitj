package com.tianji.aigc.advisor;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;

public class RecordOptimizationAdvisor implements BaseAdvisor {
    private final MyChatMemoryRepository myChatMemoryRepository;
    public RecordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        this.myChatMemoryRepository = myChatMemoryRepository;
    }
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        if (chatClientResponse == null || chatClientResponse.chatResponse() == null) {
            return chatClientResponse;
        }
        //获取大模型的响应内容
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        //获取响应内容，判断内容是否是智能体的名称
        String text = StrUtil.trim(chatResponse.getResult().getOutput().getText());
        AgentTypeEnum agentType = AgentTypeEnum.agentNameOf(text);
        if(agentType!=null){
            String conversationId = MapUtil.getStr(chatClientResponse.context(), ChatMemory.CONVERSATION_ID);
            //需要优化记录
            myChatMemoryRepository.optimization(conversationId);

        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER-100;
    }
}
