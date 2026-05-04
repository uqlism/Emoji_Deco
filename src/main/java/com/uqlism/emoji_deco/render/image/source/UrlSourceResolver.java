package com.uqlism.emoji_deco.render.image.source;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.network.UrlFetcher;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.ImageResolver;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * URL 画像リゾルバ。HTTP バイト取得は UrlFetcher に委譲し、デコードと GPU アップロードのみ担う。
 */
public final class UrlSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    static final int MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ClassLoader MOD_CL = UrlSourceResolver.class.getClassLoader();

    /** デコード専用スレッドプール。UrlFetcher の DL スレッドとは分離しデコード待ちを防ぐ。 */
    private static final Executor DECODE_EXECUTOR = new ThreadPoolExecutor(
            0, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "EmojiDeco-ImgDecode-" + COUNTER.getAndIncrement());
                t.setDaemon(true);
                t.setContextClassLoader(MOD_CL);
                return t;
            });

    /**
     * URL 画像を非同期で解決する。バイト取得はキャッシュされるが、デコードと GPU アップロードは
     * 毎回フレッシュに実行するため GPU エビクション後の再描画でも新しい ResolvedSource が返る。
     */
    public static CompletableFuture<ResolvedSource> resolve(
            BinarySource.Url source, @Nullable String format) {
        return UrlFetcher.fetch(source, MAX_BYTES)
                .thenCompose(bytes -> {
                    Format fmt = ImageFormatDetector.detect(bytes, format);
                    return CompletableFuture.<List<ImageDecoder.Frame>>supplyAsync(() -> {
                        try {
                            List<ImageDecoder.Frame> frames = ImageResolver.decoderFor(fmt).decode(bytes);
                            if (frames.isEmpty())
                                throw new IOException("No frames decoded from " + source.url());
                            return frames;
                        } catch (IOException e) {
                            LOGGER.error("[EmojiDeco] decode 失敗 [{}]: {}", source.url(), e.toString());
                            throw new RuntimeException(e);
                        }
                    }, DECODE_EXECUTOR)
                    .thenCompose(ImageResolver::uploadAsync);
                });
    }

    private UrlSourceResolver() {}
}
