package com.yj.sparepart.common;

import com.yj.sparepart.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，将业务异常转换为标准 HTTP 响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 BusinessException
     * 根据错误码决定 HTTP 状态码：404 -> NOT_FOUND，其他 -> BAD_REQUEST
     *
     * @param e 业务异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        HttpStatus status = e.getCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理其他未知异常，统一返回 500 内部服务器错误
     *
     * @param e 异常对象
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }
}