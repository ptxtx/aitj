package com.tianji.aigc.agent;

import com.tianji.aigc.advisor.RecordOptimizationAdvisor;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteAgent extends AbstractAgent{
    private final SystemPromptConfig systemPromptConfig;
    private final RecordOptimizationAdvisor recordOptimizationAdvisor;
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getRouteAgentSystemMessage().get();
    }
    @Override
    public List<Advisor> advisors() {
        return List.of(recordOptimizationAdvisor);
    }

}
