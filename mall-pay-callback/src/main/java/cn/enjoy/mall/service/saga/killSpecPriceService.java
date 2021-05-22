package cn.enjoy.mall.service.saga;

public interface killSpecPriceService {
    boolean updateKillSpecPrice(String businessKey, Integer specGoodsId);

    boolean compensateKillSpecPrice(String businessKey, Integer specGoodsId);
}
