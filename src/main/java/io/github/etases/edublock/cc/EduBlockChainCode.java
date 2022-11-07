package io.github.etases.edublock.cc;

import io.github.etases.edublock.cc.model.Record;
import io.github.etases.edublock.cc.model.*;
import io.github.etases.edublock.cc.util.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * Get student personal by id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student personal or exception if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Personal getStudentPersonal(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        String personalState = stub.getPrivateDataUTF8(getCollectionName(ctx), Long.toString(studentId));
        if (personalState == null || personalState.isEmpty()) {
            String errorMessage = String.format("Personal %d does not exist", studentId);
            System.out.println(errorMessage);
            throw newChainException(AssetErrors.ASSET_NOT_FOUND, errorMessage);
        }
        return JsonUtil.deserialize(personalState, Personal.class);
    }

    /**
     * Update student personal
     *
     * @param ctx       the transaction context, which includes the student personal in the transient map
     * @param studentId the student id
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateStudentPersonal(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        Personal personal = getValueFromTransientMap(ctx, "personal", Personal.class);
        String personalState = JsonUtil.serialize(personal);
        stub.putPrivateData(getCollectionName(ctx), Long.toString(studentId), personalState.getBytes(StandardCharsets.UTF_8));
    }

    private Record getStudentRecordOrNull(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        String recordState = stub.getStringState(composePublicKey(ctx, Long.toString(studentId)));
        if (recordState == null || recordState.isEmpty()) {
            return null;
        }
        return JsonUtil.deserialize(recordState, Record.class);
    }

    /**
     * Get student record by id
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return student record or exception if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Record getStudentRecord(final Context ctx, final long studentId) {
        Record record = getStudentRecordOrNull(ctx, studentId);
        if (record == null) {
            String errorMessage = String.format("Record %d does not exist", studentId);
            System.out.println(errorMessage);
            throw newChainException(AssetErrors.ASSET_NOT_FOUND, errorMessage);
        }
        return record;
    }

    /**
     * Update student record
     *
     * @param ctx       the transaction context, which includes the student record in the transient map
     * @param studentId the student id
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateStudentRecord(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        Record record = getValueFromTransientMap(ctx, "record", Record.class);
        String recordState = JsonUtil.serialize(record);
        stub.putStringState(composePublicKey(ctx, Long.toString(studentId)), recordState);
    }

    /**
     * Update student class record
     *
     * @param ctx       the transaction context, which includes the student class record in the transient map
     * @param studentId the student id
     * @param classId   the class id
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateStudentClassRecord(final Context ctx, final long studentId, final long classId) {
        ChaincodeStub stub = ctx.getStub();
        ClassRecord record = getValueFromTransientMap(ctx, "classRecord", ClassRecord.class);
        Record studentRecord = getStudentRecordOrNull(ctx, studentId);
        Record newRecord = Record.clone(studentRecord);
        newRecord.getClassRecords().put(classId, record);
        String recordState = JsonUtil.serialize(newRecord);
        stub.putStringState(composePublicKey(ctx, Long.toString(studentId)), recordState);
    }

    /**
     * Get the history of student record
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return the history of student record
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public RecordHistoryList getStudentRecordHistory(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyModification> iterator = stub.getHistoryForKey(composePublicKey(ctx, Long.toString(studentId)));
        List<RecordHistory> histories = new ArrayList<>();
        for (KeyModification keyModification : iterator) {
            RecordHistory history = new RecordHistory();
            history.setTimestamp(Date.from(keyModification.getTimestamp()));
            history.setRecord(JsonUtil.deserialize(keyModification.getStringValue(), Record.class));
            history.setUpdatedBy(keyModification.getTxId());
            histories.add(history);
        }
        return new RecordHistoryList(histories);
    }

    ChaincodeException newChainException(AssetErrors error, String message) {
        return new ChaincodeException(error.name() + ": " + message, error.name());
    }

    String getCollectionName(Context ctx) {
        // TODO: specify collection config
        return "_implicit_org_" + ctx.getClientIdentity().getMSPID();
    }

    String composePublicKey(Context ctx, String key) {
        return ctx.getClientIdentity().getMSPID() + "_" + key;
    }

    <T> T getValueFromTransientMap(final Context ctx, String transientKey, Class<T> clazz) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = stub.getTransient();
        if (!transientMap.containsKey(transientKey)) {
            String errorMessage = "The transient map is missing \"" + transientKey + "\"";
            System.out.println(errorMessage);
            throw newChainException(AssetErrors.ASSET_NOT_FOUND, errorMessage);
        }

        String json = new String(transientMap.get(transientKey), StandardCharsets.UTF_8);
        T t;
        try {
            t = JsonUtil.deserialize(json, clazz);
        } catch (Exception exception) {
            String errorMessage = String.format("Invalid input: %s", json);
            System.out.println(errorMessage);
            throw newChainException(AssetErrors.ASSET_INVALID, errorMessage);
        }
        return t;
    }

    enum AssetErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        ASSET_INVALID
    }
}
