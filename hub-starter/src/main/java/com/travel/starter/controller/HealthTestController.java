package com.travel.starter.controller;

import com.travel.starter.vo.ServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 
 * @author Agentic Travel Hub Team
 */
@Slf4j
@Tag(name = "健康检查", description = "系统健康状态检测接口")
@RestController
@RequestMapping("/api/v1/health-test")
public class HealthTestController {

    /**
     * 健康检查接口
     */
    @Operation(
        summary = "服务健康检查",
        description = "用于检测服务是否正常运行，返回 success 表示服务正常"
    )
    @GetMapping("/heartbeat")
    public ServiceResponse<String> heartbeat(){
        log.info(" /heartbeat接受请求 ");
        return ServiceResponse.success("响应成功");
    }

}
