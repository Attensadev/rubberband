package com.attensa.rubberband.data;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Map;

@Value
public class ElasticSearchMappings {
    //the key to this map is the type that is being mapped.
    Map<String, PropertyContainer> mappings;
    Settings settings;

    @Value
    @AllArgsConstructor
    public static class PropertyContainer {
        //the key to this map is the property of the type that is being mapped.
        Map<String, Config> properties;
        Boolean include_in_all;
        ParentConfig _parent;

        public PropertyContainer(Map<String, Config> properties) {
            this(properties, null, null);
        }
    }

    @Value
    public static class ParentConfig {
        String type;
    }

    @Value
    @AllArgsConstructor
    public static class Config {
        String type;
        //one of the valid formats, eg. "date_optional_time", or "week_date_time", etc.
        String format;
        Boolean include_in_all;
        String analyzer;
        IndexOption index;

        public Config(String type) {
            this(type, null, null, null, null);
        }

        public Config(String type, boolean include_in_all) {
            this(type, null, include_in_all, null, null);
        }

    }

    public enum IndexOption {no, not_analyzed}

    @Value
    public static class Settings {
        Analysis analysis;
    }

    @Value
    public static class Analysis {
        Analyzer analyzer;
    }

    public interface Analyzer {
    }
}
