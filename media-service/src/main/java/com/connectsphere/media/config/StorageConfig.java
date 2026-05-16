package com.connectsphere.media.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Spring MVC to serve uploaded files from the local
 * upload directory via HTTP.
 *
 * Files uploaded to ./uploads are served at:
 *   http://localhost:8087/files/**
 *
 * This is only needed for the LocalStorageServiceImpl.
 * In production with S3, files are served via CloudFront CDN
 * and this handler is not used.
 */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./uploads/");
    }
}