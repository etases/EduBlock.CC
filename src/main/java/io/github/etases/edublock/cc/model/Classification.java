package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Classification {
    String firstHaftClassify;
    String secondHaftClassify;
    String finalClassify;
}
