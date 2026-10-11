package com.tianji.learning.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tianji.api.client.aigc.AigcClient;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.service.AIService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private final AigcClient aigcClient;
    private final IInteractionReplyService iInteractionReplyService;

    @Value("${tj.ai.user-id:9999}")
    private Long aiUserId;
    @Override
    public void autoReply(InteractionQuestion interactionQuestion) {
        String question = StrUtil.format("""
                这是一个学生提出的问题，请以专业的角度进行回答，不要随意编造。
                标题：{} 。
                描述：{} 。""", interactionQuestion.getTitle(), interactionQuestion.getDescription());
        UserContext.setUser(interactionQuestion.getUserId());
        String reply = aigcClient.chatText(question);

        //构建系统自动回复数据对象 保存回答
        ReplyDTO replyDTO = ReplyDTO.builder()
                .content(reply)
                .anonymity(false)       // 明确显示系统回复身份
                .questionId(interactionQuestion.getId())  // 关联原始问题ID
                .isStudent(false)       // 标记为非学生回复
                .userId(aiUserId)
                .build();

        this.iInteractionReplyService.saveReply(replyDTO);

    }
}
