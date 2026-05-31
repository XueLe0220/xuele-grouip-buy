package cn.xuele.tag.domain.tag.adapter.repository;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:37
 */
public interface ITagRepository {

    /**
     * 判断用户是否命中标签
     * @param userId    用户id
     * @param tagId     标签id
     * @return  是否命中
     */
    boolean isUserMatchedTag(String userId, String tagId);

}
