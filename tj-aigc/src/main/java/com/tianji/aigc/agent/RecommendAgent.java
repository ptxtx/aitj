package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//课程推荐智能体
@Component
@RequiredArgsConstructor
public class RecommendAgent extends AbstractAgent{
    private final SystemPromptConfig systemPromptConfig;
    private final CourseTools courseTools;
    private final VectorStore vectorStore;
    private final SimpleLoggerAdvisor simpleLoggerAdvisor;
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.RECOMMEND;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getRecommendAgentSystemMessage().get();
    }

    @Override
    public Object[] tools() {
        return new Object[]{courseTools};
    }

    @Override
    public List<Advisor> advisors() {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)
                        .topK(6)
                        .build())
                .build();
        return List.of(qaAdvisor, simpleLoggerAdvisor);
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        Long userId = UserContext.getUser();
        return Map.of(Constant.REQUEST_ID, requestId,Constant.USER_ID, userId);
    }
}
