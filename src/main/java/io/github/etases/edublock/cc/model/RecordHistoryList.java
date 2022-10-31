package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hyperledger.fabric.contract.annotation.DataType;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class RecordHistoryList {
    List<RecordHistory> histories;
}
