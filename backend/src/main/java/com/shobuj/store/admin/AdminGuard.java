package com.shobuj.store.admin;

import com.shobuj.store.shared.CurrentStaff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security already demands ROLE_ADMIN for /api/v1/admin/**. This also checks, on every request,
 * that the staff account still exists and is active, so removing someone takes effect at once.
 */
@Configuration(proxyBeanMethods = false)
class AdminGuard implements WebMvcConfigurer {

    private final StaffService staff;

    AdminGuard(StaffService staff) {
        this.staff = staff;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                boolean ok = CurrentStaff.id().flatMap(staff::find).isPresent();
                if (!ok) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
                return ok;
            }
        }).addPathPatterns("/api/v1/admin/**");
    }
}
