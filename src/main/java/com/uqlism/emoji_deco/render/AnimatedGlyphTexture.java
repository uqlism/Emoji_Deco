package com.uqlism.emoji_deco.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Standalone animated texture for emoji_deco:texture glyphs.
 *
 * The GL texture is sized to one frame (frameWidth × frameWidth). Each tick(),
 * the next frame's pixel rows are uploaded via glTexSubImage2D so the UV in
 * BakedGlyph (0→1, 0→1) always refers to the current frame without rebuilding
 * any glyph cache entries.
 *
 * Falls back to a static texture when no .mcmeta is present.
 */
public class AnimatedGlyphTexture extends AbstractTexture {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation location;
    private NativeImage spriteSheet;
    private int frameWidth;
    private int frameCount = 1;
    private int[] frameTimes;   // ticks each frame is displayed
    private int tickAccum = 0;
    private int currentFrame = 0;
    private boolean animated = false;

    public AnimatedGlyphTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void load(ResourceManager rm) throws IOException {
        // Idempotent: safe if called more than once (TextureManager.register may call it again).
        if (spriteSheet != null) {
            spriteSheet.close();
            spriteSheet = null;
        }
        animated = false;
        tickAccum = 0;
        currentFrame = 0;

        Resource resource = rm.getResource(location)
                .orElseThrow(() -> new IOException("Missing texture: " + location));
        try (var is = resource.open()) {
            spriteSheet = NativeImage.read(is);
        }

        frameWidth = spriteSheet.getWidth();
        int totalHeight = spriteSheet.getHeight();
        frameTimes = new int[]{1};

        ResourceLocation mcmetaLoc = ResourceLocation.parse(
                location.getNamespace() + ":" + location.getPath() + ".mcmeta");
        Optional<Resource> mcmetaRes = rm.getResource(mcmetaLoc);
        if (mcmetaRes.isPresent()) {
            try (var reader = new InputStreamReader(mcmetaRes.get().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GsonHelper.parse(reader);
                if (json.has("animation")) {
                    parseAnimation(json.getAsJsonObject("animation"), totalHeight);
                }
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Bad .mcmeta for {}: {}", location, e.getMessage());
            }
        }

        // GL texture holds exactly one frame; we overwrite it each tick.
        TextureUtil.prepareImage(getId(), frameWidth, frameWidth);
        uploadFrame(0);
    }

    private void parseAnimation(JsonObject anim, int totalHeight) {
        int defaultTime = anim.has("frametime") ? anim.get("frametime").getAsInt() : 1;

        if (anim.has("frames")) {
            JsonArray frames = anim.getAsJsonArray("frames");
            frameTimes = new int[frames.size()];
            for (int i = 0; i < frames.size(); i++) {
                JsonElement fe = frames.get(i);
                frameTimes[i] = (fe.isJsonObject() && fe.getAsJsonObject().has("time"))
                        ? fe.getAsJsonObject().get("time").getAsInt()
                        : defaultTime;
            }
            frameCount = frameTimes.length;
        } else {
            frameCount = Math.max(1, totalHeight / frameWidth);
            frameTimes = new int[frameCount];
            Arrays.fill(frameTimes, defaultTime);
        }

        animated = frameCount > 1;
    }

    private void uploadFrame(int frame) {
        RenderSystem.bindTexture(getId());
        // Upload one frame from the vertical-strip spritesheet.
        // unpackSkipY = frame * frameWidth skips to the correct row in the source image.
        spriteSheet.upload(0, 0, 0, 0, frame * frameWidth, frameWidth, frameWidth,
                false, false, false, false);
    }

    /** Called every client tick to advance the animation. Must be on the render thread. */
    public void tick() {
        if (!animated || spriteSheet == null) return;
        tickAccum++;
        if (tickAccum >= frameTimes[currentFrame]) {
            tickAccum = 0;
            currentFrame = (currentFrame + 1) % frameCount;
            uploadFrame(currentFrame);
        }
    }

    public boolean isAnimated() {
        return animated;
    }

    @Override
    public void close() {
        if (spriteSheet != null) {
            spriteSheet.close();
            spriteSheet = null;
        }
        animated = false;
        releaseId();
    }
}
