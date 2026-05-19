package com.uqlism.emoji_deco.text.fetch;

import com.uqlism.emoji_deco.network.UrlFetcher;
import com.uqlism.emoji_deco.render.image.BinarySource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * decode_str 用の非同期テキスト取得。
 * HTTP フェッチは UrlFetcher に委譲する。
 * このクラスは「再フェッチ中も前回の成功値を返す」動作のみ担う。
 */
public final class TextFetchManager {

    private static final int MAX_BYTES = 1024 * 1024; // 1 MB

    /** 再フェッチ中でも前回の成功値を返せるよう、最後に成功したバイト列を保持する。 */
    private static final Map<BinarySource.Url, byte[]> LAST_KNOWN = new ConcurrentHashMap<>();

    /**
     * レンダースレッドから安全に呼べる同期メソッド。
     * フェッチ完了済みならバイト列を返す。未完了・失敗中は前回成功値を返す（なければ null）。
     */
    public static @Nullable byte[] getNow(BinarySource.Url source) {
        var future = UrlFetcher.fetch(source, MAX_BYTES);
        if (future.isDone() && !future.isCompletedExceptionally()) {
            try {
                byte[] bytes = future.get();
                LAST_KNOWN.put(source, bytes);
                return bytes;
            } catch (Exception ignored) {}
        }
        return LAST_KNOWN.get(source);
    }

    private TextFetchManager() {}
}
