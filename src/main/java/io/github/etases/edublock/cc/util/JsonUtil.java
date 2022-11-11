package io.github.etases.edublock.cc.util;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtil {
    private static final Genson genson = new GensonBuilder()
            .useRuntimeType(true)
            .failOnMissingProperty(true)
            .failOnNullPrimitive(true)
            .create();

    public static <T> T deserialize(String jsonString, Class<T> clazz) {
        return genson.deserialize(jsonString, clazz);
    }

    public static String serialize(Object obj) {
        return genson.serialize(obj);
    }
}
