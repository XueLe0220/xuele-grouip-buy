package cn.xuele.tag.domain.tag.service;

import cn.xuele.tag.domain.tag.adapter.repository.ITagRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:38
 */
@Service
@RequiredArgsConstructor
public class TagService implements ITagService {

    private final ITagRepository tagRepository;

    @Override
    public boolean isUserMatchedTag(String userId, String tagId) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(tagId)) {
            return false;
        }
        return tagRepository.isUserMatchedTag(userId, tagId);
    }

}
