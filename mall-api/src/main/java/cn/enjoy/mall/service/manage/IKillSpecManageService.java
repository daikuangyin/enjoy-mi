package cn.enjoy.mall.service.manage;

import cn.enjoy.core.utils.GridModel;
import cn.enjoy.mall.model.KillGoodsPrice;
import cn.enjoy.mall.vo.KillGoodsSpecPriceDetailVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@LocalTCC
@RequestMapping("/product/mall/service/manage/IKillSpecManageService")
public interface IKillSpecManageService {

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    int delete(@RequestParam("id") Integer id);

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    int save(@RequestBody KillGoodsPrice record);

    @RequestMapping(value = "/selectCountBySpecGoodId", method = RequestMethod.POST)
    int selectCountBySpecGoodId(@RequestParam("specGoodsId") Integer specGoodsId);

    @RequestMapping(value = "/selectByPrimaryKey", method = RequestMethod.POST)
    KillGoodsPrice selectByPrimaryKey(@RequestParam("id") Integer id);

    @RequestMapping(value = "/detailBySpecGoodId", method = RequestMethod.POST)
    KillGoodsSpecPriceDetailVo detailBySpecGoodId(@RequestParam("id") Integer id);

    @RequestMapping(value = "/detailById", method = RequestMethod.POST)
    KillGoodsSpecPriceDetailVo detailById(@RequestParam("id") Integer id);

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    int update(@RequestBody KillGoodsPrice record);

    @RequestMapping(value = "/updateSecKill", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    int updateSecKill(@RequestBody KillGoodsPrice record);

    @RequestMapping(value = "/updateBySpecGoodsId", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    int updateBySpecGoodsId(@RequestBody KillGoodsPrice record);

    @RequestMapping(value = "/updateBySpecGoodsIdSaga", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    boolean updateBySpecGoodsIdSaga(@RequestParam("businessKey") String businessKey,@RequestParam("specGoodsId") Integer specGoodsId);

    @RequestMapping(value = "/compensateKillSpecPriceSaga", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    boolean compensateKillSpecPriceSaga(@RequestParam("businessKey") String businessKey,@RequestParam("specGoodsId") Integer specGoodsId);

    @TwoPhaseBusinessAction(name = "updateBySpecGoodsIdTcc", commitMethod = "updateBySpecGoodsIdTccCommit",
            rollbackMethod = "updateBySpecGoodsIdTccRollback")
    @RequestMapping(value = "/updateBySpecGoodsIdTcc",
            method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    boolean updateBySpecGoodsIdTcc(@RequestBody BusinessActionContext businessActionContext,
                                   @BusinessActionContextParameter(paramName = "killGoodsPriceStr")
    @RequestParam("killGoodsPriceStr") String killGoodsPriceStr);

    @RequestMapping(value = "/updateBySpecGoodsIdTccCommit", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    boolean updateBySpecGoodsIdTccCommit(@RequestBody BusinessActionContext businessActionContext);

    @RequestMapping(value = "/updateBySpecGoodsIdTccRollback", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    boolean updateBySpecGoodsIdTccRollback(@RequestBody BusinessActionContext businessActionContext);

    @RequestMapping(value = "/flushCache", method = RequestMethod.POST)
    void flushCache(@RequestBody KillGoodsPrice record);

    @RequestMapping(value = "/queryByPage", method = RequestMethod.POST)
    GridModel<KillGoodsSpecPriceDetailVo> queryByPage(@RequestParam("name") String name, @RequestParam("page") int page,
                                                      @RequestParam("pageSize") int pageSize);

    @RequestMapping(value = "/queryView", method = RequestMethod.POST)
    GridModel<KillGoodsSpecPriceDetailVo> queryView(@RequestParam("page") int page, @RequestParam("pageSize") int pageSize);

}
