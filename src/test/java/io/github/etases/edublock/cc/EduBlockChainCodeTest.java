package io.github.etases.edublock.cc;

import io.github.etases.edublock.cc.model.ClassRecord;
import io.github.etases.edublock.cc.model.Classification;
import io.github.etases.edublock.cc.model.Personal;
import io.github.etases.edublock.cc.model.Record;
import io.github.etases.edublock.cc.util.JsonUtil;
import org.assertj.core.api.ThrowableAssert;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EduBlockChainCodeTest {
    @Nested
    class TransientMapTest {
        @Test
        public void getValueFromTransientMap() {
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
        public void getValueFromTransientMapNotFound() {
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
        public void getValueFromTransientMapInvalid() {
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
        public void getStudentPersonal() {
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
        public void getStudentPersonalIsNull() {
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
        public void updateStudentPersonal() {
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
        public void getStudentRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");



            ClassRecord classrecord =new ClassRecord();
            classrecord.setYear(2020);
            Record record = new Record();
            Map<Long, ClassRecord> classRecordsMap= new HashMap<>();
            long classIdInput=0;
            classRecordsMap.put(classIdInput,classrecord);
            record.setClassRecords(classRecordsMap);
            String recordSerialized = JsonUtil.serialize(record);
            long studentIdInput = 0;
            when(stub.getStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)))).thenReturn(recordSerialized);

            Record recordOutput = contract.getStudentRecord(ctx, studentIdInput);

            assertEquals(record, recordOutput);
        }
        @Test
        public void getStudentRecordIsNull() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            long studentIdInput = 0;

            when(stub.getStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)))).thenReturn(null);
            ChaincodeException chaincodeException = ThrowableAssert.catchThrowableOfType(() -> {
                contract.getStudentRecord(ctx, studentIdInput);
            }, ChaincodeException.class);

            assertArrayEquals(EduBlockChainCode.AssetErrors.ASSET_NOT_FOUND.name().getBytes(), chaincodeException.getPayload());

        }
        @Test
        public void updateStudentRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            ClassRecord classRecord =new ClassRecord();
            classRecord.setYear(2020);
            Record record = new Record();
            long classIdInput=0;
            Map<Long, ClassRecord> classRecordsMap= new HashMap<>();
            classRecordsMap.put(classIdInput,classRecord);
            record.setClassRecords(classRecordsMap);

            String recordSerialized = JsonUtil.serialize(record);
            String transientKey = "record";
            transientMap.put(transientKey, recordSerialized.getBytes(StandardCharsets.UTF_8));

            long studentIdInput = 0;
            contract.updateStudentRecord(ctx, studentIdInput);

            verify(stub).putStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)),recordSerialized);
        }

        @Test
        public void updateStudentClassRecord() {
            EduBlockChainCode contract = new EduBlockChainCode();
            Context ctx = mock(Context.class);
            ClientIdentity client = mock(ClientIdentity.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(client);
            when(client.getMSPID()).thenReturn("TestOrg");

            Map<String, byte[]> transientMap = new HashMap<>();
            when(stub.getTransient()).thenReturn(transientMap);

            ClassRecord oldClassRecord =new ClassRecord();
            oldClassRecord.setYear(2020);
            Record record = new Record();
            long oldClassIdInput=0;
            Map<Long, ClassRecord> classRecordsMap= new HashMap<>();
            classRecordsMap.put(oldClassIdInput,oldClassRecord);
            record.setClassRecords(classRecordsMap);
            String recordSerialized = JsonUtil.serialize(record);
            long studentIdInput = 0;
            when(stub.getStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)))).thenReturn(recordSerialized);

            ClassRecord newClassRecord =new ClassRecord();
            newClassRecord.setYear(2021);
            String classRecordSerialized = JsonUtil.serialize(newClassRecord);
            String transientKey = "classRecord";
            transientMap.put(transientKey, classRecordSerialized.getBytes(StandardCharsets.UTF_8));
            Record newRecord = contract.getStudentRecord(ctx, studentIdInput);
            long classIdInput=1;
            newRecord.getClassRecords().put(classIdInput, newClassRecord);

            String newRecordSerialized = JsonUtil.serialize(newRecord);
            contract.updateStudentClassRecord(ctx, studentIdInput,classIdInput);
            verify(stub).getStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)));
            verify(stub).putStringState(contract.composePublicKey(ctx,Long.toString(studentIdInput)),newRecordSerialized);

        }
    }
}
