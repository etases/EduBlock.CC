package io.github.etases.edublock.cc.util;

import com.owlike.genson.JsonBindingException;
import io.github.etases.edublock.cc.model.Classification;
import io.github.etases.edublock.cc.model.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void deserialize() {
        String jsonString = "{\"name\":\"TestSubject\",\"firstHalfScore\":9.0,\"secondHalfScore\":9.5,\"finalScore\":10.0}";
        Subject subject = JsonUtil.deserialize(jsonString, Subject.class);
        assertEquals("TestSubject", subject.getName());
        assertEquals(9.0, subject.getFirstHalfScore());
        assertEquals(9.5, subject.getSecondHalfScore());
        assertEquals(10.0, subject.getFinalScore());
    }

    @Test
    void deserializeNullProperty() {
        String jsonString = "{\"firstHalfClassify\":\"Good\",\"secondHalfClassify\":\"Bad\"}";
        Classification classification = JsonUtil.deserialize(jsonString, Classification.class);
        assertEquals("Good", classification.getFirstHalfClassify());
        assertEquals("Bad", classification.getSecondHalfClassify());
        assertNull(classification.getFinalClassify());
    }

    @Test
    void deserializeException() {
        String jsonString = "{\"name\":\"TestSubject\",\"firstHalfScore\":null,\"secondHalfScore\":\"test\",\"finalScore\":10.0}";
        assertThrows(JsonBindingException.class, () -> JsonUtil.deserialize(jsonString, Subject.class));
    }

    @Test
    void serialize() {
        Classification classification = new Classification(
                "Good", "Bad", "Empty"
        );
        String jsonString = JsonUtil.serialize(classification);
        Classification deserialized = JsonUtil.deserialize(jsonString, Classification.class);
        assertEquals(classification, deserialized);
    }
}