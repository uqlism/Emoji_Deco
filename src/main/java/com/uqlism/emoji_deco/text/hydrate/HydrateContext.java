package com.uqlism.emoji_deco.text.hydrate;

import com.uqlism.emoji_deco.text.ir.RichNode;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Runtime context passed to NodeHydrator.hydrate().
 * Use track() to wrap it in a Tracked context that records which dimensions are accessed,
 * allowing derivation of a wildcard-based AccessPattern for caching.
 */
public final class HydrateContext {
    public static final HydrateContext EMPTY = new HydrateContext(new String[0], null, null);

    private final String[]      args;
    private final @Nullable JsonArray topArgSpecs;
    private final @Nullable RichNode  slot;

    public HydrateContext(String[] args, @Nullable JsonArray topArgSpecs, @Nullable RichNode slot) {
        this.args        = args;
        this.topArgSpecs = topArgSpecs;
        this.slot        = slot;
    }

    public String            getArg(int index)  { return index >= 0 && index < args.length ? args[index] : ""; }
    public @Nullable RichNode  getSlot()          { return slot; }
    public @Nullable JsonArray getTopArgSpecs()   { return topArgSpecs; }
    public String[]            args()             { return args; }

    public Tracked track() { return new Tracked(this); }

    // ── Tracked ───────────────────────────────────────────────────────────────

    /** Wraps HydrateContext and records which dimensions are read during hydration. */
    public static final class Tracked {
        private final HydrateContext base;
        boolean slotAccessed, playerNamesAccessed, timeAccessed;
        private final Map<Integer, String> accessedArgs = new LinkedHashMap<>();

        Tracked(HydrateContext base) { this.base = base; }

        public String getArg(int index) {
            String v = base.getArg(index);
            if (index >= 0) accessedArgs.put(index, v);
            return v;
        }
        public @Nullable RichNode  getSlot()        { slotAccessed = true; return base.getSlot(); }
        public @Nullable JsonArray getTopArgSpecs() { return base.getTopArgSpecs(); }
        public HydrateContext      base()           { return base; }

        public void markTimeAccessed()        { timeAccessed        = true; }
        public void markPlayerNamesAccessed() { playerNamesAccessed = true; }

        public AccessPattern extractPattern() {
            RichNode snap = slotAccessed ? base.getSlot() : null;
            return new AccessPattern(timeAccessed, playerNamesAccessed, slotAccessed, snap,
                    Collections.unmodifiableMap(new LinkedHashMap<>(accessedArgs)));
        }
    }

    // ── AccessPattern ─────────────────────────────────────────────────────────

    /**
     * Wildcard-based cache key derived from a Tracked context.
     * Only accessed dimensions are constrained; all others are wildcards.
     * slotSnapshot stores the slot value used during the original hydration.
     */
    public record AccessPattern(
            boolean usesTime,
            boolean usesPlayerNames,
            boolean usesSlot,
            @Nullable RichNode slotSnapshot,
            Map<Integer, String> argValues) {

        /** True if the given context would produce the same hydration result. */
        boolean matches(HydrateContext ctx, @Nullable RichNode slot) {
            if (usesTime || usesPlayerNames) return false;   // never cache time/playerNames results
            for (var e : argValues.entrySet()) {
                if (!e.getValue().equals(ctx.getArg(e.getKey()))) return false;
            }
            if (usesSlot && !Objects.equals(slotSnapshot, slot)) return false;
            return true;
        }

        boolean isFullyStatic() {
            return !usesTime && !usesPlayerNames && !usesSlot && argValues.isEmpty();
        }
    }
}
