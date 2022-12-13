package io.github.etases.edublock.cc;

import io.github.etases.edublock.cc.model.Record;
import io.github.etases.edublock.cc.model.*;
import io.github.etases.edublock.cc.util.JsonUtil;
import org.assertj.core.api.ThrowableAssert;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EduBlockChainCodeTest {
    private static class MockKeyValue implements KeyValue {
        private final String key;
        private final String value;

        private MockKeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }


        @Override
        public String getKey() {
            return key;
        }

        @Override
        public byte[] getValue() {
            return value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getStringValue() {
            return value;
        }
    }

    private static class MockKeyModification implements KeyModification {
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

    private static class MockQueryResultsIterator<T> implements QueryResultsIterator<T> {
        protected final List<T> results;

        private MockQueryResultsIterator() {
            results = new ArrayList<>();
        }

        private MockQueryResultsIterator(List<T> results) {
            this.results = results;
        }


        @Override
        public void close() throws Exception {
            // do nothing
        }

        @Override
        public Iterator<T> iterator() {
            return results.iterator();
        }
    }

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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);

            long studentIdInput = 0;
            Personal personal = new Personal();
            personal.setFirstName("Tester");
            personal.setLastName("TestOrg");
            personal.setMale(false);
            String collectionName = contract.getCollectionName(ctx);
            String personalSerialized = JsonUtil.serialize(personal);
            when(stub.getPrivateDataUTF8(collectionName, compositeKey.toString())).thenReturn(personalSerialized);

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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);

            long studentIdInput = 0;
            String collectionName = contract.getCollectionName(ctx);

            when(stub.getPrivateDataUTF8(collectionName, compositeKey.toString())).thenReturn(null);
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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);

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

            verify(stub).putPrivateData(collectionName, compositeKey.toString(), personalSerialized.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class ChaincodeExceptionMessageTest {
        @Test
        void chaincodeExceptionMessage() {
            ChaincodeException chaincodeException = new ChaincodeException("Test");
            assertEquals("Test", chaincodeException.getMessage());
        }

        @Test
        void chaincodeWithErrorCode() {
            EduBlockChainCode contract = new EduBlockChainCode();
            ChaincodeException chaincodeException = contract.newChainException(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND, "Test");
            assertEquals("ASSET_NOT_FOUND: Test", chaincodeException.getMessage());
            assertEquals(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND.name(), new String(chaincodeException.getPayload()));
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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
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
            when(stub.getStringState(contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString())).thenReturn(recordSerialized);

            String output = contract.getStudentRecord(ctx, studentIdInput);
            Record recordOutput = JsonUtil.deserialize(output, Record.class);

            assertEquals(record, recordOutput);
        }

        @Test
        void getStudentRecordIsNull() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;

            when(stub.getStringState(contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString())).thenReturn(null);
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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            String publicKey = contract.composePublicKey(ctx, Long.toString(0)).toString();

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

            verify(stub).putStringState(publicKey, recordSerialized);
        }

        @Test
        void updateStudentClassRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
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
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString();
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
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString();
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
        @Test
        void getStudentRecordHistory() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(compositeKey.toString()).thenReturn("TestCK");
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
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
            MockRecordModificationResultsIterator iterator = new MockRecordModificationResultsIterator(recordHistoryList);

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString();
            when(stub.getHistoryForKey(publicKey)).thenReturn(iterator);

            String output = contract.getStudentRecordHistory(ctx, studentIdInput);
            RecordHistoryList outputRecordHistoryList = JsonUtil.deserialize(output, RecordHistoryList.class);

            assertEquals(recordHistoryList, outputRecordHistoryList);
        }

        @Test
        void getStudentRecordHistoryEmpty() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            CompositeKey compositeKey = mock(CompositeKey.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.createCompositeKey(anyString(), any())).thenReturn(compositeKey);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;
            String publicKey = contract.composePublicKey(ctx, Long.toString(studentIdInput)).toString();
            when(stub.getHistoryForKey(publicKey)).thenReturn(new MockRecordModificationResultsIterator(new RecordHistoryList(Collections.emptyList())));

            String output = contract.getStudentRecordHistory(ctx, studentIdInput);
            RecordHistoryList outputRecordHistoryList = JsonUtil.deserialize(output, RecordHistoryList.class);

            assertTrue(outputRecordHistoryList.getHistories().isEmpty());
        }

        private final class MockRecordModificationResultsIterator extends MockQueryResultsIterator<KeyModification> {
            private MockRecordModificationResultsIterator(Map<Instant, Record> recordMap) {
                List<Map.Entry<Instant, Record>> entries = new ArrayList<>(recordMap.entrySet());
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<Instant, Record> entry = entries.get(i);
                    results.add(i, new MockKeyModification(Integer.toString(i), entry.getKey(), JsonUtil.serialize(entry.getValue())));
                }
            }

            private MockRecordModificationResultsIterator(RecordHistoryList recordHistoryList) {
                for (RecordHistory recordHistory : recordHistoryList.getHistories()) {
                    results.add(new MockKeyModification(recordHistory.getUpdatedBy(), recordHistory.getTimestamp().toInstant(), JsonUtil.serialize(recordHistory.getRecord())));
                }
            }
        }
    }

    @Nested
    class AllPersonalTest {
        @Test
        void getAllStudentPersonals() {
            String mspId = "TestOrg";
            String clientId = "TestClient";
            String personalPrefix = "personal";

            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn(mspId);
            when(client.getId()).thenReturn(clientId);

            CompositeKey prefixKey = new CompositeKey(personalPrefix, mspId, clientId);
            when(stub.createCompositeKey(personalPrefix, mspId, clientId)).thenReturn(prefixKey);

            PersonalMap personalMap = new PersonalMap(new HashMap<>());

            Personal personal1 = new Personal();
            personal1.setFirstName("Test1");
            personalMap.getPersonals().put(0L, personal1);
            CompositeKey personalKey1 = new CompositeKey(personalPrefix, mspId, clientId, "0");
            when(stub.splitCompositeKey(personalKey1.toString())).thenReturn(personalKey1);

            Personal personal2 = new Personal();
            personal2.setFirstName("Test2");
            personalMap.getPersonals().put(1L, personal2);
            CompositeKey personalKey2 = new CompositeKey(personalPrefix, mspId, clientId, "1");
            when(stub.splitCompositeKey(personalKey2.toString())).thenReturn(personalKey2);

            QueryResultsIterator<KeyValue> iterator = new MockPersonalMapIterator(personalPrefix, mspId, clientId, personalMap);

            String collectionName = contract.getCollectionName(ctx);
            when(stub.getPrivateDataByPartialCompositeKey(collectionName, prefixKey)).thenReturn(iterator);

            String output = contract.getAllStudentPersonals(ctx);
            PersonalMap outputPersonalMap = JsonUtil.deserialize(output, PersonalMap.class);

            assertEquals(personalMap, outputPersonalMap);
        }

        @Test
        void getAllStudentPersonalsEmpty() {
            String mspId = "TestOrg";
            String clientId = "TestClient";
            String personalPrefix = "personal";

            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn(mspId);
            when(client.getId()).thenReturn(clientId);

            CompositeKey prefixKey = new CompositeKey(personalPrefix, mspId, clientId);
            when(stub.createCompositeKey(personalPrefix, mspId, clientId)).thenReturn(prefixKey);

            String collectionName = contract.getCollectionName(ctx);
            when(stub.getPrivateDataByPartialCompositeKey(collectionName, prefixKey)).thenReturn(new MockPersonalMapIterator(personalPrefix, mspId, clientId, new PersonalMap(new HashMap<>())));

            String output = contract.getAllStudentPersonals(ctx);
            PersonalMap outputPersonalMap = JsonUtil.deserialize(output, PersonalMap.class);

            assertTrue(outputPersonalMap.getPersonals().isEmpty());
        }

        private final class MockPersonalMapIterator extends MockQueryResultsIterator<KeyValue> {
            private MockPersonalMapIterator(String prefix, String mspId, String clientId, PersonalMap personalMap) {
                for (Map.Entry<Long, Personal> entry : personalMap.getPersonals().entrySet()) {
                    CompositeKey compositeKey = new CompositeKey(prefix, mspId, clientId, entry.getKey().toString());
                    results.add(new MockKeyValue(compositeKey.toString(), JsonUtil.serialize(entry.getValue())));
                }
            }
        }
    }

    @Nested
    class AllRecordTest {
        @Test
        void getAllStudentRecords() {
            String mspId = "TestOrg";
            String clientId = "TestClient";
            String recordPrefix = "record";

            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn(mspId);
            when(client.getId()).thenReturn(clientId);

            CompositeKey prefixKey = new CompositeKey(recordPrefix, mspId, clientId);
            when(stub.createCompositeKey(recordPrefix, mspId, clientId)).thenReturn(prefixKey);

            RecordMap recordMap = new RecordMap(new HashMap<>());

            Record record1 = new Record();
            Map<Long, ClassRecord> classRecords1 = new HashMap<>();
            ClassRecord classRecord1 = new ClassRecord();
            classRecord1.setGrade(1);
            classRecords1.put(0L, classRecord1);
            record1.setClassRecords(classRecords1);
            recordMap.getRecords().put(1L, record1);

            CompositeKey recordKey1 = new CompositeKey(recordPrefix, mspId, clientId, Long.toString(1L));
            when(stub.createCompositeKey(recordPrefix, mspId, clientId, Long.toString(1L))).thenReturn(recordKey1);
            when(stub.splitCompositeKey(recordKey1.toString())).thenReturn(recordKey1);

            Record record2 = new Record();
            Map<Long, ClassRecord> classRecords2 = new HashMap<>();
            ClassRecord classRecord2 = new ClassRecord();
            classRecord2.setGrade(2);
            classRecords2.put(1L, classRecord2);
            record2.setClassRecords(classRecords2);
            recordMap.getRecords().put(2L, record2);

            CompositeKey recordKey2 = new CompositeKey(recordPrefix, mspId, clientId, Long.toString(2L));
            when(stub.createCompositeKey(recordPrefix, mspId, clientId, Long.toString(2L))).thenReturn(recordKey2);
            when(stub.splitCompositeKey(recordKey2.toString())).thenReturn(recordKey2);

            Map<String, Record> map = new HashMap<>();
            map.put(recordKey1.toString(), record1);
            map.put(recordKey2.toString(), record2);
            QueryResultsIterator<KeyValue> iterator = new MockRecordMapIterator(map);

            when(stub.getStateByPartialCompositeKey(prefixKey)).thenReturn(iterator);

            String output = contract.getAllStudentRecords(ctx);
            RecordMap outputRecordMap = JsonUtil.deserialize(output, RecordMap.class);

            assertEquals(recordMap, outputRecordMap);
        }

        @Test
        void getAllStudentRecordsEmpty() {
            String mspId = "TestOrg";
            String clientId = "TestClient";
            String recordPrefix = "record";

            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn(mspId);
            when(client.getId()).thenReturn(clientId);

            CompositeKey prefixKey = new CompositeKey(recordPrefix, mspId, clientId);
            when(stub.createCompositeKey(recordPrefix, mspId, clientId)).thenReturn(prefixKey);

            when(stub.getStateByPartialCompositeKey(prefixKey)).thenReturn(new MockRecordMapIterator(new HashMap<>()));

            String output = contract.getAllStudentRecords(ctx);
            RecordMap outputRecordMap = JsonUtil.deserialize(output, RecordMap.class);

            assertTrue(outputRecordMap.getRecords().isEmpty());
        }

        private final class MockRecordMapIterator extends MockQueryResultsIterator<KeyValue> {
            private MockRecordMapIterator(Map<String, Record> map) {
                for (Map.Entry<String, Record> entry : map.entrySet()) {
                    results.add(new MockKeyValue(entry.getKey(), JsonUtil.serialize(entry.getValue())));
                }
            }
        }
    }
}
