package com.dclingcloud.kubetopo.config;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class OtherConfig {
    @Bean
    public Mapper beanMapper() {
        return DozerBeanMapperBuilder.buildDefault();
    }
}
