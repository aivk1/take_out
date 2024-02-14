package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart = ShoppingCart.builder()
                        .userId(userId)
                                .build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if(shoppingCarts==null && shoppingCarts.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //插入orders表
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        //插入ordersDetail表
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart cart:shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        shoppingCartMapper.deleteById(userId);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content","订单号："+outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public void remind(Long id) {
        Orders order = orderMapper.getById(id);
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号："+order.getNumber());
        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);
    }
    @Transactional
    public void repetition(Long id){
        Orders order = orderMapper.getById(id);
        orderMapper.insert(order);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        orderMapper.insert(order);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        if(orderDetails!=null && orderDetails.size()!=0){
            for(OrderDetail detail:orderDetails){
                detail.setOrderId(order.getId());
            }
            orderDetailMapper.insertBatch(orderDetails);
        }
    }

    public PageResult history(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> page = orderMapper.history(ordersPageQueryDTO);
        for(OrderVO orderVO:page){
            orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(orderVO.getId()));
        }
         return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders order = orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }
    public OrderStatisticsVO getOrderStatisticsVO(){
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.CONFIRMED, LocalDateTime.now());
        orderStatisticsVO.setConfirmed(ordersList.size());
        ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now());
        orderStatisticsVO.setDeliveryInProgress(ordersList.size());
        ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.TO_BE_CONFIRMED, LocalDateTime.now());
        orderStatisticsVO.setToBeConfirmed(ordersList.size());
        return orderStatisticsVO;
    }
    public void cancel(OrdersCancelDTO ordersCancelDTO){
        Orders order = orderMapper.getById(ordersCancelDTO.getId());
        //退款


        order.setStatus(Orders.CANCELLED);
        order.setPayStatus(Orders.REFUND);
        order.setCancelReason(ordersCancelDTO.getCancelReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }
    public void complete(Long id){
        Orders order = orderMapper.getById(id);
        if(order!=null) {
            order.setStatus(Orders.COMPLETED);
            order.setDeliveryTime(LocalDateTime.now());
            orderMapper.update(order);
        }
        else{
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }
    public void reject(OrdersRejectionDTO ordersRejectionDTO){
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        if(order!=null){
            order.setStatus(Orders.CANCELLED);
            order.setRejectionReason(ordersRejectionDTO.getRejectionReason());
            orderMapper.update(order);
        }
        else{
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }

    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders order = orderMapper.getById(ordersConfirmDTO.getId());
        if(order!=null){
            order.setStatus(Orders.CONFIRMED);
            orderMapper.update(order);
        }
        else{
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }
    public void delivery(Long id){
        Orders order = orderMapper.getById(id);
        if(order!=null){
            order.setStatus(Orders.DELIVERY_IN_PROGRESS);
            orderMapper.update(order);
        }
        else{
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        for(OrderVO orderVO:orders){
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetails);
        }
        return new PageResult(orders.getTotal(), orders.getResult());
    }

}
