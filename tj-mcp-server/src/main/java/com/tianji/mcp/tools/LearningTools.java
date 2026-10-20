package com.tianji.mcp.tools;

import com.tianji.mcp.domain.LearningPlan;
import com.tianji.mcp.domain.LearningProgress;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 可通过 MCP 远程调用的学习工具。
 *
 * <p>第一版只暴露无副作用的计算类工具，避免远程调用直接产生订单或修改业务数据。
 * 后续新增工具时仍需在 AIGC 服务的 Agent 白名单中显式授权。</p>
 */
@Component
public class LearningTools {

    private static final int MIN_WEEKS = 1;
    private static final int MAX_WEEKS = 24;
    private static final int MIN_WEEKLY_HOURS = 1;
    private static final int MAX_WEEKLY_HOURS = 40;

    @Tool(
            name = "buildLearningPlan",
            description = "根据学习主题、计划周数和每周可投入小时数生成分周学习计划。仅用于学习规划，不会修改用户或课程数据。"
    )
    public LearningPlan buildLearningPlan(
            @ToolParam(description = "学习主题，例如 Java、Spring Boot、Spring AI") String topic,
            @ToolParam(description = "计划持续周数，范围 1 到 24") Integer weeks,
            @ToolParam(description = "每周可投入小时数，范围 1 到 40") Integer weeklyHours) {
        String normalizedTopic = requireTopic(topic);
        int normalizedWeeks = requireRange("weeks", weeks, MIN_WEEKS, MAX_WEEKS);
        int normalizedWeeklyHours = requireRange(
                "weeklyHours", weeklyHours, MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS
        );

        List<LearningPlan.WeeklyMilestone> milestones = new ArrayList<>(normalizedWeeks);
        for (int week = 1; week <= normalizedWeeks; week++) {
            milestones.add(new LearningPlan.WeeklyMilestone(
                    week,
                    focusFor(week, normalizedWeeks, normalizedTopic),
                    normalizedWeeklyHours,
                    deliverableFor(week, normalizedWeeks, normalizedTopic)
            ));
        }

        return new LearningPlan(
                normalizedTopic,
                normalizedWeeks,
                normalizedWeeklyHours,
                normalizedWeeks * normalizedWeeklyHours,
                List.copyOf(milestones),
                "建议按 30% 理论、50% 编码实践、20% 复盘输出分配时间，并每周保留一次可验证的成果。"
        );
    }

    @Tool(
            name = "calculateLearningProgress",
            description = "根据已完成小节数和总小节数计算课程进度、剩余小节及学习状态。不会修改课程进度。"
    )
    public LearningProgress calculateLearningProgress(
            @ToolParam(description = "已完成的小节数，不能小于 0") Integer completedSections,
            @ToolParam(description = "课程总小节数，必须大于 0") Integer totalSections) {
        if (completedSections == null || completedSections < 0) {
            throw new IllegalArgumentException("completedSections 必须大于等于 0");
        }
        if (totalSections == null || totalSections <= 0) {
            throw new IllegalArgumentException("totalSections 必须大于 0");
        }
        if (completedSections > totalSections) {
            throw new IllegalArgumentException("completedSections 不能大于 totalSections");
        }

        int percent = (int) Math.round(completedSections * 100.0 / totalSections);
        return new LearningProgress(
                completedSections,
                totalSections,
                percent,
                totalSections - completedSections,
                statusFor(percent)
        );
    }

    private String requireTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }
        String normalized = topic.trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("topic 长度不能超过 80 个字符");
        }
        return normalized;
    }

    private int requireRange(String name, Integer value, int min, int max) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(name + " 必须在 " + min + " 到 " + max + " 之间");
        }
        return value;
    }

    private String focusFor(int week, int totalWeeks, String topic) {
        if (week == 1) {
            return topic + " 基础概念与开发环境";
        }
        if (week == totalWeeks) {
            return topic + " 综合项目、查漏补缺与面试复盘";
        }
        if (week <= Math.max(2, totalWeeks / 2)) {
            return topic + " 核心知识与小型练习";
        }
        return topic + " 项目实践、调试与工程化";
    }

    private String deliverableFor(int week, int totalWeeks, String topic) {
        if (week == totalWeeks) {
            return "完成一个可运行的 " + topic + " 综合项目并整理 README";
        }
        return "完成第 " + week + " 周代码练习、笔记和自测清单";
    }

    private String statusFor(int percent) {
        if (percent == 100) {
            return "已完成";
        }
        if (percent >= 75) {
            return "冲刺阶段";
        }
        if (percent >= 25) {
            return "持续学习中";
        }
        return "起步阶段";
    }
}
