package com.tianji.mcp.tools;

import com.tianji.mcp.domain.LearningPlan;
import com.tianji.mcp.domain.LearningProgress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningToolsTest {

    private final LearningTools learningTools = new LearningTools();

    @Test
    void shouldBuildStructuredLearningPlan() {
        LearningPlan plan = learningTools.buildLearningPlan(" Spring AI ", 4, 8);

        assertEquals("Spring AI", plan.topic());
        assertEquals(4, plan.milestones().size());
        assertEquals(32, plan.totalHours());
        assertEquals(8, plan.milestones().get(0).suggestedHours());
    }

    @Test
    void shouldCalculateProgress() {
        LearningProgress progress = learningTools.calculateLearningProgress(3, 8);

        assertEquals(38, progress.percent());
        assertEquals(5, progress.remainingSections());
        assertEquals("持续学习中", progress.status());
    }

    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> learningTools.buildLearningPlan("Java", 0, 8));
        assertThrows(IllegalArgumentException.class,
                () -> learningTools.calculateLearningProgress(9, 8));
    }
}
