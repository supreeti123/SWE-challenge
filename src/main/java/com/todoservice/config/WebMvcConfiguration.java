package com.todoservice.config;

import com.todoservice.web.WriteRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration for cross-cutting web behavior.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final WriteRateLimitInterceptor writeRateLimitInterceptor;

    public WebMvcConfiguration(WriteRateLimitInterceptor writeRateLimitInterceptor) {
        this.writeRateLimitInterceptor = writeRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(writeRateLimitInterceptor).addPathPatterns("/api/todos/**");
    }
}
