package cn.xuele.tag.api;

import cn.xuele.tag.api.dto.TagQueryRequestDTO;
import cn.xuele.tag.api.response.TagQueryResponse;

/**
 * 人群标签查询服务
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:34
 */
public interface ITagQueryService {

    TagQueryResponse isUserMatchedTag(TagQueryRequestDTO request);

}
