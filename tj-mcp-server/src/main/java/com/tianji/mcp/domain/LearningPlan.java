package com.tianji.mcp.domain;

import java.util.List;

/** MCP 工具返回的结构化学习计划。 */
public record LearningPlan(
        String topic,
        int weeks,
        int weeklyHours,
        int totalHours,
        List<WeeklyMilestone> milestones,
        String advice
) {

    public record WeeklyMilestone(
            int week,
            String focus,
            int suggestedHours,
            String deliverable
    ) {
    }
}
