package com.attensa.rubberband.data;

import lombok.Value;

@Value
public class ScoredItem<T> {
    T item;
    float score;
}
