package org.example.config;

import org.example.interceptor.HeaderInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器 + 跨域配置
 *
 * @author ruoyi
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 不需要拦截的路径 */
    public static final String[] excludeUrls = {"/login", "/logout", "/refresh", "/captchaImage"};

    /** 原有：注册自定义请求头拦截器（保持不动） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getHeaderInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(excludeUrls)
                .order(-10);
    }

    /** 原有：返回自定义拦截器实例（必须保留！） */
    public HeaderInterceptor getHeaderInterceptor() {
        return new HeaderInterceptor();
    }

    /** 新增：全局跨域配置（重点！） */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                    // 所有接口
                .allowedOriginPatterns("*")          // 允许任意来源（支持 localhost:3000 等）
                .allowedMethods("*")                 // GET POST PUT DELETE OPTIONS 全放
                .allowedHeaders("*")                 // 允许任意请求头
                .exposedHeaders("Authorization")     // 暴露 token 给前端
                .allowCredentials(true)              // 允许携带 cookie（登录态必须）
                .maxAge(3600);                       // 预检缓存 1 小时
    }
}