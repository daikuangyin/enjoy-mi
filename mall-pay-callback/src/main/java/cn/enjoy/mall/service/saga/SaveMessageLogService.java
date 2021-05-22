package cn.enjoy.mall.service.saga;

import cn.enjoy.mall.model.MessageLog;

public interface SaveMessageLogService {
    boolean saveMessageLog(MessageLog messageLog);

    boolean compensateMessageLog(MessageLog messageLog);
}
