package com.shobuj.store.shared;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded images from the storage folder at {@code /media/**}. */
@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

    private final StoreProperties properties;

    WebConfig(StoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = properties.storageRoot().toAbsolutePath().toUri().toString();
        registry.addResourceHandler(properties.mediaUrl() + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
