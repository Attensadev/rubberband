package com.attensa.rubberband.data;

import lombok.Value;

import java.util.List;

@Value
public class Page<T> {
    List<T> contents;
    PageRequest request;
    long total;
}
