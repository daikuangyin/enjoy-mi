package cn.enjoy.mall.service;

import cn.enjoy.mall.model.MessageLog;

import java.util.List;

public interface MessageLogService {

    int updateMessageLog(MessageLog messageLog);

    List<MessageLog> queryMessageLogByTimeAndStatus(MessageLog messageLog);
}
