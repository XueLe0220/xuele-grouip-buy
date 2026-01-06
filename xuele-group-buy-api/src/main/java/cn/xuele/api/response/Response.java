package cn.xuele.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用 API 响应包装类
 * <p>
 * 作用：
 * 统一所有接口的返回结构。前端或上游服务通过检查 code 来判断业务是否成功。
 * 泛型 <T> 保证了 data 字段可以灵活承载任意类型的业务数据（DTO/VO）。
 *
 * @param <T> 具体的业务数据类型 (如: String, UserDTO, OrderVO)
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/05 15:17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    // 序列化 ID：用于 RPC 传输或 Redis 缓存时的版本兼容校验
    private static final long serialVersionUID = 7000723935764546321L;

    /**
     * 状态码 (Response Code)
     */
    private String code;

    /**
     * 描述信息 (Response Message)
     */
    private String info;

    /**
     * 业务数据 (Payload)
     */
    private T data;

}