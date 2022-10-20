package io.github.etases.edublock.cc.util;

import io.github.etases.edublock.cc.model.Classification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void deserialize() {
        String jsonString = "{\"firstHaftClassify\":\"Good\",\"secondHaftClassify\":\"Bad\"}";
        Classification classification = JsonUtil.deserialize(jsonString, Classification.class);
        assertEquals("Good", classification.getFirstHaftClassify());
        assertEquals("Bad", classification.getSecondHaftClassify());
        assertNull(classification.getFinalClassify());
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