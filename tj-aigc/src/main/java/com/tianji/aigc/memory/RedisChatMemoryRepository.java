package com.tianji.aigc.memory;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

public class RedisChatMemoryRepository implements ChatMemoryRepository {
    // 默认redis中key的前缀
    public static final String DEFAULT_PREFIX = "CHAT:";

    private final String prefix;
    public RedisChatMemoryRepository() {
        this.prefix = DEFAULT_PREFIX;
    }


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void deleteByConversationId(String conversationId) {
        String redisKey = this.getKey(conversationId);
        this.stringRedisTemplate.delete(redisKey);

    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = this.stringRedisTemplate.keys(this.prefix + "*");
        if (null == keys) {
            return List.of();
        }
        return StreamUtil.of(keys)
                .map(key -> StrUtil.replace(key, this.prefix, ""))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 生成Redis键名用于存储会话消息
        var redisKey = this.getKey(conversationId);
        // 获取Redis列表操作对象
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);

        // 从Redis列表中获取所有的数据
        var messages = listOps.range(0, -1);
        // 将Redis返回的字符串列表转换为Message对象列表
        return CollStreamUtil.toList(messages, MessageUtil::toMessage);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        //注意：messages是全量消息列表(所以要先删除再保存）
        var redisKey=this.getKey(conversationId);
        BoundListOperations<String, String> listOps = this.stringRedisTemplate.boundListOps(redisKey);

        //将原有消息全部删除
        this.deleteByConversationId(conversationId);

        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson(message)));


    }
    private String getKey(String conversationId) {
        return prefix + conversationId;
    }
}
