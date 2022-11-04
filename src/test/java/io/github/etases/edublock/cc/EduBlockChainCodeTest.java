package io.github.etases.edublock.cc;

import io.github.etases.edublock.cc.model.*;
import io.github.etases.edublock.cc.model.Record;
import io.github.etases.edublock.cc.util.JsonUtil;
import org.assertj.core.api.ThrowableAssert;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EduBlockChainCodeTest {
    @Nested
    class TransientMapTest {
        @Test
        void getValueFromTransientMap() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);
            String transientKey = "classification";
            Classification classification = new Classification("Good", "Bad", "Empty");
            String jsonString = JsonUtil.serialize(classification);
            transientMap.put(transientKey, jsonString.getBytes(StandardCharsets.UTF_8));

            Classification classificationOutput = contract.getValueFromTransientMap(ctx, transientKey, Classification.class);

            assertEquals(classification, classificationOutput);
        }

        @Test
        void getValueFromTransientMapNotFound() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);
            String transientKey = "classification";

            ChaincodeException chaincodeException = ThrowableAssert.catchThrowableOfType(() -> {
                contract.getValueFromTransientMap(ctx, transientKey, Classification.class);
            }, ChaincodeException.class);

            assertArrayEquals(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND.name().getBytes(), chaincodeException.getPayload());

        }

        @Test
        void getValueFromTransientMapInvalid() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);
            String transientKey = "classification";
            transientMap.put(transientKey, "eror".getBytes(StandardCharsets.UTF_8));

            ChaincodeException chaincodeException = ThrowableAssert.catchThrowableOfType(() -> {
                contract.getValueFromTransientMap(ctx, transientKey, Classification.class);
            }, ChaincodeException.class);

            assertArrayEquals(EduBlockChainCode.AssetErrors.ASSET_INVALID.name().getBytes(), chaincodeException.getPayload());

        }
    }

    @Nested
    class PersonalTest {
        @Test
        void getStudentPersonal() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;
            Personal personal = new Personal();
            personal.setFirstName("Tester");
            personal.setLastName("TestOrg");
            personal.setMale(false);
            String collectionName = contract.getCollectionName(ctx);
            String personalSerialized = JsonUtil.serialize(personal);
            when(stub.getPrivateDataUTF8(collectionName, Long.toString(studentIdInput))).thenReturn(personalSerialized);

            Personal personalOutput = contract.getStudentPersonal(ctx, studentIdInput);

            assertEquals(personal, personalOutput);
        }

        @Test
        void getStudentPersonalIsNull() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;
            String collectionName = contract.getCollectionName(ctx);

            when(stub.getPrivateDataUTF8(collectionName, Long.toString(studentIdInput))).thenReturn(null);
            ChaincodeException chaincodeException = ThrowableAssert.catchThrowableOfType(() -> {
                contract.getStudentPersonal(ctx, studentIdInput);
            }, ChaincodeException.class);

            assertArrayEquals(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND.name().getBytes(), chaincodeException.getPayload());

        }

        @Test
        void updateStudentPersonal() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");


            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            String transientKey = "personal";
            Personal personal = new Personal();
            personal.setFirstName("Tester");
            personal.setLastName("TestOrg");
            personal.setMale(false);
            String personalSerialized = JsonUtil.serialize(personal);
            transientMap.put(transientKey, personalSerialized.getBytes(StandardCharsets.UTF_8));

            long studentIdInput = 0;
            String collectionName = contract.getCollectionName(ctx);

            contract.updateStudentPersonal(ctx, studentIdInput);

            verify(stub).putPrivateData(collectionName, Long.toString(studentIdInput), personalSerialized.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class RecordTest {
        @Test
        void getStudentRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");


            ClassRecord classrecord = new ClassRecord();
            classrecord.setYear(2020);
            Record record = new Record();
            Map<Long, ClassRecord> classRecordsMap = new HashMap<>();
            long classIdInput = 0;
            classRecordsMap.put(classIdInput, classrecord);
            record.setClassRecords(classRecordsMap);
            String recordSerialized = JsonUtil.serialize(record);
            long studentIdInput = 0;
            when(stub.getStringState(contract.composePublicKey(ctx, Long.toString(studentIdInput)))).thenReturn(recordSerialized);

            Record recordOutput = contract.getStudentRecord(ctx, studentIdInput);

            assertEquals(record, recordOutput);
        }

        @Test
        void getStudentRecordIsNull() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;

            when(stub.getStringState(contract.composePublicKey(ctx, Long.toString(studentIdInput)))).thenReturn(null);
            ChaincodeException chaincodeException = ThrowableAssert.catchThrowableOfType(() -> {
                contract.getStudentRecord(ctx, studentIdInput);
            }, ChaincodeException.class);

            assertArrayEquals(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND.name().getBytes(), chaincodeException.getPayload());

        }

        @Test
        void updateStudentRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            ClassRecord classRecord = new ClassRecord();
            classRecord.setYear(2020);
            Record record = new Record();
            long classIdInput = 0;
            Map<Long, ClassRecord> classRecordsMap = new HashMap<>();
            classRecordsMap.put(classIdInput, classRecord);
            record.setClassRecords(classRecordsMap);

            String recordSerialized = JsonUtil.serialize(record);
            String transientKey = "record";
            transientMap.put(transientKey, recordSerialized.getBytes(StandardCharsets.UTF_8));

            long studentIdInput = 0;
            contract.updateStudentRecord(ctx, studentIdInput);

            verify(stub).putStringState(contract.composePublicKey(ctx, Long.toString(studentIdInput)), recordSerialized);
        }

        @Test
        void updateStudentClassRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            ClassRecord oldClassRecord = new ClassRecord();
            oldClassRecord.setYear(2020);
            Record record = new Record();
            long oldClassIdInput = 0;
            Map<Long, ClassRecord> classRecordsMap = new HashMap<>();
            classRecordsMap.put(oldClassIdInput, oldClassRecord);
            record.setClassRecords(classRecordsMap);
            String recordSerialized = JsonUtil.serialize(record);

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput));
            when(stub.getStringState(publicKey)).thenReturn(recordSerialized);

            ClassRecord newClassRecord = new ClassRecord();
            newClassRecord.setYear(2021);
            String classRecordSerialized = JsonUtil.serialize(newClassRecord);
            String transientKey = "classRecord";
            transientMap.put(transientKey, classRecordSerialized.getBytes(StandardCharsets.UTF_8));

            Record newRecord = Record.clone(record);
            long classIdInput = 1;
            newRecord.getClassRecords().put(classIdInput, newClassRecord);
            String newRecordSerialized = JsonUtil.serialize(newRecord);

            contract.updateStudentClassRecord(ctx, studentIdInput, classIdInput);

            verify(stub).putStringState(publicKey, newRecordSerialized);
        }

        @Test
        void updateStudentClassRecordWhenNotExist() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput));
            when(stub.getStringState(publicKey)).thenReturn("");

            ClassRecord classRecord = new ClassRecord();
            classRecord.setYear(2020);
            long classIdInput = 0;
            String classRecordSerialized = JsonUtil.serialize(classRecord);
            String transientKey = "classRecord";
            transientMap.put(transientKey, classRecordSerialized.getBytes(StandardCharsets.UTF_8));

            Record record = Record.clone(null);
            record.getClassRecords().put(classIdInput, classRecord);
            String newRecordSerialized = JsonUtil.serialize(record);

            contract.updateStudentClassRecord(ctx, studentIdInput, classIdInput);

            verify(stub).putStringState(publicKey, newRecordSerialized);
        }
    }

    @Nested
    class HistoryTest {
        private final class MockKeyModification implements KeyModification {
            private final String txId;
            private final Instant timestamp;
            private final String value;

            private MockKeyModification(String txId, Instant timestamp, String value) {
                this.txId = txId;
                this.timestamp = timestamp;
                this.value = value;
            }

            @Override
            public String getTxId() {
                return txId;
            }

            @Override
            public byte[] getValue() {
                return value.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getStringValue() {
                return value;
            }

            @Override
            public Instant getTimestamp() {
                return timestamp;
            }

            @Override
            public boolean isDeleted() {
                return false;
            }
        }

        private final class MockKeyModificationResultsIterator implements QueryResultsIterator<KeyModification> {
            private final List<KeyModification> list;

            private MockKeyModificationResultsIterator(List<KeyModification> list) {
                this.list = list;
            }

            private MockKeyModificationResultsIterator(Map<Instant, Record> recordMap) {
                this.list = new ArrayList<>();
                List<Map.Entry<Instant, Record>> entries = new ArrayList<>(recordMap.entrySet());
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<Instant, Record> entry = entries.get(i);
                    list.add(i, new MockKeyModification(Integer.toString(i), entry.getKey(), JsonUtil.serialize(entry.getValue())));
                }
            }

            private MockKeyModificationResultsIterator(RecordHistoryList recordHistoryList) {
                this.list = new ArrayList<>();
                for (RecordHistory recordHistory : recordHistoryList.getHistories()) {
                    list.add(new MockKeyModification(recordHistory.getUpdatedBy(), recordHistory.getTimestamp().toInstant(), JsonUtil.serialize(recordHistory.getRecord())));
                }
            }

            @Override
            public void close() {
                // EMPTY
            }

            @Override
            public Iterator<KeyModification> iterator() {
                return list.iterator();
            }
        }

        @Test
        void getStudentRecordHistory() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Record record1 = Record.clone(null);
            ClassRecord classRecord1 = ClassRecord.clone(null);
            classRecord1.setYear(2020);
            record1.getClassRecords().put(0L, classRecord1);

            Record record2 = Record.clone(null);
            ClassRecord classRecord2 = ClassRecord.clone(null);
            classRecord2.setYear(2021);
            record2.getClassRecords().put(0L, classRecord1);
            record2.getClassRecords().put(1L, classRecord2);

            List<RecordHistory> recordHistories = new ArrayList<>();
            recordHistories.add(new RecordHistory(Date.from(Instant.EPOCH), record1, "tx1"));
            recordHistories.add(new RecordHistory(Date.from(Instant.ofEpochMilli(1000000)), record2, "tx2"));

            RecordHistoryList recordHistoryList = new RecordHistoryList(recordHistories);
            MockKeyModificationResultsIterator iterator = new MockKeyModificationResultsIterator(recordHistoryList);

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput));
            when(stub.getHistoryForKey(publicKey)).thenReturn(iterator);

            RecordHistoryList recordHistoryListOutput = contract.getStudentRecordHistory(ctx, studentIdInput);

            assertEquals(recordHistoryList, recordHistoryListOutput);
        }

        @Test
        void getStudentRecordHistoryEmpty() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput));
            when(stub.getHistoryForKey(publicKey)).thenReturn(new MockKeyModificationResultsIterator(Collections.emptyList()));

            RecordHistoryList recordHistoryListOutput = contract.getStudentRecordHistory(ctx, studentIdInput);

            assertTrue(recordHistoryListOutput.getHistories().isEmpty());
        }
    }
}
