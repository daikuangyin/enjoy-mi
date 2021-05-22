package cn.enjoy.mall.service.saga.impl;

import cn.enjoy.mall.service.manage.IKillSpecManageService;
import cn.enjoy.mall.service.saga.killSpecPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Classname killSpecPriceServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/22 21:29
 * Version 1.0
 */
@Service("killSpecPriceService")
public class KillSpecPriceServiceImpl implements killSpecPriceService {

    @Autowired
    private IKillSpecManageService killSpecManageService;

    @Override
    public boolean updateKillSpecPrice(String businessKey, Integer specGoodsId) {
        return killSpecManageService.updateBySpecGoodsIdSaga(businessKey, specGoodsId);
    }

    @Override
    public boolean compensateKillSpecPrice(String businessKey, Integer specGoodsId) {
        return killSpecManageService.compensateKillSpecPriceSaga(businessKey,specGoodsId);
    }
}
