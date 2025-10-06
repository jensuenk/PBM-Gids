package com.ineos.oxide.pbmgids.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve images from a local folder relative to the working directory
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./images/", "classpath:/static/images/")
                .setCachePeriod(3600);

        // Serve downloadable documents and norms
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:./static/", "classpath:/static/")
                .setCachePeriod(3600);
    }
}
