package com.stylefeng.guns.rest.modular.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.baomidou.mybatisplus.plugins.Page;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.stylefeng.guns.api.alipay.AlipayServiceAPI;
import com.stylefeng.guns.api.alipay.vo.AlipayInfoVO;
import com.stylefeng.guns.api.alipay.vo.AlipayResultVO;
import com.stylefeng.guns.api.order.OrderServiceAPI;
import com.stylefeng.guns.api.order.vo.OrderVO;
import com.stylefeng.guns.core.util.TokenBucket;
import com.stylefeng.guns.core.util.ToolUtil;
import com.stylefeng.guns.rest.common.CurrentUser;
import com.stylefeng.guns.rest.modular.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry
 **/
@RestController
@Slf4j
@RequestMapping(value = "/order/")
public class OrderController {
    private static final String IMG_PRE = "http://img.meetingshop.cn/";

    @Reference(interfaceClass = OrderServiceAPI.class, group = "default", timeout = 3000, check = false, filter = "tracing")
    private OrderServiceAPI orderServiceAPI;

    @Reference(interfaceClass = OrderServiceAPI.class, group = "order2017", check = false, filter = "tracing")
    private OrderServiceAPI orderServiceAPI2017;

    @Reference(interfaceClass = AlipayServiceAPI.class, timeout = 3000, check = false, filter = "tracing")
    private AlipayServiceAPI alipayServiceAPI;

    private static TokenBucket tokenBucket = new TokenBucket();

    public ResponseVO error(Integer fieldId, String soldSeats, String seatsName) {
        return ResponseVO.serviceFail("网络异常");
    }

    // 购票
    @HystrixCommand(fallbackMethod = "error", commandProperties = {
            @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD"),
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value
                    = "4000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
    }, threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "1"),
            @HystrixProperty(name = "maxQueueSize", value = "10"),
            @HystrixProperty(name = "keepAliveTimeMinutes", value = "1000"),
            @HystrixProperty(name = "queueSizeRejectionThreshold", value = "8"),
            @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "12"),
            @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1500")
    })
    @RequestMapping(value = "buyTickets", method = RequestMethod.POST)
    public ResponseVO buyTickets(Integer fieldId, String soldSeats, String seatsName) {
        try {
            if (tokenBucket.getToken()) {
                // 验证售出的票是否为真
                boolean trueSeats = orderServiceAPI.isTrueSeats(fieldId + "", soldSeats);
                // 已经销售的座位里有无这些座位
                boolean notSoldSeats = orderServiceAPI.isNotSoldSeats(fieldId + "", soldSeats);

                //验证
                if (trueSeats && notSoldSeats) {
                    // 创建订单信息,注意获取登录人
                    String userId = CurrentUser.getCurrentUser();
                    if (userId == null && userId.trim().length() == 0) {
                        return ResponseVO.serviceFail("用户未登录");
                    } else {
                        OrderVO orderVO = orderServiceAPI.saveOrderInfo(fieldId, soldSeats, seatsName, Integer.parseInt(userId));
                        if (orderVO == null) {
                            log.error("购票未成功");
                            return ResponseVO.serviceFail("购票失败");
                        } else {
                            return ResponseVO.success(orderVO);
                        }
                    }

                } else {
                    return ResponseVO.serviceFail("订单中座位编号出错");
                }
            } else {
                return ResponseVO.serviceFail("购票人数过多");
            }
        } catch (Exception e) {
            log.error("购票异常", e);
            return ResponseVO.serviceFail("购票失败");
        }
    }

    @RequestMapping(value = "getOrderInfo", method = RequestMethod.POST)
    public ResponseVO getOrderInfo(
            @RequestParam(name = "nowPage", required = false, defaultValue = "1") Integer nowPage,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize
    ) {
        // 获取当前登录用户的信息
        String userId = CurrentUser.getCurrentUser();
        // 获取当前登录用户已经购买的订单
        Page<OrderVO> page = new Page<>(nowPage, pageSize);
        if (userId != null && userId.trim().length() > 0) {
            Page<OrderVO> orders2018 = orderServiceAPI.getOrdersByUserId(Integer.parseInt(userId), page);
            Page<OrderVO> orders2017 = orderServiceAPI2017.getOrdersByUserId(Integer.parseInt(userId), page);
            // 合并结果
            int totalPage = (int) (orders2017.getPages() + orders2018.getPages());
            // 合并2017和2018年订单的结果
            List<OrderVO> orderVOS = new ArrayList<>();
            orderVOS.addAll(orders2017.getRecords());
            orderVOS.addAll(orders2018.getRecords());
            return ResponseVO.success(nowPage, totalPage, IMG_PRE, orderVOS);
        } else {
            return ResponseVO.serviceFail("用户未登录");
        }
    }

    @RequestMapping(value = "getPayInfo", method = RequestMethod.POST)
    public ResponseVO getPayInfo(@RequestParam(name = "orderId") String orderId) {
        // 获取当前登录人的信息
        String userId = CurrentUser.getCurrentUser();

        if (userId == null || userId.trim().length() == 0) {
            return ResponseVO.serviceFail("抱歉，当前用户未登录");
        }
        // 订单二维码查询
        AlipayInfoVO alipayInfo = alipayServiceAPI.getQRCode(orderId);

        return ResponseVO.success(IMG_PRE, alipayInfo);
    }

    @RequestMapping(value = "getPayResult", method = RequestMethod.POST)
    public ResponseVO getPayResult(@RequestParam(name = "orderId") String orderId,
                                   @RequestParam(name = "tryNums", required = false, defaultValue = "1") Integer tryNums) {
        // 获取当前登录人的信息
        String userId = CurrentUser.getCurrentUser();

        if (userId == null || userId.trim().length() == 0) {
            return ResponseVO.serviceFail("抱歉，当前用户未登录");
        }
        // 将当前登录人的信息传入后端
        RpcContext.getContext().setAttachment("userId", userId);
        // 判断是否支付超时
        if (tryNums >= 4) {
            return ResponseVO.serviceFail("订单支付失败，请稍后重试");
        } else {
            AlipayResultVO alipayResultVO = alipayServiceAPI.getOrderStatus(orderId);
            System.out.println(alipayResultVO.getOrderStatus());
            if (alipayResultVO == null || ToolUtil.isEmpty(alipayResultVO.getOrderId())) {
                AlipayResultVO alipayResult = new AlipayResultVO();
                alipayResult.setOrderId(orderId);
                alipayResult.setOrderStatus(0);
                alipayResult.setOrderMsg("支付不成功!");
                return ResponseVO.success(alipayResultVO);
            }
            return ResponseVO.success(alipayResultVO);
        }
    }
}
