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

        Map<String, Config> fields;

        Boolean include_in_parent;
        Map<String, Config> properties;

        public Config(String type) {
            this(type, null, null, null, null, null, null, null);
        }

        public Config(String type, boolean include_in_all) {
            this(type, null, include_in_all, null, null, null, null, null);
        }

        /**
         * Simple configuration of a non-nested property.
         *
         * @param type           - see ES docs
         * @param format         - see ES docs
         * @param include_in_all - see ES docs
         * @param analyzer       - see ES docs
         * @param index          - see ES docs
         * @param fields         - Map of field name to config, for pieces of a compound type.
         */
        public Config(String type, String format, Boolean include_in_all, String analyzer, IndexOption index, Map<String, Config> fields) {
            this(type, format, include_in_all, analyzer, index, fields, null, null);
        }

        /**
         * A sub-document type of configuration (non-nested).
         *
         * @param include_in_parent - see ES docs
         * @param properties        - Mapping of property name to sub-Config.
         */
        public Config(Boolean include_in_parent, Map<String, Config> properties) {
            this(null, null, null, null, null, null, include_in_parent, properties);
        }

        /**
         * A nested sub-document type of configuration.
         *
         * @param properties - Mapping of property name to sub-Config.
         */
        public Config(Map<String, Config> properties) {
            this("nested", null, null, null, null, null, true, properties);
        }

    }

    public enum IndexOption {no, not_analyzed}

    @Value
    public static class Settings {
        Analysis analysis;
        int number_of_shards;
        int number_of_replicas;
    }

    @Value
    public static class Analysis {
        Analyzer analyzer;
    }

    public interface Analyzer {
    }
}
