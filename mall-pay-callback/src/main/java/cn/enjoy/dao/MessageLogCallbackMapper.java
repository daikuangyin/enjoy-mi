package cn.enjoy.dao;

import cn.enjoy.mall.model.MessageLog;

/**
 * @Classname MessageLogMapper
 * @Description TODO
 * @Author Jack
 * Date 2020/10/13 16:50
 * Version 1.0
 */
public interface MessageLogCallbackMapper {

    int insertC(MessageLog record);

    MessageLog selectCByPrimaryKey(Long messageId);
}
