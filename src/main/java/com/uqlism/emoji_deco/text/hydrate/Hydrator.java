package com.uqlism.emoji_deco.text.hydrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Converts a JsonElement into T using a HydrateContext.
 * Returns null to signal "this element is not handled by this hydrator."
 * Build instances once as static finals; hydrate() calls are stateless.
 */
@FunctionalInterface
public interface Hydrator<T> {

    @Nullable T hydrate(JsonElement el, HydrateContext.Tracked ctx);

    @FunctionalInterface interface TriFunction <A, B, C, R>    { R apply(A a, B b, C c); }
    @FunctionalInterface interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d); }

    // ── Instance combinators ───────────────────────────────────────────────────

    /** Applies f to the result; propagates null. */
    default <R> Hydrator<R> map(Function<T, R> f) {
        return (el, ctx) -> { T r = hydrate(el, ctx); return r != null ? f.apply(r) : null; };
    }

    /** Replaces null with def, guaranteeing a non-null return. */
    default Hydrator<T> withDefault(T def) {
        return (el, ctx) -> { T r = hydrate(el, ctx); return r != null ? r : def; };
    }

    // ── Static combinators ─────────────────────────────────────────────────────

    /** Runs effect on ctx before hydrating — used for side effects like markTimeAccessed. */
    static <T> Hydrator<T> withEffect(Consumer<HydrateContext.Tracked> effect, Hydrator<T> inner) {
        return (el, ctx) -> { effect.accept(ctx); return inner.hydrate(el, ctx); };
    }

    /** Extracts a named field from a JsonObject and hydrates it. */
    static <T> Hydrator<T> field(String key, Hydrator<T> inner) {
        return (el, ctx) -> el != null && el.isJsonObject()
                ? inner.hydrate(el.getAsJsonObject().get(key), ctx) : null;
    }

    /** Applies two hydrators to the same element and combines results. */
    static <A, B, R> Hydrator<R> zip(Hydrator<A> a, Hydrator<B> b, BiFunction<A, B, R> fn) {
        return (el, ctx) -> {
            A ra = a.hydrate(el, ctx); if (ra == null) return null;
            B rb = b.hydrate(el, ctx); if (rb == null) return null;
            return fn.apply(ra, rb);
        };
    }

    /** Applies three hydrators to the same element and combines results. */
    static <A, B, C, R> Hydrator<R> zip(
            Hydrator<A> a, Hydrator<B> b, Hydrator<C> c, TriFunction<A, B, C, R> fn) {
        return (el, ctx) -> {
            A ra = a.hydrate(el, ctx); if (ra == null) return null;
            B rb = b.hydrate(el, ctx); if (rb == null) return null;
            C rc = c.hydrate(el, ctx); if (rc == null) return null;
            return fn.apply(ra, rb, rc);
        };
    }

    /** Applies four hydrators to the same element and combines results. */
    static <A, B, C, D, R> Hydrator<R> zip(
            Hydrator<A> a, Hydrator<B> b, Hydrator<C> c, Hydrator<D> d, QuadFunction<A, B, C, D, R> fn) {
        return (el, ctx) -> {
            A ra = a.hydrate(el, ctx); if (ra == null) return null;
            B rb = b.hydrate(el, ctx); if (rb == null) return null;
            C rc = c.hydrate(el, ctx); if (rc == null) return null;
            D rd = d.hydrate(el, ctx); if (rd == null) return null;
            return fn.apply(ra, rb, rc, rd);
        };
    }

    /** Lazily resolves the Hydrator on each call — breaks static initialisation cycles. */
    static <T> Hydrator<T> lazy(Supplier<Hydrator<T>> supplier) {
        return (el, ctx) -> supplier.get().hydrate(el, ctx);
    }

    /** Tries each alternative in order; returns the first non-null result. */
    @SafeVarargs
    static <T> Hydrator<T> firstOf(Hydrator<T>... alts) {
        return (el, ctx) -> {
            for (var h : alts) { T r = h.hydrate(el, ctx); if (r != null) return r; }
            return null;
        };
    }

    /**
     * O(1) dispatch on the "type" field — optimised for discriminated unions.
     * Returns null when el is not a JsonObject or the type is not in cases.
     * fallback is used when el is a JsonObject with no "type" field.
     */
    static <T> Hydrator<T> dispatch(Map<String, Hydrator<T>> cases, @Nullable Hydrator<T> fallback) {
        return (el, ctx) -> {
            if (el == null || !el.isJsonObject()) return null;
            JsonObject obj = el.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : null;
            if (type != null) {
                Hydrator<T> h = cases.get(type);
                return h != null ? h.hydrate(el, ctx) : null;
            }
            return fallback != null ? fallback.hydrate(el, ctx) : null;
        };
    }

    /**
     * Hydrates a list of T from a JsonArray or a comma-separated arg ref.
     * Returns null for null / non-array / non-arg-ref inputs so firstOf can fall through.
     */
    static <T> Hydrator<List<T>> list(Hydrator<T> item) {
        return (el, ctx) -> {
            if (el == null || el.isJsonNull()) return null;
            if (el.isJsonArray()) {
                List<T> result = new ArrayList<>(el.getAsJsonArray().size());
                for (JsonElement e : el.getAsJsonArray()) {
                    T r = item.hydrate(e, ctx);
                    if (r != null) result.add(r);
                }
                return result;
            }
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if ("emoji_deco:arg".equals(obj.has("type") ? obj.get("type").getAsString() : null)) {
                    int index = obj.has("index") ? obj.get("index").getAsInt() : 0;
                    String raw = ctx.getArg(index);
                    JsonArray arr = new JsonArray();
                    if (!raw.isBlank())
                        for (String part : raw.split(",")) arr.add(part.strip());
                    List<T> result = new ArrayList<>(arr.size());
                    for (JsonElement e : arr) {
                        T r = item.hydrate(e, ctx);
                        if (r != null) result.add(r);
                    }
                    return result;
                }
            }
            return null;
        };
    }
}
