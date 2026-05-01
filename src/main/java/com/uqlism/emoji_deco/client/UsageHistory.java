package com.uqlism.emoji_deco.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** 最近使ったショートコード名の履歴。クライアントメインスレッドからのみアクセスする。 */
public class UsageHistory {

    private static final int MAX  = 50;
    private static final Gson GSON = new Gson();

    private static final LinkedList<String> history = new LinkedList<>();
    private static boolean loaded = false;

    /** ショートコード確定時に呼ぶ。先頭に追加し、超過分を末尾から削除して非同期保存する。 */
    public static void record(String canonical) {
        ensureLoaded();
        history.remove(canonical);
        history.addFirst(canonical);
        while (history.size() > MAX) history.removeLast();
        saveAsync();
    }

    /** 最大 limit 件の最近使ったショートコード正式名を返す（新しい順）。 */
    public static List<String> getRecent(int limit) {
        ensureLoaded();
        return history.stream().limit(limit).collect(Collectors.toList());
    }

    // ── persistence ───────────────────────────────────────────────────────────

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = savePath();
        if (path == null || !Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            if (arr == null) return;
            for (JsonElement e : arr) {
                try { history.add(e.getAsString()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void saveAsync() {
        Path path = savePath();
        if (path == null) return;
        List<String> snapshot = new ArrayList<>(history);
        CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(path, GSON.toJson(snapshot));
            } catch (IOException ignored) {}
        });
    }

    private static Path savePath() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        return mc.gameDirectory.toPath().resolve("emoji_deco_history.json");
    }
}
