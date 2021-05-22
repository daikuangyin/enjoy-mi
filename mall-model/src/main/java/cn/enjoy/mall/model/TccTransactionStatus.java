package cn.enjoy.mall.model;

/**
 * @Classname TccTransactionStatus
 * @Description TODO
 * @Author Jack
 * Date 2020/10/21 15:39
 * Version 1.0
 */
public class TccTransactionStatus {
    private String txId;

    private String branchId;

    private Long createTime;

    private Long updateTime;

    private Integer status;

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
