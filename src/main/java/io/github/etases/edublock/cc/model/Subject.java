package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    String name;
    float firstHaftScore;
    float secondHaftScore;
    float finalScore;
}
