package com.example.doktoribackend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MdcFilterConfig {

    @Bean
    public FilterRegistrationBean<MdcTraceFilter> mdcTraceFilter() {
        FilterRegistrationBean<MdcTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcTraceFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}