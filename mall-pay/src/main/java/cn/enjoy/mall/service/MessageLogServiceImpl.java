package cn.enjoy.mall.service;

import cn.enjoy.dao.MessageLogMapper;
import cn.enjoy.mall.model.MessageLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Classname MessageLogServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/13 17:29
 * Version 1.0
 */
@Service
public class MessageLogServiceImpl implements MessageLogService {

    @Autowired
    private MessageLogMapper messageLogMapper;

    @Transactional
    @Override
    public int updateMessageLog(MessageLog messageLog) {
        return messageLogMapper.updatePByPrimaryKeySelective(messageLog);
    }

    @Override
    public List<MessageLog> queryMessageLogByTimeAndStatus(MessageLog messageLog) {
        return messageLogMapper.queryMessageLogByTimeAndStatus(messageLog);
    }
}
