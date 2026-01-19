package cn.xuele.api.dto;

import java.util.List;

/**
 * 回调请求对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 14:53
 */
public class NotifyRequestDTO {

    /** 组队ID */
    private String teamId;
    /** 外部单号 */
    private List<String> outTradeNoList;

}
