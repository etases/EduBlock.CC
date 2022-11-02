package io.github.etases.edublock.cc.util;

import io.github.etases.edublock.cc.model.Classification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void deserialize() {
        String jsonString = "{\"firstHalfClassify\":\"Good\",\"secondHalfClassify\":\"Bad\"}";
        Classification classification = JsonUtil.deserialize(jsonString, Classification.class);
        assertEquals("Good", classification.getFirstHalfClassify());
        assertEquals("Bad", classification.getSecondHalfClassify());
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