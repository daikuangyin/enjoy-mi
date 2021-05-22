package cn.enjoy.kill.service.impl.tcc;

import cn.enjoy.mall.model.Order;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import org.springframework.web.bind.annotation.RequestParam;

@LocalTCC
public interface KillPayService {

    Order updateStatusByActionIdTcc(BusinessActionContext businessActionContext, @BusinessActionContextParameter(paramName = "actionId")
    @RequestParam("actionId") String actionId) ;

    public boolean updateStatusByActionIdTccCommit(BusinessActionContext businessActionContext);

    public boolean updateStatusByActionIdTccRollback(BusinessActionContext businessActionContext);
}
