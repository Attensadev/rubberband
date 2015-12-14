package com.attensa.rubberband.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

import java.lang.reflect.Type;

@Value
@Wither
public class ScrollContext<T> {
    String scrollId;
    String keepAliveTime;
    Type documentType;
    @Getter(AccessLevel.NONE)
    boolean hasMore;
    long total;

    public boolean hasMore() {
        return hasMore;
    }
}
