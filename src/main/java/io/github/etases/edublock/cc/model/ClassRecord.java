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
public class ClassRecord {
    @Property
    int year;
    @Property
    int grade;
    @Property
    String className;
    @Property
    Map<Long, Subject> subjects; // key : subject id
    @Property
    Classification classification;

    public static ClassRecord clone(ClassRecord classRecord) {
        if (classRecord == null) {
            var clone = new ClassRecord();
            clone.setSubjects(new HashMap<>());
            clone.setClassification(new Classification());
            return clone;
        }
        Classification cloneClassification = Classification.clone(classRecord.getClassification());
        var cloneSubjects = new HashMap<Long, Subject>();
        if (classRecord.getSubjects() != null) {
            for (var entry : classRecord.getSubjects().entrySet()) {
                cloneSubjects.put(entry.getKey(), Subject.clone(entry.getValue()));
            }
        }
        return new ClassRecord(classRecord.getYear(), classRecord.getGrade(), classRecord.getClassName(), cloneSubjects, cloneClassification);
    }
}