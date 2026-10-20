package com.tianji.aigc.config;

import com.tianji.aigc.enums.AgentTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 天机学堂 MCP 工具授权配置。
 *
 * <p>Spring AI 负责 MCP 连接和工具发现，本配置只负责业务侧的 Agent 工具白名单。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tj.ai.mcp")
public class McpProperties {

    /** 是否允许业务 Agent 使用 MCP 工具。 */
    private boolean enabled = false;

    /** MCP 配置错误时是否快速失败。 */
    private boolean failFast = true;

    /** key 为 AgentTypeEnum 名称，value 为允许使用的 MCP 工具名。 */
    private Map<String, List<String>> agentTools = Collections.emptyMap();

    public Set<String> allowedTools(AgentTypeEnum agentType) {
        if (agentType == null || agentTools == null || agentTools.isEmpty()) {
            return Set.of();
        }
        List<String> configured = agentTools.getOrDefault(
                agentType.name().toUpperCase(Locale.ROOT),
                List.of()
        );
        if (configured == null || configured.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String toolName : configured) {
            if (toolName != null && !toolName.isBlank()) {
                result.add(toolName.trim());
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
