package cn.enjoy.mall.dao;

import cn.enjoy.mall.model.TccTransactionStatus;

public interface TccTransactionStatusMapper {
    TccTransactionStatus queryStatusById(TccTransactionStatus status);

    int insertStatus(TccTransactionStatus status);

    int updateStatus(TccTransactionStatus status);
}
