package com.imooc.mall.listener;

import com.google.gson.Gson;
import com.imooc.mall.listenVo.PayInfo;
import com.imooc.mall.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
payInfo正确姿势:pay项目提供client.jar，mall项目引入jar包
 */
@Component
@RabbitListener(queues = "payNotify")
@Slf4j
public class PayMsgListener {

    @Autowired
    private IOrderService orderService;

    private final Gson gson = new Gson();

    @RabbitHandler
    public void process(String msg) {
        log.info("【接收到支付通知】=> {}", msg);
        try {
            PayInfo payInfo = gson.fromJson(msg, PayInfo.class);      // 拆纸条
            if (payInfo == null || payInfo.getOrderNo() == null
                    || payInfo.getPlatformStatus() == null) {         // 残缺纸条 → 丢弃
                log.error("【支付通知不完整】msg={}", msg);
                return;
            }
            if ("SUCCESS".equals(payInfo.getPlatformStatus())) {      // 常量前置防 NPE
                orderService.paid(payInfo.getOrderNo());              // 推进订单状态
                log.info("【订单已付款】orderNo={}", payInfo.getOrderNo());
            } else {
                log.warn("【支付未成功，忽略】status={}", payInfo.getPlatformStatus());
            }
        } catch (Exception e) {
            log.error("【处理支付通知失败】msg={}", msg, e);           // 岔子记日志，不外抛
        }
    }
}
