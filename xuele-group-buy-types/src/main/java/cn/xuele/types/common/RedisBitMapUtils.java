package cn.xuele.types.common;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * Redis 位图工具类
 * <p>
 * 提供统一的位图 Key 生成策略与 UserId 偏移量计算算法。
 * 使用 MurmurHash3 算法保证高随机性与低碰撞率。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 12:20
 */
public class RedisBitMapUtils {

    /**
     * Redis Key 前缀
     */
    public static final String TAG_PREFIX = "crowd:tag:bitmap:";

    /**
     * 位图容量上限：1亿
     * <p>
     * 内存估算：100,000,000 bits ≈ 12 MB。
     * 这意味着每个标签的位图最大占用 12MB 内存，可容纳 1亿 个离散用户 ID 映射。
     */
    private static final int BITMAP_MAX_SIZE = 100_000_000;

    /**
     * [核心算法] 将 String 类型的 userId 映射为 Bitmap 的 offset
     * <p>
     * 算法原理：
     * 1. 使用 Google Guava 的 MurmurHash3_32 算法进行哈希（高性能、低碰撞）。
     * 2. 使用 (hash & 0x7FFFFFFF) 强转正整数。
     * 3. 对 BITMAP_MAX_SIZE 取模，限制内存占用。
     *
     * @param userId 用户ID字符串
     * @return 映射后的正整数索引 (范围: 0 ~ 99,999,999)
     */
    public static long getIndexFromUserId(String userId) {
        // 使用 MurmurHash3 算法生成 32位 哈希值
        int hash32 = Hashing.murmur3_32_fixed()
                .hashString(userId, StandardCharsets.UTF_8)
                .asInt();

        // 强转为非负数 (利用位运算清空最高位的符号位)
        int index = hash32 & 0x7FFFFFFF;

        // 取模限制范围，防止 Offset 过大导致 Redis 内存分配瞬间爆炸
        return index % BITMAP_MAX_SIZE;
    }

    /**
     * 生成 Redis Key
     *
     * @param tagId 标签/人群包 ID
     * @return 完整的 Redis Key
     */
    public static String getTagBitMapKey(String tagId) {
        return TAG_PREFIX + tagId;
    }

}