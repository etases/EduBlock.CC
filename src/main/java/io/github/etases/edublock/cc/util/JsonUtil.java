package io.github.etases.edublock.cc.util;

import com.owlike.genson.Genson;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtil {
    private static final Genson genson = new Genson();

    public static <T> T deserialize(String jsonString, Class<T> clazz) {
        return genson.deserialize(jsonString, clazz);
    }

    public static String serialize(Object obj) {
        return genson.serialize(obj);
    }
}
