package com.uqlism.emoji_deco.text.registry;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Misskey スタイルの多段階スコア付き絵文字/デコレーター検索エンジン。
 *
 * スコア (qLen = クエリ長):
 *   qLen+3  正規名と完全一致
 *   qLen+2  エイリアスと完全一致
 *   qLen+1  正規名が前方一致
 *   qLen+0  エイリアスが前方一致
 *   qLen-1  正規名 or エイリアスに部分一致 (contains)
 *   fuzzy   3文字以上クエリ専用・末尾に最大6件 (順序通りに50%超の文字が出現)
 */
public final class SuggestionEngine {

    private SuggestionEngine() {}

    /** 検索結果。matchedAlias は alias でヒットした場合のみ非 null。 */
    public record SearchResult(String canonical, @Nullable String matchedAlias) {}

    /**
     * @param canonicals  正規名の集合
     * @param aliases     alias→canonical マップ（なければ {@link Map#of()}）
     * @param query       ユーザー入力
     * @param maxResults  最大件数
     */
    public static List<SearchResult> search(
            Iterable<String> canonicals,
            Map<String, String> aliases,
            String query,
            int maxResults) {

        if (query.isEmpty()) return List.of();
        String q = query.toLowerCase(Locale.ROOT);
        int qLen = q.length();
        boolean doFuzzy = qLen >= 3;

        // canonical → List<alias> を構築
        Map<String, List<String>> aliasByCanonical = new HashMap<>();
        aliases.forEach((alias, canonical) ->
                aliasByCanonical.computeIfAbsent(canonical, k -> new ArrayList<>()).add(alias));

        record Candidate(String canonical, @Nullable String matchedAlias, int score) {}
        List<Candidate> nonFuzzy = new ArrayList<>();
        List<Candidate> fuzzy    = new ArrayList<>();

        for (String code : canonicals) {
            String cLow = code.toLowerCase(Locale.ROOT);
            int    best = nonFuzzyScore(cLow, q, qLen, false);
            String bestAlias = null;

            List<String> codeAliases = aliasByCanonical.get(code);
            if (codeAliases != null) {
                for (String alias : codeAliases) {
                    int s = nonFuzzyScore(alias.toLowerCase(Locale.ROOT), q, qLen, true);
                    if (s > best) { best = s; bestAlias = alias; }
                }
            }

            if (best > 0) {
                nonFuzzy.add(new Candidate(code, bestAlias, best));
                continue;
            }
            if (!doFuzzy) continue;

            // fuzzy: 正規名
            int fs = fuzzyScore(cLow, q);
            if (fs > 0) { fuzzy.add(new Candidate(code, null, fs)); continue; }
            // fuzzy: エイリアス
            if (codeAliases != null) {
                for (String alias : codeAliases) {
                    fs = fuzzyScore(alias.toLowerCase(Locale.ROOT), q);
                    if (fs > 0) { fuzzy.add(new Candidate(code, alias, fs)); break; }
                }
            }
        }

        nonFuzzy.sort((a, b) -> b.score() - a.score());
        fuzzy.sort((a, b) -> b.score() - a.score());

        List<SearchResult> results = new ArrayList<>(Math.min(maxResults, nonFuzzy.size() + 6));
        for (Candidate c : nonFuzzy) {
            if (results.size() >= maxResults) break;
            results.add(new SearchResult(c.canonical(), c.matchedAlias()));
        }
        int fuzzyAdded = 0;
        for (Candidate c : fuzzy) {
            if (results.size() >= maxResults || fuzzyAdded >= 6) break;
            results.add(new SearchResult(c.canonical(), c.matchedAlias()));
            fuzzyAdded++;
        }
        return results;
    }

    // ── スコア計算 ─────────────────────────────────────────────────────────────

    private static int nonFuzzyScore(String name, String query, int qLen, boolean isAlias) {
        if (name.equals(query))     return qLen + (isAlias ? 2 : 3);
        if (name.startsWith(query)) return qLen + (isAlias ? 0 : 1);
        if (name.contains(query))   return qLen - 1;
        return 0;
    }

    /** >50% の文字が順序通りに出現すればマッチ数を返す、それ以外 0。 */
    private static int fuzzyScore(String name, String query) {
        int matched = 0, ni = 0;
        for (int qi = 0; qi < query.length(); qi++) {
            char c = query.charAt(qi);
            while (ni < name.length() && name.charAt(ni) != c) ni++;
            if (ni < name.length()) { matched++; ni++; }
        }
        return (double) matched / query.length() > 0.5 ? matched : 0;
    }
}
