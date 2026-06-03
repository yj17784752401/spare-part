package com.yj.sparepart.common;

import lombok.Data;

/**
 * 统一 API 响应结果封装
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {

    /**
     * 状态码，0 表示成功，非0 表示各种错误
     */
    private int code;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "成功";
        r.data = data;
        return r;
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 包含错误信息的 Result 对象
     */
    public static Result<Void> error(int code, String message) {
        Result<Void> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}