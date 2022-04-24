package us.ajg0702.leaderboards.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.UUID;

public class EasyJsonObject {
    JsonObject handle = new JsonObject();

    public EasyJsonObject add(String key, JsonElement value) {
        this.handle.add(key, value);
        return this;
    }

    public EasyJsonObject add(String key, String value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public EasyJsonObject add(String key, Number value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public EasyJsonObject add(String key, Boolean value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public JsonObject getHandle() {
        return handle;
    }
}
