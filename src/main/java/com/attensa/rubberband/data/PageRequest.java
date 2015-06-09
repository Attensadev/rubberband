package com.attensa.rubberband.data;

import lombok.Value;

@Value
public class PageRequest {
    int size;
    int pageNumber;
}
