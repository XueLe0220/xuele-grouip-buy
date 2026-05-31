package cn.xuele.tag.trigger.rpc;

import cn.xuele.tag.api.ITagQueryService;
import cn.xuele.tag.api.dto.TagQueryRequestDTO;
import cn.xuele.tag.api.response.TagQueryResponse;
import cn.xuele.tag.domain.tag.service.ITagService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 人群标签查询 Dubbo 服务
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:36
 */
@DubboService(version = "1.0.0", timeout = 3000)
@RequiredArgsConstructor
public class TagQueryServiceRpc implements ITagQueryService {

    private final ITagService tagService;

    @Override
    public TagQueryResponse isUserMatchedTag(TagQueryRequestDTO request) {
        String tagId = request.getTagId();
        String userId = request.getUserId();
        boolean matched = tagService.isUserMatchedTag(userId, tagId);

        return TagQueryResponse.builder()
                .tagId(tagId)
                .userId(userId)
                .matched(matched)
                .build();
    }

}
