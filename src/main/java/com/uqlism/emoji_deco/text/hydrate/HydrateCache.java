package com.uqlism.emoji_deco.text.hydrate;

import com.uqlism.emoji_deco.text.ir.RichNode;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-shortcode / per-decorator hydration result cache.
 *
 * Each entry stores an AccessPattern (which context dimensions affected the result) and
 * the cached RichNode. Entries not accessed for STALE_TICKS are GC'd.
 *
 * Thread-safety: callers (ShortcodeManager / DecoratorManager) are responsible for synchronization.
 */
public final class HydrateCache {
    private static final long STALE_TICKS  = 600L;   // 30 seconds
    private static final int  MAX_ENTRIES  = 64;      // upper bound before forced GC

    private final List<Entry> entries = new ArrayList<>();

    private record Entry(HydrateContext.AccessPattern pattern, RichNode result, long lastUsedTick) {}

    @Nullable
    public RichNode lookup(HydrateContext ctx, @Nullable RichNode slot, long currentTick) {
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.pattern().matches(ctx, slot)) {
                entries.set(i, new Entry(e.pattern(), e.result(), currentTick));
                return e.result();
            }
        }
        return null;
    }

    public void store(HydrateContext.AccessPattern pattern, RichNode result, long currentTick) {
        entries.add(new Entry(pattern, result, currentTick));
        if (entries.size() > MAX_ENTRIES) gc(currentTick);
    }

    public void gc(long currentTick) {
        entries.removeIf(e -> currentTick - e.lastUsedTick() > STALE_TICKS);
    }

    public void clear() { entries.clear(); }
}
