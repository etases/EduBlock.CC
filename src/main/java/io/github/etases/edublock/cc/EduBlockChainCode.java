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
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Contract(name = "EduBlockChainCode",
        info = @Info(title = "EduBlock contract",
                description = "A contract to store & modify student records",
                version = "0.0.1"
        ))
@Default
public class EduBlockChainCode implements ContractInterface {
    private static final String RECORD_PREFIX = "record";

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
     * Get all student personals
     *
     * @param ctx the transaction context
     * @return all student personals as a serialized {@link PersonalMap}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllStudentPersonals(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyValue> personalState = stub.getPrivateDataByRange(getCollectionName(ctx), "", "");
        Map<Long, Personal> personals = new HashMap<>();
        for (KeyValue kv : personalState) {
            long studentId = Long.parseLong(kv.getKey());
            Personal personal = JsonUtil.deserialize(kv.getStringValue(), Personal.class);
            personals.put(studentId, personal);
        }
        return JsonUtil.serialize(new PersonalMap(personals));
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
        String recordState = stub.getStringState(composePublicKey(ctx, RECORD_PREFIX, Long.toString(studentId)).toString());
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
     * @return student {@link Record} as serialized string or exception if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getStudentRecord(final Context ctx, final long studentId) {
        Record record = getStudentRecordOrNull(ctx, studentId);
        if (record == null) {
            String errorMessage = String.format("Record %d does not exist", studentId);
            System.out.println(errorMessage);
            throw newChainException(AssetErrors.ASSET_NOT_FOUND, errorMessage);
        }
        return JsonUtil.serialize(record);
    }

    /**
     * Get all student records
     *
     * @param ctx the transaction context
     * @return all student records as a serialized {@link RecordMap}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllStudentRecords(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyValue> recordState = stub.getStateByPartialCompositeKey(composePrefixKey(ctx, RECORD_PREFIX));
        Map<Long, Record> records = new HashMap<>();
        for (KeyValue kv : recordState) {
            List<String> attributes = verifyAndGetAttributes(ctx, kv.getKey(), RECORD_PREFIX);
            if (attributes.size() != 1) {
                throw newChainException(AssetErrors.ASSET_NOT_FOUND, "Invalid key");
            }
            long studentId = Long.parseLong(attributes.get(0));
            Record record = JsonUtil.deserialize(kv.getStringValue(), Record.class);
            records.put(studentId, record);
        }
        return JsonUtil.serialize(new RecordMap(records));
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
        stub.putStringState(composePublicKey(ctx, RECORD_PREFIX, Long.toString(studentId)).toString(), recordState);
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
        stub.putStringState(composePublicKey(ctx, RECORD_PREFIX, Long.toString(studentId)).toString(), recordState);
    }

    /**
     * Get the history of student record
     *
     * @param ctx       the transaction context
     * @param studentId the student id
     * @return the history of student record as a serialized {@link RecordHistoryList}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getStudentRecordHistory(final Context ctx, final long studentId) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyModification> iterator = stub.getHistoryForKey(composePublicKey(ctx, RECORD_PREFIX, Long.toString(studentId)).toString());
        List<RecordHistory> histories = new ArrayList<>();
        for (KeyModification keyModification : iterator) {
            RecordHistory history = new RecordHistory();
            history.setTimestamp(Date.from(keyModification.getTimestamp()));
            history.setRecord(JsonUtil.deserialize(keyModification.getStringValue(), Record.class));
            history.setUpdatedBy(keyModification.getTxId());
            histories.add(history);
        }
        return JsonUtil.serialize(new RecordHistoryList(histories));
    }

    ChaincodeException newChainException(AssetErrors error, String message) {
        return new ChaincodeException(error.name() + ": " + message, error.name());
    }

    String getCollectionName(Context ctx) {
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        byte[] collectionNameBytes = transientMap.get("collectionName");
        if (collectionNameBytes != null) {
            String collectionName = new String(collectionNameBytes, StandardCharsets.UTF_8);
            if (!collectionName.isEmpty()) {
                return collectionName;
            }
        }
        return "_implicit_org_" + ctx.getClientIdentity().getMSPID();
    }

    CompositeKey composePublicKey(Context ctx, String type, String... key) {
        String[] combined = new String[key.length + 1];
        combined[0] = ctx.getClientIdentity().getMSPID();
        System.arraycopy(key, 0, combined, 1, key.length);
        return ctx.getStub().createCompositeKey(type, combined);
    }

    CompositeKey composePrefixKey(Context ctx, String type) {
        return ctx.getStub().createCompositeKey(type, ctx.getClientIdentity().getMSPID());
    }

    List<String> verifyAndGetAttributes(Context ctx, String key, String type) {
        CompositeKey compositeKey = ctx.getStub().splitCompositeKey(key);
        if (!compositeKey.getObjectType().equals(type)) {
            throw newChainException(AssetErrors.ASSET_INVALID, "Invalid composite key type");
        }
        List<String> attributes = compositeKey.getAttributes();
        if (attributes.isEmpty()) {
            throw newChainException(AssetErrors.ASSET_INVALID, "Invalid composite key");
        }
        if (!attributes.get(0).equals(ctx.getClientIdentity().getMSPID())) {
            throw newChainException(AssetErrors.ASSET_INVALID, "Invalid composite key");
        }
        return attributes.subList(1, attributes.size());
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
