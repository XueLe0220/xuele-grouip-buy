package cn.xuele.test.trigger;

import cn.xuele.api.IDCCService;
import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.IIndexGroupBuyMarketService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 动态配置中心 (DCC) 功能集成测试
 * <p>
 * 目的：验证动态降级开关 (Downgrade Switch) 是否生效，以及对业务流程的影响。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/05 15:51
 */
@Slf4j
@SpringBootTest
public class DCCControllerTest {

    @Resource
    private IDCCService dccService;

    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;

    /**
     * 测试用例：仅更新配置值
     */
    @Test
    public void test_updateConfig() {
        // 动态调整配置：开启降级 (1)
        dccService.updateConfig("downgradeSwitch", "1");
        log.info("配置已更新: downgradeSwitch -> 1");
    }

    /**
     * 测试用例：更新配置并验证对营销试算流程的影响
     */
    @Test
    public void test_updateConfig2indexMarketTrial() throws Exception {
        // 1. 动态调整配置：关闭降级 (0)，预期走正常业务逻辑
        dccService.updateConfig("downgradeSwitch", "0");

        // 2. 线程休眠 1s
        // 原因：模拟配置传播延时，确保 value 变更已在内存中生效
        Thread.sleep(1000);

        // 3. 构造营销试算请求参数
        MarketProductEntity marketProductEntity = new MarketProductEntity();
        marketProductEntity.setUserId("xuele");
        marketProductEntity.setSource("s01");
        marketProductEntity.setChannel("c01");
        marketProductEntity.setGoodsId("9890001");

        // 4. 执行业务逻辑验证
        TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(marketProductEntity);

        log.info("请求参数:{}", JSON.toJSONString(marketProductEntity));
        log.info("返回结果:{}", JSON.toJSONString(trialBalanceEntity));
    }
}