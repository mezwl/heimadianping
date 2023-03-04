package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenIntercepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ClassName: MvcConfig
 * Package: com.hmdp.config
 * Description:
 *
 * @Author 梓维李
 * @Create 2023/2/26 23:57
 * @Version 2.0
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor ;

    @Autowired
    private RefreshTokenIntercepter refreshTokenIntercepter ;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                ).order(1);
        //todo token拦截器   需要注意 .order() 是设置拦截器的执行顺序
        //todo              order里面的值越小越先执行
        registry.addInterceptor(refreshTokenIntercepter).addPathPatterns("/**").order(0);
    }
}
