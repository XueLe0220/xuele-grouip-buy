package cn.xuele.tag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人群标签查询请求 DTO
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:40
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagQueryRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String userId;
    private String tagId;

}
