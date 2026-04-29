package com.uqlism.emoji_deco.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import java.util.List;
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
 * The GL texture is sized to one frame (frameWidth × frameHeight). Each tick(),
 * the next frame's pixel rows are uploaded via glTexSubImage2D so the UV in
 * BakedGlyph (0→1, 0→1) always refers to the current frame without rebuilding
 * any glyph cache entries.
 *
 * Falls back to a static texture when no .mcmeta is present (frameCount=1,
 * frameHeight=totalHeight, supporting non-square images without buffer overflow).
 */
public class AnimatedGlyphTexture extends AbstractTexture {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation location;
    private NativeImage spriteSheet;
    private int frameWidth;
    private int frameHeight;   // height of one frame in the GL texture
    private int frameCount = 1;
    private int[] frameTimes;         // 各フレームの表示時間（tick 単位）
    private int[] cumulativeTicks;    // cumulative[i] = フレーム i の開始 tick
    private int totalLoopTicks = 1;   // 1ループ分の総 tick 数
    private int currentFrame = 0;
    private boolean animated = false;

    // ── fromFrames モード専用フィールド ───────────────────────────────────────
    private List<ImageDecoder.Frame> decodedFrames;  // null = .mcmeta モード

    public AnimatedGlyphTexture(ResourceLocation location) {
        this.location = location;
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

        // Frame height derived from total image height and frame count.
        frameHeight = totalHeight / frameCount;
        animated = frameCount > 1;
        buildCumulative();
    }

    /** cumulativeTicks と totalLoopTicks を frameTimes から構築する。 */
    private void buildCumulative() {
        cumulativeTicks = new int[frameCount];
        int sum = 0;
        for (int i = 0; i < frameCount; i++) {
            cumulativeTicks[i] = sum;
            sum += frameTimes[i];
        }
        totalLoopTicks = Math.max(1, sum);
    }

    private void uploadFrame(int frame) {
        RenderSystem.bindTexture(getId());
        // Upload one frame (frameWidth × frameHeight) from the vertical-strip spritesheet.
        // GL_UNPACK_SKIP_ROWS = frame * frameHeight skips to the correct row in the source image.
        spriteSheet.upload(0, 0, 0, 0, frame * frameHeight, frameWidth, frameHeight,
                false, false, false, false);
    }

    // ── fromFrames ファクトリ ─────────────────────────────────────────────────

    /**
     * 事前デコード済みフレームリストから AnimatedGlyphTexture を生成する。
     * レンダースレッドから呼ぶこと。frames は所有権が移る（close() で解放される）。
     */
    public static AnimatedGlyphTexture fromFrames(List<ImageDecoder.Frame> frames) {
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty frame list");
        AnimatedGlyphTexture tex = new AnimatedGlyphTexture(null);
        tex.decodedFrames = frames;
        tex.frameCount    = frames.size();
        tex.animated      = frames.size() > 1;

        ImageDecoder.Frame first = frames.get(0);
        tex.frameWidth  = first.pixels().getWidth();
        tex.frameHeight = first.pixels().getHeight();

        // durationMs → ticks (50ms/tick)
        tex.frameTimes = frames.stream()
                .mapToInt(f -> Math.max(1, f.durationMs() / 50))
                .toArray();
        tex.buildCumulative();

        // GL テクスチャを確保して最初のフレームをアップロード
        TextureUtil.prepareImage(tex.getId(), tex.frameWidth, tex.frameHeight);
        tex.uploadDecodedFrame(0);
        return tex;
    }

    // ── tick / upload ─────────────────────────────────────────────────────────

    /**
     * 絶対 tick を受け取ってフレームを更新する。レンダースレッドから呼ぶこと。
     * gameTick % totalLoopTicks でフレームを決定するため、オフスクリーン復帰時も
     * 正確なフレームが即座に表示される。
     */
    public void tick(long gameTick) {
        if (!animated) return;
        int newFrame = frameAt(gameTick);
        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            if (decodedFrames != null) uploadDecodedFrame(currentFrame);
            else                       uploadFrame(currentFrame);
        }
    }

    /** gameTick に対応するフレームインデックスを返す。 */
    private int frameAt(long gameTick) {
        int t = (int)(gameTick % totalLoopTicks);
        // cumulativeTicks は昇順なので末尾から線形探索（フレーム数は通常少ない）
        for (int i = frameCount - 1; i > 0; i--) {
            if (t >= cumulativeTicks[i]) return i;
        }
        return 0;
    }

    private void uploadDecodedFrame(int index) {
        RenderSystem.bindTexture(getId());
        decodedFrames.get(index).pixels().upload(0, 0, 0, false);
    }

    public boolean isAnimated() {
        return animated;
    }

    // ── load は .mcmeta モード専用 ───────────────────────────────────────────

    @Override
    public void load(ResourceManager rm) throws IOException {
        if (location == null)
            throw new IllegalStateException("load() は fromFrames() で作成したインスタンスでは使えません");
        // 既存の .mcmeta 読み込みロジック（以下 unchanged）
        if (spriteSheet != null) { spriteSheet.close(); spriteSheet = null; }
        animated = false; currentFrame = 0;

        Resource resource = rm.getResource(location)
                .orElseThrow(() -> new IOException("Missing texture: " + location));
        try (var is = resource.open()) {
            spriteSheet = NativeImage.read(is);
        }

        frameWidth = spriteSheet.getWidth();
        int totalHeight = spriteSheet.getHeight();
        frameHeight = totalHeight;
        frameCount  = 1;
        frameTimes  = new int[]{1};

        ResourceLocation mcmetaLoc = ResourceLocation.parse(
                location.getNamespace() + ":" + location.getPath() + ".mcmeta");
        Optional<Resource> mcmetaRes = rm.getResource(mcmetaLoc);
        if (mcmetaRes.isPresent()) {
            try (var reader = new InputStreamReader(mcmetaRes.get().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GsonHelper.parse(reader);
                if (json.has("animation")) parseAnimation(json.getAsJsonObject("animation"), totalHeight);
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Bad .mcmeta for {}: {}", location, e.getMessage());
            }
        }

        TextureUtil.prepareImage(getId(), frameWidth, frameHeight);
        uploadFrame(0);
    }

    @Override
    public void close() {
        if (decodedFrames != null) {
            decodedFrames.forEach(f -> f.pixels().close());
            decodedFrames = null;
        }
        if (spriteSheet != null) {
            spriteSheet.close();
            spriteSheet = null;
        }
        animated = false;
        releaseId();
    }
}
