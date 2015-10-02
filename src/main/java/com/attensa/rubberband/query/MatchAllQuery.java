package com.attensa.rubberband.query;

import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
public class MatchAllQuery implements QueryType {
    Map<Object, Object> match_all = Collections.emptyMap();
}
