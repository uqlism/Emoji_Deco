package com.uqlism.emoji_deco.render.image.source;

import java.util.concurrent.ConcurrentHashMap;

/** デコード失敗した URL の負キャッシュ。失敗は一定時間ブロックし、ログも初回のみにする。 */
final class DecodeFailureCache {
    static final long BLOCK_MS = 10 * 60_000L;
    private static final int MAX_ENTRIES = 1024;

    private final ConcurrentHashMap<String, Long> failedAt = new ConcurrentHashMap<>();

    boolean isBlocked(String url, long nowMs) {
        Long t = failedAt.get(url);
        if (t == null) return false;
        if (nowMs - t >= BLOCK_MS) {
            failedAt.remove(url, t);
            return false;
        }
        return true;
    }

    /** 失敗を記録する。ブロック中でなかった (= 初回ログすべき) 場合に true。 */
    boolean recordFailure(String url, long nowMs) {
        boolean wasBlocked = isBlocked(url, nowMs);
        if (failedAt.size() >= MAX_ENTRIES && !failedAt.containsKey(url)) failedAt.clear();
        failedAt.put(url, nowMs);
        return !wasBlocked;
    }
}
