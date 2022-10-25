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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Contract(name = "EduBlockChainCode",
        info = @Info(title = "EduBlock contract",
                description = "A contract to store & modify student records",
                version = "0.0.1"
        ))
@Default
public class EduBlockChainCode implements ContractInterface {
    /**
     * Init the ledger
     *
     * @param ctx the transaction context
     * @return the response
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String init(final Context ctx) {
        return "EduBlockChainCode";
    }

    /**
     * Get Student by id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student or null if not found
     */
    private Student getStudentOrNull(final Context ctx, final int studentId) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getPrivateDataUTF8(getCollectionName(ctx), Integer.toString(studentId));

        if (assetJSON == null || assetJSON.isEmpty()) {
            return null;
        }

        return JsonUtil.deserialize(assetJSON, Student.class);
    }

    /**
     * Get Student by id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student or exception if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Student getStudent(final Context ctx, final int studentId) {
        Student student = getStudentOrNull(ctx, studentId);
        if (student == null) {
            String errorMessage = String.format("Student %s does not exist", studentId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        return student;
    }

    /**
     * Get Student's record by Id, grade
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @param classId   the Student Record's class id
     * @return student or null if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Record getRecord(final Context ctx, final int studentId, final int classId) {
        return Optional.ofNullable(getStudent(ctx, studentId))
                .map(Student::getRecord)
                .map(record -> record.get(classId))
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
     * @param ctx the transaction context with the transient map including the "student" object
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student createStudent(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = stub.getTransient();
        if (!transientMap.containsKey("student")) {
            String errorMessage = "The transient map is missing \"student\"";
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }
        String studentJson = new String(transientMap.get("student"), StandardCharsets.UTF_8);
        Student student;
        try {
            student = JsonUtil.deserialize(studentJson, Student.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", studentJson);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        if (isStudentExist(ctx, student.getId())) {
            String errorMessage = String.format("Student %s already exists", student.getId());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.name());
        }
        stub.putPrivateData(getCollectionName(ctx), Integer.toString(student.getId()), studentJson);
        return student;
    }

    /**
     * Update Student's Personal
     *
     * @param ctx       the transaction context with the transient map including the "personal" object
     * @param studentId the Student's id
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student updateStudentPersonal(final Context ctx, int studentId) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = stub.getTransient();
        if (!transientMap.containsKey("personal")) {
            String errorMessage = "The transient map is missing \"personal\"";
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        String personalJson = new String(transientMap.get("personal"), StandardCharsets.UTF_8);
        Personal personal;
        try {
            personal = JsonUtil.deserialize(personalJson, Personal.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", personalJson);
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
     * @param ctx       the transaction context with the transient map including the "record" object
     * @param studentId the Student's id
     * @param classId   the Student Record's class id
     * @return student or exception
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Student updateStudentRecord(final Context ctx, int studentId, int classId) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = stub.getTransient();
        if (!transientMap.containsKey("record")) {
            String errorMessage = "The transient map is missing \"record\"";
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        String recordJson = new String(transientMap.get("record"), StandardCharsets.UTF_8);
        Record record;
        try {
            record = JsonUtil.deserialize(recordJson, Record.class);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", recordJson);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_INVALID.name());
        }

        Student student = getStudent(ctx, studentId);
        if (student == null) {
            String errorMessage = String.format("Student %d does not exist", studentId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }
        if (!student.getRecord().containsKey(classId)) {
            String errorMessage = String.format("Student %d does not have record classId %d", studentId, classId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }

        student.getRecord().put(classId, record);
        return updateStudent(ctx, student);
    }

    private Student updateStudent(final Context ctx, Student student) {
        ChaincodeStub stub = ctx.getStub();
        if (!isStudentExist(ctx, student.getId())) {
            String errorMessage = String.format("Student %d does not exist", student.getId());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.name());
        }
        stub.putPrivateData(getCollectionName(ctx), Integer.toString(student.getId()), JsonUtil.serialize(student));
        return student;
    }

    private String getCollectionName(Context ctx) {
        // TODO: specify collection config
        return "_implicit_org_" + ctx.getClientIdentity().getMSPID();
    }

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        ASSET_INVALID
    }
}
