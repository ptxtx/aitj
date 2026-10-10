package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import com.tianji.aigc.service.ChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class JdbcChatMemoryRepository implements ChatMemoryRepository, MyChatMemoryRepository {
    @Resource
    private ChatRecordService chatRecordService;

    @Override
    public List<String> findConversationIds() {
        List<ChatRecord> chatRecordList = this.chatRecordService.lambdaQuery()
                .select(ChatRecord::getConversationId)
                .list();
        return CollStreamUtil.toList(chatRecordList, ChatRecord::getConversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        this.deleteByConversationId(conversationId);
        //通过对话id 获取用户id
        String user = StrUtil.subBefore(conversationId, '_', true);
        Long userId = Long.parseLong(user);
        //批量保存数据到数据库
        List<ChatRecord> chatRecordList = CollStreamUtil.toList(messages, message ->
                ChatRecord.builder()
                        .data(MessageUtil.toJson(message))
                        .conversationId(conversationId)
                        .creater(userId)
                        .updater(userId)
                        .build());
        this.chatRecordService.saveBatch(chatRecordList);

    }

    @Override
    public void deleteByConversationId(String conversationId) {
        this.chatRecordService.lambdaUpdate().eq(ChatRecord::getConversationId, conversationId).remove();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatRecord> chatRecordList = this.chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return CollStreamUtil.toList(chatRecordList, chatRecord -> MessageUtil.toMessage(String.valueOf(chatRecord.getData())));
    }

    @Override
    public void optimization(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return;
        }

        List<ChatRecord> lastRecords = chatRecordService.lambdaQuery()
                .select(ChatRecord::getId)
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByDesc(ChatRecord::getId)
                .last("LIMIT 2")
                .list();

        if (lastRecords.size() == 2) {
            chatRecordService.removeByIds(
                    lastRecords.stream()
                            .map(ChatRecord::getId)
                            .toList()
            );
        }
    }
}
