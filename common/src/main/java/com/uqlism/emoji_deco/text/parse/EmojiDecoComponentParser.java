package com.uqlism.emoji_deco.text.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * JSON ツリーを走査して emoji_deco:arg スペックを探すユーティリティ。
 * パース・ハイドレーション実装は Hydrators / RichTextParser に集約済み。
 */
public final class EmojiDecoComponentParser {

    private EmojiDecoComponentParser() {}

    /**
     * JSON ツリーを再帰的に走査し、指定 index の emoji_deco:arg スペックを返す。
     * 主に arg のサジェスト仕様取得で使用。
     */
    @Nullable
    public static JsonObject findArgSpec(JsonElement el, int index) {
        if (el == null) return null;
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if ("emoji_deco:arg".equals(obj.has("type") ? obj.get("type").getAsString() : null)) {
                if ((obj.has("index") ? obj.get("index").getAsInt() : 0) == index) return obj;
            }
            for (var entry : obj.entrySet()) {
                JsonObject found = findArgSpec(entry.getValue(), index);
                if (found != null) return found;
            }
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) {
                JsonObject found = findArgSpec(child, index);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * JSON ツリーを再帰的に走査し、すべての emoji_deco:arg スペックを index をキーとして返す。
     * 同一 index が複数存在する場合は最初の出現が優先される。
     */
    public static Map<Integer, JsonObject> findAllArgSpecs(JsonElement el) {
        Map<Integer, JsonObject> result = new TreeMap<>();
        collectArgSpecs(el, result);
        return result;
    }

    private static void collectArgSpecs(JsonElement el, Map<Integer, JsonObject> result) {
        if (el == null) return;
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if ("emoji_deco:arg".equals(obj.has("type") ? obj.get("type").getAsString() : null)) {
                int idx = obj.has("index") ? obj.get("index").getAsInt() : 0;
                result.putIfAbsent(idx, obj);
            }
            for (var entry : obj.entrySet()) collectArgSpecs(entry.getValue(), result);
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) collectArgSpecs(child, result);
        }
    }
}
