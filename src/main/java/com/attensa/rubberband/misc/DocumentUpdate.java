package com.attensa.rubberband.misc;

import lombok.Value;

import java.util.Map;

@Value
public class DocumentUpdate {
    String documentId;
    Map<String, Object> fieldUpdates;
}

