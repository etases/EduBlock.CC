package io.github.etases.edublock.cc.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Date;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DataType
public class Personal {
    @Property
    String firstName;
    @Property
    String lastName;
    @Property
    boolean male;
    @Property
    String avatar;
    @Property
    Date birthDate;
    @Property
    String address;
    @Property
    String ethnic;
    @Property
    String fatherName;
    @Property
    String fatherJob;
    @Property
    String motherName;
    @Property
    String motherJob;
    @Property
    String guardianName;
    @Property
    String guardianJob;
    @Property
    String homeTown;
}
