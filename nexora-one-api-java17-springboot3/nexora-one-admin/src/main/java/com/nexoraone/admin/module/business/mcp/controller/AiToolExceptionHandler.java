package com.nexoraone.admin.module.business.mcp.controller;

import com.nexoraone.base.common.domain.ResponseDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将工具状态、归属、Schema 和 HTTP 安全校验错误转换为用户可读响应。 */
@RestControllerAdvice(assignableTypes = {
        AiToolManageController.class, AiToolOpenController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiToolExceptionHandler {

    /** 将可预期的业务校验异常返回为参数错误，避免暴露服务端堆栈。 */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseDTO<String> invalidOperation(RuntimeException exception) {
        return ResponseDTO.userErrorParam(exception.getMessage());
    }
}
