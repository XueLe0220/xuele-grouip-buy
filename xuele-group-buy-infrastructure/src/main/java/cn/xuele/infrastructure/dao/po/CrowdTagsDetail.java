package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签明细表持久化对象
 * <p>
 * 对应数据库表：crowd_tags_detail
 * 记录了“哪个用户被打上了哪个标签”的具体关系数据。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 16:59
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTagsDetail {

    /** 自增ID */
    private Long id;
    /** 人群ID */
    private String tagId;
    /** 用户ID */
    private String userId;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}
