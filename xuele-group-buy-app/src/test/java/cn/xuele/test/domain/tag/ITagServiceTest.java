package cn.xuele.test.domain.tag;

import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.tag.adapter.repository.ITagRepository;
import cn.xuele.domain.tag.service.ITagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 人群标签领域服务集成测试
 * <p>
 * 验证 TagService 的全链路逻辑：
 * 1. 从 DB 读取任务配置。
 * 2. 模拟/查询统计数据。
 * 3. 将结果写回 crowd_tags_detail 表。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/03 14:37
 */
@Slf4j
@SpringBootTest
public class ITagServiceTest {

    @Resource
    private ITagService tagService;

    @Resource
    private IActivityRepository repository;

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void test_tag_job() {
        tagService.execTagBatchJob("RQ_KJHKL98UU78H66554GFDV", "10001");
    }

    @Test
    public void test_tag_bitmap() {
        String tagId = "RQ_KJHKL98UU78H66554GFDV";
        // 是否存在
        log.info("xuele 存在，预期结果为 true，测试结果:{}", repository.isUserInTag("xuele"));
        log.info("xiaofuge 不存在，预期结果为 false，测试结果:{}", repository.isUserInTag("xiaofuge"));
    }

    @Test
    public void test_null_tag_bitmap() {
        RBitSet bitSet = redissonClient.getBitSet("null");
        log.info("测试结果:{}", bitSet.isExists());
    }

}
