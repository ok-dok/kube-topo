package com.dclingcloud.kubetopo.beanmapper;

import com.dclingcloud.kubetopo.entity.IngressPO;
import com.dclingcloud.kubetopo.entity.PodPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IngressPOMapper {
    void updatePropertiesIgnoresNull(@MappingTarget IngressPO target, IngressPO source);
}
