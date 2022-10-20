package io.github.etases.edublock.cc;

import io.github.etases.edublock.cc.model.Personal;
import io.github.etases.edublock.cc.model.Record;
import io.github.etases.edublock.cc.model.Student;
import io.github.etases.edublock.cc.util.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.Optional;

@Contract(name = "EduBlockChainCode",
        info = @Info(title = "EduBlock contract",
                description = "A contract to store & modify student records",
                version = "0.0.1"
//                license =
//                @License(name = "SPDX-License-Identifier: Apache-2.0",
//                        url = ""),
//                contact =  @Contact(email = "MyAssetContract@example.com",
//                        name = "MyAssetContract",
//                        url = "http://MyAssetContract.me")
        ))
@Default
public class EduBlockChainCode implements ContractInterface {

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        ASSET_INVALID
    }

    /**
     * Get Student by Id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student or null if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Student getStudent(final Context ctx, final int studentId) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(Integer.toString(studentId));

        if (assetJSON == null || assetJSON.isEmpty()) {
            return null;
        }

        return JsonUtil.deserialize(assetJSON, Student.class);
    }

    /**
     * Get Student's record by Id, grade
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @param grade     the Student Record's grade
     * @return student or null if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Record getRecord(final Context ctx, final int studentId, final int grade) {
        return Optional.ofNullable(getStudent(ctx, studentId))
                .map(Student::getRecord)
                .map(record -> record.get(grade))
                .orElse(null);
    }


    /**
     * Check is Student exists by Id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student or null if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean isStudentExist(final Context ctx, final int studentId) {
        return getStudent(ctx, studentId) != null;
    }


    /**
     * Create Student
     *
     * @param ctx        the transaction context
     * @param jsonString the JsonString of Student object
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student createStudent(final Context ctx, String jsonString) {
        ChaincodeStub stub = ctx.getStub();
        Student student;
        try {
            student = JsonUtil.deserialize(jsonString, Student.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", jsonString);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        if (isStudentExist(ctx, student.getId())) {
            String errorMessage = String.format("Student %s already exists", student.getId());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.name());
        }
        stub.putStringState(Integer.toString(student.getId()), jsonString);
        return student;
    }

    /**
     * Update Student's Personal
     *
     * @param ctx        the transaction context
     * @param studentId  the Student's id
     * @param jsonString the JsonString of Student's Personal object
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student updateStudentPersonal(final Context ctx, int studentId, String jsonString) {
        Personal personal;
        try {
            personal = JsonUtil.deserialize(jsonString, Personal.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", jsonString);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        Student student = getStudent(ctx, studentId);
        if (student == null) {
            String errorMessage = String.format("Student %d does not exist", studentId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }

        student.setPersonal(personal);
        return updateStudent(ctx, student);
    }

    /**
     * Update Student's Record
     *
     * @param ctx        the transaction context
     * @param studentId  the Student's id
     * @param grade      the Student Record's grade
     * @param jsonString the JsonString of Student's Record object
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student updateStudentRecord(final Context ctx, int studentId, int grade, String jsonString) {
        Record record;
        try {
            record = JsonUtil.deserialize(jsonString, Record.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", jsonString);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        Student student = getStudent(ctx, studentId);
        if (student == null) {
            String errorMessage = String.format("Student %d does not exist", studentId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }
        if (!student.getRecord().containsKey(grade)) {
            String errorMessage = String.format("Student %d does not have record grade %d", studentId, grade);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }

        student.getRecord().put(grade, record);
        return updateStudent(ctx, student);
    }

    private Student updateStudent(final Context ctx, Student student) {
        ChaincodeStub stub = ctx.getStub();
        if (!isStudentExist(ctx, student.getId())) {
            String errorMessage = String.format("Student %d does not exist", student.getId());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }
        stub.putStringState(Integer.toString(student.getId()), JsonUtil.serialize(student));
        return student;
    }
}
