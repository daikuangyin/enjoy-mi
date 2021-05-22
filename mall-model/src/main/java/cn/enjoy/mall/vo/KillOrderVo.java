package cn.enjoy.mall.vo;

import java.io.Serializable;

public class KillOrderVo implements Serializable{
    private static final long serialVersionUID = 1L;

    private KillGoodsSpecPriceDetailVo killGoodsSpecPriceDetailVo;
    private Long addressId;
    private String userId;
    private Long orderId;

    public KillGoodsSpecPriceDetailVo getKillGoodsSpecPriceDetailVo() {
        return killGoodsSpecPriceDetailVo;
    }

    public void setKillGoodsSpecPriceDetailVo(KillGoodsSpecPriceDetailVo killGoodsSpecPriceDetailVo) {
        this.killGoodsSpecPriceDetailVo = killGoodsSpecPriceDetailVo;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
