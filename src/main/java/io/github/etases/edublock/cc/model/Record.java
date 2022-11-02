package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DataType
public class Record {
    @Property
    Map<Long, ClassRecord> classRecords; // key : record id (class id)

    public static Record clone(Record record) {
        if (record == null) {
            return new Record(new HashMap<>());
        }
        var cloneClassRecords = new HashMap<Long, ClassRecord>();
        if (record.getClassRecords() != null) {
            for (var entry : record.getClassRecords().entrySet()) {
                cloneClassRecords.put(entry.getKey(), ClassRecord.clone(entry.getValue()));
            }
        }
        return new Record(cloneClassRecords);
    }
}
