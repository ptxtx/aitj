package com.tianji.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证真实的 SSE 握手、工具发现和远程工具调用。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldDiscoverAndInvokeToolsOverSse() {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder("http://127.0.0.1:" + port)
                .sseEndpoint("/sse")
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(5))
                .clientInfo(new McpSchema.Implementation("tianji-aigc", "1.0.0"))
                .build();

        try {
            client.initialize();

            Set<String> toolNames = client.listTools().tools().stream()
                    .map(McpSchema.Tool::name)
                    .collect(Collectors.toSet());
            assertTrue(toolNames.contains("buildLearningPlan"));
            assertTrue(toolNames.contains("calculateLearningProgress"));

            Set<String> callbackNames = Set.of(
                    new SyncMcpToolCallbackProvider(client).getToolCallbacks()
            ).stream()
                    .map(ToolCallback::getToolDefinition)
                    .map(definition -> definition.name())
                    .collect(Collectors.toSet());
            assertTrue(callbackNames.contains("tianji_aigc_buildLearningPlan"));
            assertTrue(callbackNames.contains("tianji_aigc_calculateLearningProgress"));

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    "buildLearningPlan",
                    Map.of("topic", "Spring AI", "weeks", 4, "weeklyHours", 8)
            ));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            assertTrue(result.content().toString().contains("Spring AI"));
        } finally {
            client.closeGracefully();
        }
    }
}
