package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class NotFilter implements QueryType {
    QueryType not;
}
