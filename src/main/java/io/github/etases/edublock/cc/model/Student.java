package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    int id;
    Personal personal;
    Map<Integer, Record> record;
}
