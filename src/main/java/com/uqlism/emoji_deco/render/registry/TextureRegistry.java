package com.uqlism.emoji_deco.render.registry;

import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Manages codepoint allocation for emoji_deco:texture glyphs (individual textures, lazy-loaded). */
public class TextureRegistry {

    public static final ResourceLocation TEXTURE_FONT = ResourceLocation.parse("emoji_deco:texture");

    private static final Map<TextureKey, Integer> TEXTURE_TO_CP = new ConcurrentHashMap<>();
    private static final Map<Integer, TextureKey> CP_TO_TEXTURE = new ConcurrentHashMap<>();
    private static final AtomicInteger nextCp = new AtomicInteger(0xE000);

    // Animated textures: ticked each client tick, never LRU-evicted.
    private static final Map<ResourceLocation, AnimatedGlyphTexture> ANIMATED = new ConcurrentHashMap<>();

    // LRU eviction
    private static final Map<Integer, Long> LAST_USED = new ConcurrentHashMap<>();
    private static final Set<Integer> EVICTED = ConcurrentHashMap.newKeySet();
    static volatile long currentTick = 0L;
    /** Ticks before an unseen texture's GL object is released (~30s). */
    static final long EVICTION_TICKS = 600L;
    private static final long SCAN_INTERVAL = 20L;

    public record TextureKey(ResourceLocation texture, int width, int height) {}

    public static int allocate(ResourceLocation texture, int width, int height) {
        TextureKey key = new TextureKey(texture, width, height);
        return TEXTURE_TO_CP.computeIfAbsent(key, k -> {
            int cp = nextCp.getAndIncrement();
            CP_TO_TEXTURE.put(cp, k);
            return cp;
        });
    }

    public static TextureKey getTextureKey(int codePoint) {
        return CP_TO_TEXTURE.get(codePoint);
    }

    public static Component createComponent(ResourceLocation texture, int width, int height) {
        int cp = allocate(texture, width, height);
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(TEXTURE_FONT));
    }

    // ── animated texture management ───────────────────────────────────────────

    public static void putAnimated(ResourceLocation rl, AnimatedGlyphTexture tex) {
        ANIMATED.put(rl, tex);
    }

    /** Advances all animated textures by one tick. Must be called from the render thread. */
    public static void tickAnimatedTextures() {
        for (AnimatedGlyphTexture tex : ANIMATED.values()) {
            tex.tick();
        }
    }

    // ── LRU usage tracking ────────────────────────────────────────────────────

    /** Called by MixinFontSet each time a texture glyph is rendered. */
    public static void markUsed(int codePoint) {
        LAST_USED.put(codePoint, currentTick);
    }

    public static boolean isEvicted(int codePoint) {
        return EVICTED.contains(codePoint);
    }

    public static void clearEvicted(int codePoint) {
        EVICTED.remove(codePoint);
    }

    /**
     * Increments the tick counter and, every SCAN_INTERVAL ticks, collects textures
     * that have not been rendered for EVICTION_TICKS ticks.
     * Returns their ResourceLocations so the caller can release the GL objects.
     */
    public static List<ResourceLocation> tickAndEvict() {
        currentTick++;
        if (currentTick % SCAN_INTERVAL != 0) return List.of();

        List<ResourceLocation> toRelease = new ArrayList<>();
        LAST_USED.entrySet().removeIf(entry -> {
            if (currentTick - entry.getValue() > EVICTION_TICKS) {
                int cp = entry.getKey();
                TextureKey key = CP_TO_TEXTURE.get(cp);
                if (key != null && ANIMATED.containsKey(key.texture())) {
                    // Never evict animated textures.
                    // markUsed() keeps the timestamp fresh while the texture is on screen;
                    // if it goes off screen the stale entry is harmless — we just skip it here.
                    return false;
                }
                EVICTED.add(cp);
                if (key != null) toRelease.add(key.texture());
                return true;
            }
            return false;
        });
        return toRelease;
    }

    /** Called on resource reload; clears eviction and animation state (FontSet caches are rebuilt anyway). */
    public static void onResourceReload() {
        LAST_USED.clear();
        EVICTED.clear();
        // AnimatedGlyphTexture instances are closed by TextureManager on reload;
        // clear our references so tick() is no longer called on stale objects.
        ANIMATED.clear();
    }
}
