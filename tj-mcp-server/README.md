# 天机学堂 MCP Server

该模块是独立的 MCP Server，通过 SSE 暴露经过审核的只读学习工具：

- `buildLearningPlan`：生成结构化学习计划；
- `calculateLearningProgress`：计算学习进度。

## 启动顺序

1. 启动 `TianjiMcpServerApplication`，默认端口为 `8095`；
2. 给 AIGC 服务设置环境变量 `TJ_AI_MCP_ENABLED=true`；
3. 启动 `AIGCApplication`；
4. 向知识问答 Agent 提问，例如：`我每周能学 8 小时，请给我制定一份 4 周 Spring AI 学习计划`。

如果 MCP Server 不在本机，可在 AIGC 服务中设置：

```text
TJ_MCP_SERVER_URL=http://实际主机:8095
```

MCP Server 不持有模型密钥、用户会话，也不开放下单等写操作。新增远程工具后，还必须在
`tj-aigc/src/main/resources/application.yml` 的 `tj.ai.mcp.agent-tools` 中显式加入相应 Agent 白名单。
