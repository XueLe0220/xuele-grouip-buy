package cn.xuele.tag.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人群标签查询响应
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:41
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagQueryResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String userId;
    private String tagId;
    private boolean matched;
    private String tagName;

}
