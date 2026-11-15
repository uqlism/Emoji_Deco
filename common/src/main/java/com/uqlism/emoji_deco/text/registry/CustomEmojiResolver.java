package com.uqlism.emoji_deco.text.registry;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.emoji.MisskeyEmojiProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Single entry point for resolving MFM {@code :name:} emoji codes to a {@link RichNode}.
 *
 * <p>Switches between a resourcepack provider ({@link ShortcodeManager}) and a Misskey
 * server provider ({@link MisskeyEmojiProvider}) based on {@link Config#emojiProviderType}
 * (see the MFM implementation plan, §4). When the provider is set to Misskey, ShortcodeManager
 * is still consulted as a fallback for names the Misskey server doesn't have (or hasn't
 * finished fetching yet). Callers should go through this class rather than calling
 * {@code ShortcodeManager} directly for emoji-code resolution.
 */
public final class CustomEmojiResolver {
    private CustomEmojiResolver() {}

    /** Resolves {@code name} to a display node, or {@code null} if unresolved. */
    public static @Nullable RichNode resolve(String name) {
        if (Config.emojiProviderType == Config.EmojiProviderType.MISSKEY) {
            RichNode viaMisskey = MisskeyEmojiProvider.resolve(name);
            if (viaMisskey != null) return viaMisskey;
        }
        return ShortcodeManager.has(name) ? ShortcodeManager.resolve(name, new String[0]) : null;
    }
}
