package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class Subject {
    @Property
    String name;
    @Property
    float firstHaftScore;
    @Property
    float secondHaftScore;
    @Property
    float finalScore;
}
