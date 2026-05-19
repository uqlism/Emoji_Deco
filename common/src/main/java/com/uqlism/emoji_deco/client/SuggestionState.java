package com.uqlism.emoji_deco.client;

import com.google.gson.JsonElement;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.hydrate.Hydrators;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;
import com.uqlism.emoji_deco.text.registry.SuggestionEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SuggestionState {

    public enum TriggerType { SHORTCODE, DECORATOR, DECORATOR_ARG, SHORTCODE_ARG }

    public record Entry(String label, Supplier<Component> preview, String insertion, TriggerType type) {}

    private static final long DEBOUNCE_MS = 200L;

    // ── instance state ────────────────────────────────────────────────────────

    public String lastInput = "";
    public int lastCursor = 0;
    public int pendingCursor = -1;
    public List<Entry> suggestions = Collections.emptyList();
    public int selectedIndex = 0;
    public int triggerPos = 0;
    private long pendingAt = -1L; // -1 = 計算済み or pending なし

    // ── per-screen registry ───────────────────────────────────────────────────

    // Accessed only on the client main thread; no synchronization needed.
    // WeakHashMap: entry is removed automatically when the screen is GC'd.
    private static final WeakHashMap<Object, SuggestionState> REGISTRY = new WeakHashMap<>();

    public static SuggestionState of(Object screen) {
        return REGISTRY.computeIfAbsent(screen, k -> new SuggestionState());
    }

    // ── update ────────────────────────────────────────────────────────────────

    /** キー入力ごとに呼ばれる。pending を記録するだけで計算はしない（debounce）。 */
    public void update(String text, int cursor) {
        lastInput = text;
        lastCursor = Math.min(cursor, text.length());
        pendingAt = System.currentTimeMillis();
        suggestions = Collections.emptyList();
        selectedIndex = 0;
    }

    /** レンダリング前に毎フレーム呼ぶ。DEBOUNCE_MS 経過後に初めてサジェストを計算する。 */
    public void computeIfReady() {
        if (pendingAt < 0) return;
        if (System.currentTimeMillis() - pendingAt < DEBOUNCE_MS) return;
        pendingAt = -1L;
        computeNow();
    }

    private void computeNow() {
        String active = lastInput.substring(0, lastCursor);

        int colonPos = RichTextParser.findActiveColonPos(active);
        int hashPos  = RichTextParser.findActiveHashPos(active);

        if (colonPos < 0 && hashPos < 0) { clear(); return; }

        int maxPos = Math.max(colonPos, hashPos);

        if (maxPos == hashPos) {
            String prefix = active.substring(hashPos + 1);
            int dotPos = prefix.indexOf('.');
            if (dotPos >= 0 && DecoratorManager.has(prefix.substring(0, dotPos))) {
                String tagName  = prefix.substring(0, dotPos);
                String afterDot = prefix.substring(dotPos + 1);
                int commaCount  = countCommas(afterDot);
                int lastComma   = afterDot.lastIndexOf(',');
                String argPrefix = lastComma >= 0 ? afterDot.substring(lastComma + 1) : afterDot;
                triggerPos = hashPos + 1 + dotPos + 1 + (lastComma >= 0 ? lastComma + 1 : 0);
                suggestions = buildArgSuggestions(
                        DecoratorManager.getArgSuggestionsSpec(tagName, commaCount),
                        argPrefix, TriggerType.DECORATOR_ARG);
            } else {
                triggerPos = hashPos;
                suggestions = buildStyleTagSuggestions(prefix);
            }
        } else {
            String prefix = active.substring(colonPos + 1);
            int dotPos = prefix.indexOf('.');
            if (dotPos >= 0 && ShortcodeManager.has(prefix.substring(0, dotPos))) {
                String code     = prefix.substring(0, dotPos);
                String afterDot = prefix.substring(dotPos + 1);
                int commaCount  = countCommas(afterDot);
                int lastComma   = afterDot.lastIndexOf(',');
                String argPrefix = lastComma >= 0 ? afterDot.substring(lastComma + 1) : afterDot;
                triggerPos = colonPos + 1 + dotPos + 1 + (lastComma >= 0 ? lastComma + 1 : 0);
                suggestions = buildArgSuggestions(
                        ShortcodeManager.getArgSuggestionsSpec(code, commaCount),
                        argPrefix, TriggerType.SHORTCODE_ARG);
            } else {
                triggerPos = colonPos;
                suggestions = buildShortcodeSuggestions(prefix);
            }
        }
        selectedIndex = 0;
    }

    // ── suggestion builders ───────────────────────────────────────────────────

    private static List<Entry> buildStyleTagSuggestions(String prefix) {
        if (prefix.isEmpty()) {
            List<String> recent = UsageHistory.getRecent(30, UsageHistory.Kind.DECORATOR);
            if (recent.isEmpty()) return Collections.emptyList();
            List<Entry> hist = new ArrayList<>();
            for (String canonical : recent) {
                if (!DecoratorManager.has(canonical)) continue;
                hist.add(new Entry(
                        DecoratorManager.getLabel(canonical),
                        () -> DecoratorManager.getPreview(canonical),
                        canonical, TriggerType.DECORATOR));
            }
            return hist;
        }
        List<Entry> results = new ArrayList<>();
        DecoratorManager.searchSuggestions(prefix, 30).forEach(r -> {
            String canonical = r.canonical();
            results.add(new Entry(
                    DecoratorManager.getLabel(canonical),
                    () -> DecoratorManager.getPreview(canonical),
                    canonical, TriggerType.DECORATOR));
        });
        return results;
    }

    private static List<Entry> buildShortcodeSuggestions(String prefix) {
        if (prefix.isEmpty()) {
            List<String> recent = UsageHistory.getRecent(30, UsageHistory.Kind.SHORTCODE);
            if (recent.isEmpty()) return Collections.emptyList();
            List<Entry> hist = new ArrayList<>();
            for (String canonical : recent) {
                if (!ShortcodeManager.has(canonical)) continue;
                hist.add(new Entry(
                        ShortcodeManager.getLabel(canonical),
                        () -> ShortcodeManager.getPreview(canonical),
                        canonical, TriggerType.SHORTCODE));
            }
            return hist;
        }
        List<Entry> results = new ArrayList<>();
        ShortcodeManager.searchSuggestions(prefix, 30).forEach(r -> {
            String canonical = r.canonical();
            String alias     = r.matchedAlias();
            // alias でヒットした場合はそのエイリアス名をラベルに表示
            String label = alias != null ? ":" + alias + ":" : ShortcodeManager.getLabel(canonical);
            results.add(new Entry(label,
                    () -> ShortcodeManager.getPreview(canonical),
                    canonical, TriggerType.SHORTCODE));
        });
        return results;
    }

    private static List<Entry> buildArgSuggestions(JsonElement spec, String prefix, TriggerType type) {
        if (spec == null) return Collections.emptyList();
        List<String> resolved = Hydrators.STRING_LIST.hydrate(spec, HydrateContext.EMPTY.track());
        return (resolved != null ? resolved : List.<String>of()).stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .map(v -> {
                    TextColor color = TextColor.parseColor(v).result().orElse(null);
                    Supplier<Component> preview = color != null
                            ? () -> Component.literal("■ " + v).withStyle(Style.EMPTY.withColor(color))
                            : () -> Component.literal(v);
                    return new Entry(v, preview, v, type);
                })
                .collect(Collectors.toList());
    }

    // ── apply completion ──────────────────────────────────────────────────────

    public String applyTo(String text, Entry entry) {
        int split = Math.min(lastCursor, text.length());
        String before = text.substring(0, split);
        String after  = text.substring(split);

        switch (entry.type()) {
            case SHORTCODE -> {
                int pos = before.lastIndexOf(':');
                if (pos < 0) { pendingCursor = -1; return text; }
                UsageHistory.record(entry.insertion(), UsageHistory.Kind.SHORTCODE);
                String head = before.substring(0, pos + 1) + entry.insertion() + ":";
                pendingCursor = head.length();
                return head + after;
            }
            case DECORATOR -> {
                int pos = before.lastIndexOf('#');
                if (pos < 0) { pendingCursor = -1; return text; }
                UsageHistory.record(entry.insertion(), UsageHistory.Kind.DECORATOR);
                String head = before.substring(0, pos) + "#" + entry.insertion() + "[";
                pendingCursor = head.length();
                return head + "]" + after;
            }
            case DECORATOR_ARG -> {
                int hashPos = before.lastIndexOf('#');
                if (hashPos < 0) { pendingCursor = -1; return text; }
                int dotPos  = before.indexOf('.', hashPos);
                if (dotPos  < 0) { pendingCursor = -1; return text; }
                int lastComma = before.lastIndexOf(',');
                int argStart  = (lastComma > dotPos) ? lastComma + 1 : dotPos + 1;
                String head = before.substring(0, argStart) + entry.insertion() + "[";
                pendingCursor = head.length();
                return head + "]" + after;
            }
            case SHORTCODE_ARG -> {
                int colonPos = before.lastIndexOf(':');
                if (colonPos < 0) { pendingCursor = -1; return text; }
                int dotPos   = before.indexOf('.', colonPos);
                if (dotPos   < 0) { pendingCursor = -1; return text; }
                int lastComma = before.lastIndexOf(',');
                int argStart  = (lastComma > dotPos) ? lastComma + 1 : dotPos + 1;
                String head = before.substring(0, argStart) + entry.insertion() + ":";
                pendingCursor = head.length();
                return head + after;
            }
            default -> { pendingCursor = -1; return text; }
        }
    }

    // ── state helpers ─────────────────────────────────────────────────────────

    private static int countCommas(String s) {
        int count = 0;
        for (char c : s.toCharArray()) if (c == ',') count++;
        return count;
    }

    public void clear() {
        suggestions = Collections.emptyList();
        selectedIndex = 0;
        pendingAt = -1L;
    }

    public boolean hasSuggestions() { return !suggestions.isEmpty(); }

    public void moveUp() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public void moveDown() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex + 1) % suggestions.size();
    }

    public Entry getSelected() {
        return suggestions.isEmpty() ? null : suggestions.get(selectedIndex);
    }
}
