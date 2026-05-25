package com.travel.starter.aop;

import com.travel.common.constant.BizException;
import com.travel.common.constant.ServiceResponseTypeEnum;
import com.travel.starter.vo.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 对接口层做切面，避免错误堆栈信息泄露
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 自定义业务异常拦截
     */
    @ExceptionHandler(BizException.class)
    public ServiceResponse<Void> handleBizException(BizException e) {
        log.warn("业务拦截 - 错误码: {}, 错误信息: {}", e.getCode(), e.getMessage());
        return ServiceResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 未被自定义业务异常描述的逃逸异常
     */
    @ExceptionHandler(Exception.class)
    public ServiceResponse<Void> handleUnkonwnException(Exception e) {
        log.error("未知错误拦截");
        e.printStackTrace();
        return ServiceResponse.fail(ServiceResponseTypeEnum.SYSTEM_ERROR.getCode(), ServiceResponseTypeEnum.SYSTEM_ERROR.getMessage());
    }

}
