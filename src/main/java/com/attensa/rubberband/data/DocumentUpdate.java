package com.attensa.rubberband.data;

import lombok.Value;

import java.util.Map;

@Value
public class DocumentUpdate {
    String documentId;
    Map<String, Object> fieldUpdates;
}

