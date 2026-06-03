package com.yj.sparepart.exception;

import lombok.Getter;

/**
 * 业务异常类，用于在服务层抛出可识别的业务错误
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码，对应 HTTP 状态码或自定义错误码
     */
    private final int code;

    /**
     * 构造业务异常
     *
     * @param code    错误码
     * @param message 错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}