package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Record {
    int year;
    String className;
    Map<Integer, Subject> subject;
    Classification classification;
}