package cn.enjoy.mall.service.saga.impl;

import cn.enjoy.dao.MessageLogCallbackMapper;
import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mall.service.saga.SaveMessageLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Classname SaveMessageLogServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/22 21:53
 * Version 1.0
 */
@Service("saveMessageLogService")
public class SaveMessageLogServiceImpl implements SaveMessageLogService {

    @Autowired
    private MessageLogCallbackMapper messageLogCallbackMapper;

    @Override
    public boolean saveMessageLog(MessageLog messageLog) {
        int i = messageLogCallbackMapper.insertC(messageLog);
        return i == 1;
    }

    @Override
    public boolean compensateMessageLog(MessageLog messageLog) {
        return true;
    }
}
