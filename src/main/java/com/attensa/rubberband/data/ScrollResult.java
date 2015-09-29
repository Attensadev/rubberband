package com.attensa.rubberband.data;

import lombok.Value;

import java.util.List;

@Value
public class ScrollResult<T> {
    ScrollContext<T> scrollContext;
    List<T> data;
}
