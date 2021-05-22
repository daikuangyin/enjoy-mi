package cn.enjoy.dao;

import cn.enjoy.mall.model.MessageLog;

import java.util.List;

/**
 * @Classname MessageLogMapper
 * @Description TODO
 * @Author Jack
 * Date 2020/10/13 16:50
 * Version 1.0
 */
public interface MessageLogMapper {

    int insertP(MessageLog record);

    MessageLog selectPByPrimaryKey(Long messageId);

    int updatePByPrimaryKeySelective(MessageLog record);

    List<MessageLog> queryMessageLogByTimeAndStatus(MessageLog record);
}
