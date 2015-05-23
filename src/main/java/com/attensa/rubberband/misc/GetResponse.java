package com.attensa.rubberband.misc;

import lombok.Value;

@Value
public class GetResponse<T> {
    boolean found;
    T _source;
}
