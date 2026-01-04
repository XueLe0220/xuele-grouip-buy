package cn.xuele.domain.tag.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签任务实体对象
 * <p>
 * 也就是我们在业务中定义的 “一次具体的打标任务”。
 * 包含了任务的执行规则、针对的统计时间范围（业务时间）以及任务类型。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/02 21:55
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTagsJobEntity {
    /** 标签类型（参与量、消费金额等） */
    private Integer tagType;
    /** 标签规则（如：限定类型 N次） */
    private String tagRule;
    /** 统计数据开始时间（业务时间窗口起始） */
    private Date statStartTime;
    /** 统计数据结束时间（业务时间窗口结束） */
    private Date statEndTime;
}