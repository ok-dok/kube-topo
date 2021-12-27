package com.dclingcloud.kubetopo.util;

import io.kubernetes.client.custom.IntOrString;

import javax.persistence.AttributeConverter;

public class IntOrStringConverter implements AttributeConverter<IntOrString, String> {
    @Override
    public String convertToDatabaseColumn(IntOrString attribute) {
        return attribute.toString();
    }

    @Override
    public IntOrString convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        else try {
            Integer value = Integer.valueOf(dbData);
            return new IntOrString(value);
        } catch (NumberFormatException e) {
            return new IntOrString(dbData);
        }
    }
}
