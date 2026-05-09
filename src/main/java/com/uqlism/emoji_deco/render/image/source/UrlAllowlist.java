package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * URL フェッチ許可ドメインのアローリスト。
 *
 * 各リソースパックの assets/emoji_deco/url_allowlist.json を読み込み、
 * allowed_domains のリストを全パック分 union する。
 * Config.additionalAllowedDomains（プレイヤー設定）もチェック対象に含める。
 *
 * パターン形式:
 *   "example.com"      → example.com のみ
 *   "*.example.com"    → 任意サブドメイン（example.com 自体は含まない）
 *
 * url_allowlist.json が1つも存在しない・allowed_domains が空の場合は全拒否。
 */
public class UrlAllowlist implements PreparableReloadListener {

    public static final UrlAllowlist INSTANCE = new UrlAllowlist();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath("emoji_deco", "url_allowlist.json");

    private volatile Set<String> packPatterns = Set.of();

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier, ResourceManager rm,
            ProfilerFiller prep, ProfilerFiller apply,
            Executor prepExec, Executor applyExec) {
        return CompletableFuture
                .supplyAsync(() -> loadFromPacks(rm), prepExec)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(patterns -> {
                    packPatterns = patterns;
                    if (patterns.isEmpty()) {
                        LOGGER.info("[EmojiDeco] url_allowlist: パターンなし — fetch_url は全拒否 " +
                                "(許可するには resource pack に url_allowlist.json を追加してください)");
                    } else {
                        LOGGER.info("[EmojiDeco] url_allowlist: {} パターンを読み込みました: {}",
                                patterns.size(), patterns);
                    }
                }, applyExec);
    }

    private static Set<String> loadFromPacks(ResourceManager rm) {
        Set<String> patterns = new HashSet<>();
        List<Resource> stack = rm.getResourceStack(LOCATION);
        for (Resource resource : stack) {
            try (InputStreamReader reader = new InputStreamReader(
                    resource.open(), StandardCharsets.UTF_8)) {
                JsonObject json = GsonHelper.parse(reader);
                for (JsonElement el : json.getAsJsonArray("allowed_domains")) {
                    String p = el.getAsString().strip().toLowerCase();
                    if (!p.isEmpty()) patterns.add(p);
                }
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] url_allowlist.json 読み込み失敗 ({}): {}",
                        resource.sourcePackId(), e.getMessage());
            }
        }
        return Set.copyOf(patterns);
    }

    /**
     * 指定 URL がアローリストで許可されているかチェックする。
     * Config.enableUrlFetch が false の場合は常に false。
     */
    public boolean isAllowed(String urlStr) {
        if (!Config.enableUrlFetch) return false;
        String host;
        try {
            host = new URL(urlStr).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return false;
        }
        for (String pattern : packPatterns) {
            if (matches(host, pattern)) return true;
        }
        for (String pattern : Config.additionalAllowedDomains) {
            if (matches(host, pattern.strip().toLowerCase())) return true;
        }
        return false;
    }

    private static boolean matches(String host, String pattern) {
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1); // ".example.com"
            return host.endsWith(suffix);
        }
        return host.equals(pattern);
    }
}
