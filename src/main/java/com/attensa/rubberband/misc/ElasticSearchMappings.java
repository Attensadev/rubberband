package com.attensa.rubberband.misc;

import lombok.AllArgsConstructor;
import lombok.Value;

public interface ElasticSearchMappings {

    @Value
    @AllArgsConstructor
    class PropertyContainer<T> {
        T properties;
        Boolean include_in_all;
        ParentConfig _parent;

        public PropertyContainer(T properties) {
            this(properties, null, null);
        }
    }

    @Value
    class ParentConfig {
        String type;
    }

    @Value
    @AllArgsConstructor
    class Config {
        String type;
        String format;
        Boolean include_in_all;
        String analyzer;
        String index;

        public Config(String type) {
            this(type, null, null, null, null);
        }

        public Config(String type, boolean include_in_all) {
            this(type, null, include_in_all, null, null);
        }
    }
}
