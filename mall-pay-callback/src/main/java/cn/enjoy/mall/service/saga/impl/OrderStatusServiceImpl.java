package cn.enjoy.mall.service.saga.impl;

import cn.enjoy.mall.service.IKillPayService;
import cn.enjoy.mall.service.saga.OrderStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Classname OrderStatusServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/22 21:12
 * Version 1.0
 */
@Service("orderStatusService")
public class OrderStatusServiceImpl implements OrderStatusService {

    @Autowired
    private IKillPayService iKillPayService;

    @Override
    public boolean updateOrderStatus(String businessKey, String actionId) {
        return iKillPayService.updateStatusByActionIdSaga(businessKey, actionId);
    }

    @Override
    public boolean compensateOrderStatus(String businessKey, String actionId) {
        return iKillPayService.compensateOrderStatusSaga(businessKey, actionId);
    }
}
