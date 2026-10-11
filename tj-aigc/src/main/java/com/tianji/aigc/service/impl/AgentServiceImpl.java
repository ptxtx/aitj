package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

//基于路由工作流模式实现的智能体
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai",name = "chat-type",havingValue = "ROUTE")
public class AgentServiceImpl implements ChatService {

    @Override
    public void stop(String sessionId) {
        this.findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        //实现思路：先把问题发给路由智能体，根据路由结果调用对应智能体
        Agent routeAgent = this.findAgentByType(AgentTypeEnum.ROUTE);
        String routeResult = routeAgent.process(question, sessionId);
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.agentNameOf(routeResult);

        Agent agent = this.findAgentByType(agentTypeEnum);
        if(agent==null){
            //找不到对应的智能体，直接返回结果
            ChatEventVO chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(routeResult)
                    .build();
            return Flux.just(chatEventVO);
        }
        return agent.processStream(question, sessionId);
    }
    //根据类型找到对应的agent实例
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum){
        Map<String, Agent> agents = SpringUtil.getBeansOfType(Agent.class);
        for (Agent agent : agents.values()) {
            if (agent.getAgentType().equals(agentTypeEnum)) {
                return agent;
            }
        }
        return null;


    }

    @Override
    public String chatText(String question) {
        return "";
    }
}
