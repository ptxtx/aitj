package com.tianji.mcp.config;

import com.tianji.mcp.tools.LearningTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 将经过审核的 Java 方法注册为 MCP 工具。 */
@Configuration
public class McpToolConfiguration {

    @Bean
    ToolCallbackProvider learningToolCallbackProvider(LearningTools learningTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(learningTools)
                .build();
    }
}
