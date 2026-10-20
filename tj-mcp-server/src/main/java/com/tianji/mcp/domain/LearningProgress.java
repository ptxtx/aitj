package com.tianji.mcp.domain;

/** MCP 工具返回的学习进度计算结果。 */
public record LearningProgress(
        int completedSections,
        int totalSections,
        int percent,
        int remainingSections,
        String status
) {
}
