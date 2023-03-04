package com.asjy.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ClassName: WebConfig
 * Package: com.asjy.condig
 * Description:
 *
 * @Author 梓维李
 * @Create 2023/2/26 16:38
 * @Version 2.0
 */
@Component
public class WebConfig implements HandlerInterceptor {
    /**
     * 	拦截所有请求同时放行login和验证码请求
     * @param request reponse跨域请求处理
     * @param handler 跨域请求处理
     * @return 返回 是否拦截
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        this.setResponseCrossOrigin(request,response);
        return true;

    }

    /**
     * 设定响应对象允许跨域的必要条件
     *
     * @param request 当前请求
     * @param response 当前响应
     * @return 被设定允许跨域的响应
     */
    public HttpServletResponse setResponseCrossOrigin(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "content-type, TOKEN");
        response.setHeader("Access-Control-Expose-Headers", "TOKEN");
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        return response;
    }
}
