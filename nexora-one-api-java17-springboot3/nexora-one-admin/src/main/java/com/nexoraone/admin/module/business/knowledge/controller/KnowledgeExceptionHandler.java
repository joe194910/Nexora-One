package com.nexoraone.admin.module.business.knowledge.controller;

import com.nexoraone.base.common.domain.ResponseDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 仅转换知识库模块可预期的用户操作和依赖配置错误。 */
@RestControllerAdvice(assignableTypes = KnowledgeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KnowledgeExceptionHandler {

    /** 将所有权、状态和配置校验错误按用户可读的参数错误返回。 */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseDTO<String> invalidOperation(RuntimeException exception) {
        return ResponseDTO.userErrorParam(exception.getMessage());
    }
}
