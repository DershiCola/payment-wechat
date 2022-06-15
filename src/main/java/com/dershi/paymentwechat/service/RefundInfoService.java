package com.dershi.paymentwechat.service;

import com.dershi.paymentwechat.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RefundInfoService extends IService<RefundInfo> {
    RefundInfo createRefundInfoByOrderNo(String orderNo, String reason);
    void updateRefundInfoForWx(String content);
    void updateRefundInfoForAli(String refundNo, String content, String status);
    String getRefundStatusByRefundNo(String refundNo);
    void updateRefundStatusByRefundNo(String refundNo, String refundStatus);
    List<RefundInfo> getProcessingRefundsByDuration(int minutes);
}
