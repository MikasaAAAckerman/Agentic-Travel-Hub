package com.travel.starter.vo;

import com.travel.common.constant.ServiceResponseTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 封装响应体请求，{code:200 or 500 or 其他, message:{xxx}}，便于前端统一处理
 */
@Data
public class ServiceResponse<T> implements Serializable {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应描述信息
     */
    private String message;

    /**
     * 响应体
     */
    private T data;

    public static <T> ServiceResponse<T> success() {
        ServiceResponse<T> serviceResponse = new ServiceResponse<>();
        serviceResponse.setCode(ServiceResponseTypeEnum.SUCCESS.getCode());
        serviceResponse.setMessage(ServiceResponseTypeEnum.SUCCESS.getMessage());
        return serviceResponse;
    }

    public static <T> ServiceResponse<T> success(T data) {
        ServiceResponse<T> serviceResponse = success();
        serviceResponse.setData(data);
        return serviceResponse;
    }

    public static <T> ServiceResponse<T> fail(Integer code, String message) {
        ServiceResponse<T> serviceResponse = new ServiceResponse<>();
        serviceResponse.setCode(code);
        serviceResponse.setMessage(message);
        return null;
    }
}
