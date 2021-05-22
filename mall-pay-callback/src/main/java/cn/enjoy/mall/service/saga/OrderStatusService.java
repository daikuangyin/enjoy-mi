package cn.enjoy.mall.service.saga;

public interface OrderStatusService {

    boolean updateOrderStatus(String businessKey,String actionId);

    boolean compensateOrderStatus(String businessKey,String actionId);
}
