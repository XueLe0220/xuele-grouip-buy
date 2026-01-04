package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签主表持久化对象
 * <p>
 * 对应数据库表：crowd_tags
 * 记录标签的基础定义（名称、描述）以及当前的统计总量。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 16:59
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTags {

    /** 自增ID */
    private Long id;
    /** 人群ID */
    private String tagId;
    /** 人群名称 */
    private String tagName;
    /** 人群描述 */
    private String tagDesc;
    /** 人群标签统计量 */
    private Integer statistics;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}

