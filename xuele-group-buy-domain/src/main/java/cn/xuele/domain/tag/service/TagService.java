package cn.xuele.domain.tag.service;

import cn.xuele.domain.tag.adapter.repository.ITagRepository;
import cn.xuele.domain.tag.model.entity.CrowdTagsJobEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 人群标签领域服务实现
 * <p>
 * 负责编排人群标签的计算、存储与统计更新流程。
 * 核心遵循：查任务 -> 算人群 -> 落库 -> 更新统计 的 DDD 编排逻辑。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 17:05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TagService implements ITagService {

    private final ITagRepository repository;

    @Override
    @Transactional
    public void execTagBatchJob(String tagId, String batchId) {
        log.info("【Tag领域】开始执行人群标签批次任务，TagId：{}，BatchId：{}", tagId, batchId);

        // 1. 查询批次任务 (Defensive Check)
        CrowdTagsJobEntity jobEntity = repository.queryCrowdTagsJobEntity(tagId, batchId);
        if (null == jobEntity) {
            log.error("【Tag领域】批次任务不存在或状态异常，停止执行。TagId：{}，BatchId：{}", tagId, batchId);
            return;
        }

        // 2. 采集用户数据 (核心业务逻辑)
        // TODO: 这里未来将接入规则引擎或策略模式 or AI。
        // 目前根据 jobEntity.getTagType() 和 getTagRule() 决定去查订单库还是行为库。
        // 现在暂时使用模拟数据。
        List<String> userIdList = new ArrayList<String>() {{
            add("xuele");
            add("keke");
            add("bangzhi");
        }};

        // 3. 数据写入记录 (批量处理)
        if (!userIdList.isEmpty()) {
            repository.addCrowdTagsUsers(tagId, userIdList);
        }

        // 4. 更新统计量 (反范式设计，提升查询性能)
        repository.updateCrowdTagStatistics(tagId, userIdList.size());

        log.info("【Tag领域】人群标签批次任务执行完成，共圈选用户 {} 人", userIdList.size());
    }

}