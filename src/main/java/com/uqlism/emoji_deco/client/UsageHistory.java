package com.uqlism.emoji_deco.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** 最近使ったショートコード／デコレータ名の履歴。クライアントメインスレッドからのみアクセスする。 */
public class UsageHistory {

    public enum Kind { SHORTCODE, DECORATOR }

    private static final int MAX  = 50;
    private static final Gson GSON = new Gson();

    private static final LinkedList<String> shortcodes = new LinkedList<>();
    private static final LinkedList<String> decorators = new LinkedList<>();
    private static boolean loaded = false;

    /** 確定時に呼ぶ。先頭に追加し、超過分を末尾から削除して非同期保存する。 */
    public static void record(String canonical, Kind kind) {
        ensureLoaded();
        LinkedList<String> list = listFor(kind);
        list.remove(canonical);
        list.addFirst(canonical);
        while (list.size() > MAX) list.removeLast();
        saveAsync();
    }

    /** 最大 limit 件の最近使ったエントリを返す（新しい順）。 */
    public static List<String> getRecent(int limit, Kind kind) {
        ensureLoaded();
        return listFor(kind).stream().limit(limit).collect(Collectors.toList());
    }

    private static LinkedList<String> listFor(Kind kind) {
        return kind == Kind.DECORATOR ? decorators : shortcodes;
    }

    // ── persistence ───────────────────────────────────────────────────────────

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = savePath();
        if (path == null || !Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root == null || !root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("shortcodes")) readInto(obj.getAsJsonArray("shortcodes"), shortcodes);
            if (obj.has("decorators")) readInto(obj.getAsJsonArray("decorators"), decorators);
        } catch (Exception ignored) {}
    }

    private static void readInto(JsonArray arr, LinkedList<String> list) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try { list.add(e.getAsString()); } catch (Exception ignored) {}
        }
    }

    private static void saveAsync() {
        Path path = savePath();
        if (path == null) return;
        List<String> sc = new ArrayList<>(shortcodes);
        List<String> dc = new ArrayList<>(decorators);
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject obj = new JsonObject();
                obj.add("shortcodes", GSON.toJsonTree(sc));
                obj.add("decorators", GSON.toJsonTree(dc));
                Files.writeString(path, GSON.toJson(obj));
            } catch (IOException ignored) {}
        });
    }

    private static Path savePath() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        return mc.gameDirectory.toPath().resolve("emoji_deco_history.json");
    }
}
