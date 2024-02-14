package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    void remind(Long id);

    void repetition(Long id);
    PageResult history(OrdersPageQueryDTO ordersPageQueryDTO);
    OrderVO getOrderDetail(Long id);
    public OrderStatisticsVO getOrderStatisticsVO();
    public void cancel(OrdersCancelDTO ordersCancelDTO);
    void complete(Long id);

    void reject(OrdersRejectionDTO ordersRejectionDTO);

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void delivery(Long id);

    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);
}
