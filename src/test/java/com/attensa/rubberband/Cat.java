package com.attensa.rubberband;

import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class Cat {
    String id;
    String name;
    String gender;
    String breed;
    String description;
}
