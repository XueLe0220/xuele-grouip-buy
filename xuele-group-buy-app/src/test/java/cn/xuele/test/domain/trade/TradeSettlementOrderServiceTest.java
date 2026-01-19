package cn.xuele.test.domain.trade;

import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementEntity;
import cn.xuele.domain.trade.service.settlement.ITradeSettlementOrderService;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 10:59
 */
@SpringBootTest
@Slf4j
public class TradeSettlementOrderServiceTest {
    @Resource
    private ITradeSettlementOrderService tradeSettlementOrderService;

    @Test
    public void test_settlementMarketPayOrder() throws Exception {
        TradePaySettlementEntity tradePaySettlementEntity = new TradePaySettlementEntity();
        tradePaySettlementEntity.setSource("s01");
        tradePaySettlementEntity.setChannel("c01");
        tradePaySettlementEntity.setUserId("keke");
        tradePaySettlementEntity.setOutTradeNo("571011418085");
        tradePaySettlementEntity.setOutTradeTime(LocalDateTime.now());
        TradeSettlementEntity tradeSettlementEntity = tradeSettlementOrderService.settlement(tradePaySettlementEntity);
        log.info("请求参数:{}", JSON.toJSONString(tradePaySettlementEntity));
        log.info("测试结果:{}", JSON.toJSONString(tradeSettlementEntity));
    }
}
