package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.UserContext;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Builder
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
    private final SessionProperties sessionProperties;
    @Override
    public SessionVO createSession(Integer num) {
      var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
      sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));
      //随机生成3个热门问题 放到sessionVO中
      sessionVO.setSessionId(IdUtil.simpleUUID());

      //保存会话数据到数据库
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())//当前用户id
                .build();
        save(chatSession);

        return sessionVO;

    }

    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), 3);
    }
}
