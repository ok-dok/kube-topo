package com.dclingcloud.kubetopo.beanmapper;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.IngressPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BackendEndpointRelationPOMapper {
    void updatePropertiesIgnoresNull(@MappingTarget BackendEndpointRelationPO target, BackendEndpointRelationPO source);
}
