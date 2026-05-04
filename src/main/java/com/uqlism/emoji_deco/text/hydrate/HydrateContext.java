package com.uqlism.emoji_deco.text.hydrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.uqlism.emoji_deco.text.ir.RichNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Runtime context passed to NodeHydrator.hydrate().
 * Use track() to wrap it in a Tracked context that records which dimensions are accessed,
 * allowing derivation of a wildcard-based AccessPattern for caching.
 *
 * args は JsonElement[] で保持する。テキスト構文由来の引数は ShortcodeManager / DecoratorManager が
 * argSpec に従って事前に適切な型へ変換して渡す。JSON 呼び出し由来の引数はそのまま渡される。
 */
public final class HydrateContext {
    public static final HydrateContext EMPTY = new HydrateContext(new JsonElement[0], null, null);

    private final JsonElement[]       args;
    private final @Nullable JsonArray topArgSpecs;
    private final @Nullable RichNode  slot;

    public HydrateContext(JsonElement[] args, @Nullable JsonArray topArgSpecs, @Nullable RichNode slot) {
        this.args        = args;
        this.topArgSpecs = topArgSpecs;
        this.slot        = slot;
    }

    public JsonElement          getArg(int index)  { return index >= 0 && index < args.length ? args[index] : JsonNull.INSTANCE; }
    public @Nullable RichNode   getSlot()          { return slot; }
    public @Nullable JsonArray  getTopArgSpecs()   { return topArgSpecs; }
    public JsonElement[]        args()             { return args; }

    public Tracked track() { return new Tracked(this); }

    // ── テキスト構文 arg 変換 ───────────────────────────────────────────────────

    /**
     * テキスト構文（:shortcode.a,b: / #dec.a,b[...]）の String[] 引数を、
     * argSpecs の type フィールドに従って適切な JsonElement に変換する。
     * argSpecs が null または対応する spec がない場合は string 扱い。
     */
    public static JsonElement[] parseTextArgs(@Nullable JsonArray argSpecs, String[] textArgs) {
        JsonElement[] out = new JsonElement[textArgs.length];
        for (int i = 0; i < textArgs.length; i++)
            out[i] = parseTextArg(textArgs[i], argType(argSpecs, i));
        return out;
    }

    private static JsonElement parseTextArg(String s, @Nullable String type) {
        if (s.isEmpty()) return JsonNull.INSTANCE;
        return switch (type != null ? type : "string") {
            case "integer" -> { try { yield new JsonPrimitive(Integer.parseInt(s)); }
                                catch (NumberFormatException e) { yield JsonNull.INSTANCE; } }
            case "float"   -> { try { yield new JsonPrimitive(Float.parseFloat(s)); }
                                catch (NumberFormatException e) { yield JsonNull.INSTANCE; } }
            case "boolean" -> new JsonPrimitive(
                    s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("yes"));
            default        -> new JsonPrimitive(s);
        };
    }

    @Nullable
    private static String argType(@Nullable JsonArray specs, int idx) {
        if (specs == null || idx < 0 || idx >= specs.size()) return null;
        JsonElement el = specs.get(idx);
        if (!el.isJsonObject()) return null;
        JsonObject spec = el.getAsJsonObject();
        return spec.has("type") ? spec.get("type").getAsString() : null;
    }

    // ── Tracked ───────────────────────────────────────────────────────────────

    /** Wraps HydrateContext and records which dimensions are read during hydration. */
    public static final class Tracked {
        private final HydrateContext base;
        boolean slotAccessed, playerNamesAccessed, timeAccessed, urlDataAccessed;
        private final Map<Integer, JsonElement> accessedArgs = new LinkedHashMap<>();

        Tracked(HydrateContext base) { this.base = base; }

        public JsonElement getArg(int index) {
            JsonElement v = base.getArg(index);
            if (index >= 0) accessedArgs.put(index, v);
            return v;
        }
        public @Nullable RichNode  getSlot()        { slotAccessed = true; return base.getSlot(); }
        public @Nullable JsonArray getTopArgSpecs() { return base.getTopArgSpecs(); }
        public HydrateContext      base()           { return base; }

        public void markTimeAccessed()        { timeAccessed        = true; }
        public void markPlayerNamesAccessed() { playerNamesAccessed = true; }
        public void markUrlDataAccessed()     { urlDataAccessed     = true; }

        /** Creates a fresh sub-tracker sharing the same base context. */
        public Tracked sub() { return new Tracked(this.base); }

        /** Propagates all recorded deps into another tracker. */
        public void replayInto(Tracked other) {
            other.accessedArgs.putAll(this.accessedArgs);
            if (timeAccessed)        other.timeAccessed        = true;
            if (playerNamesAccessed) other.playerNamesAccessed = true;
            if (slotAccessed)        other.slotAccessed        = true;
            if (urlDataAccessed)     other.urlDataAccessed     = true;
        }

        public AccessPattern extractPattern() {
            RichNode snap = slotAccessed ? base.getSlot() : null;
            return new AccessPattern(timeAccessed, playerNamesAccessed, urlDataAccessed, slotAccessed, snap,
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
            boolean usesUrlData,
            boolean usesSlot,
            @Nullable RichNode slotSnapshot,
            Map<Integer, JsonElement> argValues) {

        /** True when the result depends on data that changes independently of args/slot. */
        public boolean isDynamic() { return usesTime || usesPlayerNames || usesUrlData; }

        /** True if the given context would produce the same hydration result. */
        boolean matches(HydrateContext ctx, @Nullable RichNode slot) {
            if (isDynamic()) return false;
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
