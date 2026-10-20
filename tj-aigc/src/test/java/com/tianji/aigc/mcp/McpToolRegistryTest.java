package com.tianji.aigc.mcp;

import com.tianji.aigc.config.McpProperties;
import com.tianji.aigc.enums.AgentTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolRegistryTest {

    @Test
    void shouldReturnNoToolsWhenMcpIsDisabled() {
        McpProperties properties = new McpProperties();
        McpToolRegistry registry = new McpToolRegistry(
                () -> {
                    throw new AssertionError("MCP 关闭时不应读取远程工具");
                },
                properties
        );

        assertEquals(0, registry.toolsFor(AgentTypeEnum.KNOWLEDGE).length);
    }

    @Test
    void shouldSelectOnlyAllowListedTools() {
        McpProperties properties = enabledProperties(true,
                Map.of("KNOWLEDGE", List.of("knowledge_search")));
        ToolCallback allowed = tool("knowledge_search");
        ToolCallback denied = tool("filesystem_delete");

        McpToolRegistry registry = new McpToolRegistry(
                () -> new ToolCallback[]{allowed, denied}, properties
        );
        ToolCallback[] selected = registry.toolsFor(AgentTypeEnum.KNOWLEDGE);

        assertEquals(1, selected.length);
        assertSame(allowed, selected[0]);
    }

    @Test
    void shouldFailFastWhenConfiguredToolDoesNotExist() {
        McpProperties properties = enabledProperties(true,
                Map.of("KNOWLEDGE", List.of("missing_tool")));
        McpToolRegistry registry = new McpToolRegistry(
                () -> new ToolCallback[]{tool("knowledge_search")}, properties
        );

        assertThrows(IllegalStateException.class,
                () -> registry.toolsFor(AgentTypeEnum.KNOWLEDGE));
    }

    @Test
    void shouldDegradeWhenProviderIsMissingAndFailFastIsDisabled() {
        McpProperties properties = enabledProperties(false,
                Map.of("KNOWLEDGE", List.of("knowledge_search")));
        McpToolRegistry registry = new McpToolRegistry(
                () -> {
                    throw new IllegalStateException("MCP provider unavailable");
                },
                properties
        );

        assertEquals(0, registry.toolsFor(AgentTypeEnum.KNOWLEDGE).length);
    }

    private McpProperties enabledProperties(boolean failFast, Map<String, List<String>> agentTools) {
        McpProperties properties = new McpProperties();
        properties.setEnabled(true);
        properties.setFailFast(failFast);
        properties.setAgentTools(agentTools);
        return properties;
    }

    private ToolCallback tool(String name) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description("test tool")
                .inputSchema("{}")
                .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String toolInput) {
                return "ok";
            }
        };
    }
}
