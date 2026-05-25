package com.travel.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ServiceResponseTypeEnum {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "Token失效"),
    FORBIDDEN(403, "禁止访问"),
    SYSTEM_ERROR(500, "系统错误"),
    AI_CALL_FAILED(5001, "大模型 API 调用失败"),

    ;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;
}
