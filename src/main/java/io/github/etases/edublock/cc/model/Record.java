package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class Record {
    @Property
    int year;
    @Property
    int grade;
    @Property
    String className;
    @Property
    Map<Integer, Subject> subject; // key : subject id
    @Property
    Classification classification;
}