package com.attensa.rubberband.data.internal;

import lombok.Value;

@Value
public class GetResponse<T> {
    boolean found;
    T _source;
}
