package cn.enjoy;

import cn.enjoy.dao.MessageLogMapper;
import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mq.OrderLogSender;
import com.baidu.fsg.uid.impl.DefaultUidGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Classname MyTest
 * @Description TODO
 * @Author Jack
 * Date 2020/10/14 15:32
 * Version 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PayServiceApp.class)
public class MyTest {

    @Autowired
    private DefaultUidGenerator defaultUidGenerator;

    @Autowired
    private MessageLogMapper messageLogMapper;

    @Autowired
    private OrderLogSender orderLogSender;

    @Test
    public void test1() {
        MessageLog messageLog = new MessageLog();
        messageLog.setMessageId(defaultUidGenerator.getUID());
        messageLog.setMessage("12770913458184192");
        messageLog.setStatus(0);
        messageLog.setTryCount(0);
        Long now = System.currentTimeMillis();
        messageLog.setCreateTime(now);
        messageLog.setUpdateTime(now);
        //延迟5秒被定时器扫描判断是否需要重发
        messageLog.setDelayTime(now + 5000);
        messageLogMapper.insertP(messageLog);
        CorrelationData correlationData = new CorrelationData(messageLog.getMessageId().toString());
        orderLogSender.send(messageLog,correlationData);
    }
}
