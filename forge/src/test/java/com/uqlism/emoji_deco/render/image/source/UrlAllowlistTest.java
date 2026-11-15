package com.uqlism.emoji_deco.render.image.source;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.MinecraftTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Config.onLoad() never runs in unit tests, so enableUrlFetch/additionalAllowedDomains
 * are seeded manually here (mirrors what ModConfigEvent would populate at runtime).
 */
class UrlAllowlistTest extends MinecraftTestBase {

    @BeforeEach
    void setUp() {
        Config.enableUrlFetch = true;
        Config.additionalAllowedDomains = List.of();
    }

    @Test void unknownHostIsRejected() {
        assertFalse(UrlAllowlist.INSTANCE.isAllowed("https://never-allowed.example/x.png"));
    }

    @Test void allowDynamicSingleHost() {
        UrlAllowlist.INSTANCE.allowDynamic("dynamic-single.example");
        assertTrue(UrlAllowlist.INSTANCE.isAllowed("https://dynamic-single.example/x.png"));
    }

    @Test void allowDynamicIsCaseInsensitive() {
        UrlAllowlist.INSTANCE.allowDynamic("Dynamic-Case.Example");
        assertTrue(UrlAllowlist.INSTANCE.isAllowed("https://dynamic-case.example/x.png"));
    }

    @Test void allowDynamicCollection() {
        UrlAllowlist.INSTANCE.allowDynamic(Set.of("dynamic-multi-a.example", "dynamic-multi-b.example"));
        assertTrue(UrlAllowlist.INSTANCE.isAllowed("https://dynamic-multi-a.example/x.png"));
        assertTrue(UrlAllowlist.INSTANCE.isAllowed("https://dynamic-multi-b.example/x.png"));
    }

    @Test void allowDynamicDoesNotMatchSubdomains() {
        UrlAllowlist.INSTANCE.allowDynamic("dynamic-exact.example");
        assertFalse(UrlAllowlist.INSTANCE.isAllowed("https://sub.dynamic-exact.example/x.png"));
    }

    @Test void enableUrlFetchFalseOverridesDynamicAllowlist() {
        UrlAllowlist.INSTANCE.allowDynamic("dynamic-disabled.example");
        Config.enableUrlFetch = false;
        assertFalse(UrlAllowlist.INSTANCE.isAllowed("https://dynamic-disabled.example/x.png"));
    }
}
