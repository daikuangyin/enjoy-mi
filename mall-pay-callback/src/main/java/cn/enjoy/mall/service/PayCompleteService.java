package cn.enjoy.mall.service;

import cn.enjoy.mall.model.MessageLog;

public interface PayCompleteService {

    void payCompleteBusiness(String actionId, MessageLog messageLog);

    boolean payCompleteBusinessTcc(String actionId, MessageLog messageLog);

    boolean payCompleteBusinessSaga(String actionId, MessageLog messageLog);
}
