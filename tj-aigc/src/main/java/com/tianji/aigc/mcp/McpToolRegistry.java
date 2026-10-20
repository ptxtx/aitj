package com.tianji.aigc.mcp;

import com.tianji.aigc.config.McpProperties;
import com.tianji.aigc.enums.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 根据 Agent 白名单选择 MCP 工具。
 *
 * <p>禁止使用通配符。新增 MCP 工具后必须显式加入对应 Agent 的白名单，
 * 避免远程服务扩容工具集合后自动扩大模型权限。</p>
 */
@Slf4j
@Component
public class McpToolRegistry {

    private static final ToolCallback[] NO_TOOLS = new ToolCallback[0];

    private final Supplier<ToolCallback[]> toolCallbackSupplier;
    private final McpProperties properties;

    @Autowired
    public McpToolRegistry(ObjectProvider<SyncMcpToolCallbackProvider> provider,
                           McpProperties properties) {
        this(() -> {
            SyncMcpToolCallbackProvider callbackProvider = provider.getIfAvailable();
            if (callbackProvider == null) {
                throw new IllegalStateException(
                        "MCP 已启用，但 Spring 容器中不存在 SyncMcpToolCallbackProvider"
                );
            }
            return callbackProvider.getToolCallbacks();
        }, properties);
    }

    /** 仅用于隔离工具选择逻辑，便于不启动 MCP 连接即可进行单元测试。 */
    McpToolRegistry(Supplier<ToolCallback[]> toolCallbackSupplier,
                    McpProperties properties) {
        this.toolCallbackSupplier = toolCallbackSupplier;
        this.properties = properties;
    }

    public ToolCallback[] toolsFor(AgentTypeEnum agentType) {
        if (!properties.isEnabled()) {
            return NO_TOOLS;
        }

        Set<String> allowList = properties.allowedTools(agentType);
        if (allowList.isEmpty()) {
            return NO_TOOLS;
        }

        try {
            Map<String, ToolCallback> discovered = indexByName(toolCallbackSupplier.get());
            Set<String> missing = allowList.stream()
                    .filter(name -> !discovered.containsKey(name))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            if (!missing.isEmpty()) {
                return handleFailure(
                        "Agent " + agentType + " 配置了不存在的 MCP 工具: " + missing
                                + "，已发现工具: " + discovered.keySet(),
                        null
                );
            }

            ToolCallback[] selected = allowList.stream()
                    .map(discovered::get)
                    .toArray(ToolCallback[]::new);
            log.debug("Agent {} 已装载 MCP 工具 {}", agentType, allowList);
            return selected;
        } catch (RuntimeException exception) {
            return handleFailure("读取 MCP 工具列表失败，agent=" + agentType, exception);
        }
    }

    private Map<String, ToolCallback> indexByName(ToolCallback[] callbacks) {
        Map<String, ToolCallback> result = new LinkedHashMap<>();
        if (callbacks == null) {
            return result;
        }
        Arrays.stream(callbacks).forEach(callback -> {
            String name = callback.getToolDefinition().name();
            ToolCallback previous = result.putIfAbsent(name, callback);
            if (previous != null) {
                throw new IllegalStateException("发现重名 MCP 工具: " + name);
            }
        });
        return result;
    }

    private ToolCallback[] handleFailure(String message, RuntimeException cause) {
        if (properties.isFailFast()) {
            if (cause == null) {
                throw new IllegalStateException(message);
            }
            throw new IllegalStateException(message, cause);
        }
        if (cause == null) {
            log.warn("{}，本次请求降级为不使用 MCP 工具", message);
        } else {
            log.warn("{}，本次请求降级为不使用 MCP 工具", message, cause);
        }
        return NO_TOOLS;
    }
}
