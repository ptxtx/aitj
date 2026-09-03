package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {//和数据库有交互才做这个继承

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

    List<SessionVO.Example> hotExamples(Integer num);
}
