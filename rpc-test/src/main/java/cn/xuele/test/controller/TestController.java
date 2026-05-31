package cn.xuele.test.controller;

import cn.xuele.tag.api.ITagQueryService;
import cn.xuele.tag.api.dto.TagQueryRequestDTO;
import cn.xuele.tag.api.response.TagQueryResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 19:04
 */
@RestController
public class TestController {

    @DubboReference(version = "1.0.0")
    private ITagQueryService tagQueryService;

    @GetMapping("/debug/tag/match")
    public void testIsUserMatchedTag(String userId, String tagId) {
        TagQueryRequestDTO requestDTO = TagQueryRequestDTO.builder().userId(userId).tagId(tagId).build();
        TagQueryResponse userMatchedTag = tagQueryService.isUserMatchedTag(requestDTO);

        if (userMatchedTag.isMatched()) {
            System.out.println("用户命中标签");
        } else {
            System.out.println("用户未命中");
        }
    }
}
