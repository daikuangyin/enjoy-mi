package cn.enjoy.kill.service.impl.tcc;

import cn.enjoy.mall.model.Order;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;

/**
 * @Classname KillPayServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/20 21:24
 * Version 1.0
 */
@Service
public class KillPayServiceImpl implements KillPayService {
    @Override
    public Order updateStatusByActionIdTcc(BusinessActionContext businessActionContext, String actionId) {
        return null;
    }

    @Override
    public boolean updateStatusByActionIdTccCommit(BusinessActionContext businessActionContext) {
        return false;
    }

    @Override
    public boolean updateStatusByActionIdTccRollback(BusinessActionContext businessActionContext) {
        return false;
    }
}
