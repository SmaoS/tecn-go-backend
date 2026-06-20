package com.tecngo.users.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UserOperationGuardWebConfig implements WebMvcConfigurer {
    private final UserOperationGuardInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/auth/login", "/v1/auth/register", "/v1/auth/verify-email",
                        "/v1/auth/phone/**", "/v1/auth/register-by-phone", "/v1/auth/login-by-phone",
                        "/v1/auth/forgot-password", "/v1/auth/reset-password",
                        "/v1/legal/documents/public", "/v1/services/**", "/v1/service-categories/**",
                        "/v1/catalogs/**", "/v1/referrals/validate/**", "/v1/app-version/check");
    }
}
