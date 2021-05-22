package cn.enjoy;

import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mall.service.PayCompleteService;
import cn.enjoy.mq.OrderLogSender;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@SpringBootTest(classes = PayServiceCallbackApp.class)
public class MyTest {

    @Autowired
    private OrderLogSender orderLogSender;

    @Autowired
    private PayCompleteService payCompleteService;

    @Test
    public void test1() {
        MessageLog messageLog = new MessageLog();
        messageLog.setMessageId(768352473505796L);
        messageLog.setMessage("535891662626816");
        payCompleteService.payCompleteBusiness("535891662626816",messageLog);
    }
}
