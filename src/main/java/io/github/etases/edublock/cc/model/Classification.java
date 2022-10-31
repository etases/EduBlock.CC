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
@EqualsAndHashCode
@DataType
public class Classification {
    @Property
    String firstHalfClassify;
    @Property
    String secondHalfClassify;
    @Property
    String finalClassify;

    public static Classification clone(Classification classification) {
        return classification == null ? new Classification() : new Classification(classification.getFirstHalfClassify(), classification.getSecondHalfClassify(), classification.getFinalClassify());
    }
}
