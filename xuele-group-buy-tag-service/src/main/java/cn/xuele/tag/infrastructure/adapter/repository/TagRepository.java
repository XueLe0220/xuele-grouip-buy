package cn.xuele.tag.infrastructure.adapter.repository;

import cn.xuele.tag.domain.tag.adapter.repository.ITagRepository;
import org.springframework.stereotype.Repository;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:39
 */
@Repository
public class TagRepository implements ITagRepository {


    @Override
    public boolean isUserMatchedTag(String userId, String tagId) {
        return "xuele".equals(userId) && "10001".equals(tagId);
    }


}
