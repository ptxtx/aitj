package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {//和数据库有交互才做这个继承

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

    List<SessionVO.Example> hotExamples(Integer num);

    /**
     * 根据会话id查询消息列表
     *
     * @param sessionId 会话id
     * @return 消息列表
     */
    List<MessageVO> queryBySessionId(String sessionId);

    void update(String sessionId,String title,Long userId);

    Map<String, List<ChatSessionVO>> queryHistorySession();

    void deleteHistorySession(String sessionId);

    void updateTitle(String sessionId, String title);

    boolean needsTitle(String sessionId, Long userId);

    void initTitle(String sessionId, String title, Long userId);
}
