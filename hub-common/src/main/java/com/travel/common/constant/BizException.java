package com.travel.common.constant;

import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ServiceResponseTypeEnum typeEnum) {
        super(typeEnum.getMessage());
        this.code = typeEnum.getCode();
    }

}
