package cn.xuele.test.domain.trade;

import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;
import cn.xuele.domain.trade.service.ITradeRefundOrderService;
import cn.xuele.trigger.listener.RefundSuccessTopicListener;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.concurrent.CountDownLatch;

/**
 * 逆向流程单测
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 16:56
 */
@Slf4j
@SpringBootTest
@Import({RefundSuccessTopicListener.class})
public class TradeRefundOrderServiceTest {

    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;

    @Test
    public void test_refundOrder() throws InterruptedException {
        TradeRefundCommandEntity tradeRefundCommandEntity = TradeRefundCommandEntity.builder()
                .userId("bangzhi")
                .outTradeNo("971697405275")
                .source("s01")
                .channel("c01")
                .build();

        TradeRefundBehaviorEntity tradeRefundBehaviorEntity = tradeRefundOrderService.refund(tradeRefundCommandEntity);

        log.info("请求参数:{}", JSON.toJSONString(tradeRefundCommandEntity));
        log.info("测试结果:{}", JSON.toJSONString(tradeRefundBehaviorEntity));

        // 暂停，等待MQ消息。处理完后，手动关闭程序
        new CountDownLatch(1).await();
    }

}
