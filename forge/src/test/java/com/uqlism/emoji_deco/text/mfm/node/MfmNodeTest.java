package com.uqlism.emoji_deco.text.mfm.node;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MfmNodeTest {

    @Test
    void textStringifiesToItsLiteralContent() {
        MfmText text = new MfmText("hello");
        assertEquals(MfmNodeType.Text, text.type());
        assertEquals("hello", text.stringify());
    }

    @Test
    void textEqualsAndHashCodeAreStructural() {
        assertEquals(new MfmText("a"), new MfmText("a"));
        assertEquals(new MfmText("a").hashCode(), new MfmText("a").hashCode());
        assertNotEquals(new MfmText("a"), new MfmText("b"));
    }

    @Test
    void boldStringifiesWithAsteriskDelimitersAndNestedChildren() {
        MfmBold bold = new MfmBold(List.of(new MfmText("hi")));
        assertEquals(MfmNodeType.Bold, bold.type());
        assertEquals("**hi**", bold.stringify());
    }

    @Test
    void childrenHolderVarargsConstructorMatchesListConstructor() {
        MfmItalic viaList = new MfmItalic(List.of(new MfmText("x")));
        MfmItalic viaVarargs = new MfmItalic(new MfmText("x"));
        assertEquals(viaList, viaVarargs);
        assertEquals("<i>x</i>", viaVarargs.stringify());
    }

    @Test
    void childrenListIsDefensivelyCopiedAndImmutable() {
        List<IMfmNode> mutable = new java.util.ArrayList<>();
        mutable.add(new MfmText("a"));
        MfmStrike strike = new MfmStrike(mutable);
        mutable.add(new MfmText("b"));

        // the record's own children list must not observe the caller's later mutation
        assertEquals(1, strike.children().size());
        assertThrows(UnsupportedOperationException.class, () -> strike.children().add(new MfmText("c")));
    }

    @Test
    void addChildIsUnsupportedOnImmutableRecordNodes() {
        MfmSmall small = new MfmSmall(List.of());
        assertThrows(UnsupportedOperationException.class, () -> small.addChild(new MfmText("late")));
    }

    @Test
    void quoteStringifyPrependsGreaterThanToEveryLine() {
        MfmQuote quote = new MfmQuote(List.of(new MfmText("line1\nline2")));
        assertEquals("> line1\n> line2", quote.stringify());
    }

    @Test
    void mentionDerivesAcctFromUsernameAndHost() {
        MfmMention withHost = new MfmMention("alice", "example.com");
        assertEquals("@alice@example.com", withHost.acct());
        assertEquals(withHost.acct(), withHost.stringify());

        MfmMention withoutHost = new MfmMention("alice", null);
        assertEquals("@alice", withoutHost.acct());
    }

    @Test
    void mentionEqualityIsBasedOnUsernameAndHost() {
        assertEquals(new MfmMention("alice", "example.com"), new MfmMention("alice", "example.com"));
        assertNotEquals(new MfmMention("alice", "example.com"), new MfmMention("alice", "other.com"));
        assertNotEquals(new MfmMention("alice", null), new MfmMention("alice", "example.com"));
    }

    @Test
    void urlStringifyOnlyAddsBracketsWhenTrue() {
        assertEquals("https://example.com", new MfmUrl("https://example.com", false).stringify());
        assertEquals("https://example.com", new MfmUrl("https://example.com", null).stringify());
        assertEquals("<https://example.com>", new MfmUrl("https://example.com", true).stringify());
    }

    @Test
    void linkStringifyAddsSilentPrefixOnlyWhenSilent() {
        MfmLink loud = new MfmLink(false, "https://example.com", new MfmText("label"));
        MfmLink silent = new MfmLink(true, "https://example.com", new MfmText("label"));
        assertEquals("[label](https://example.com)", loud.stringify());
        assertEquals("?[label](https://example.com)", silent.stringify());
    }

    @Test
    void blockCodeStringifyOmitsLangFieldWhenNull() {
        assertEquals("```java\ncode\n```", new MfmBlockCode("code", "java").stringify());
        assertEquals("```\ncode\n```", new MfmBlockCode("code").stringify());
    }

    @Test
    void fnStringifyRendersFlagOnlyArgsWithoutEqualsSign() {
        MfmFn flagOnly = new MfmFn("spin", Map.of("x", MfmFn.NO_VALUE), new MfmText("content"));
        assertEquals("$[spin.x content]", flagOnly.stringify());

        MfmFn withValue = new MfmFn("scale", Map.of("x", "2"), new MfmText("content"));
        assertEquals("$[scale.x=2 content]", withValue.stringify());

        MfmFn noArgs = new MfmFn("tada", Map.of(), new MfmText("content"));
        assertEquals("$[tada content]", noArgs.stringify());
    }

    @Test
    void nodeTypeAttributesMatchMfmKtDefinitions() {
        assertEquals(Set.of(MfmNodeAttribute.Inline, MfmNodeAttribute.Simple), MfmNodeType.Text.attributes());
        assertEquals(Set.of(MfmNodeAttribute.Block), MfmNodeType.Quote.attributes());
        assertEquals(Set.of(MfmNodeAttribute.Virtuality), MfmNodeType.Nest.attributes());
        assertEquals(Set.of(MfmNodeAttribute.Inline), MfmNodeType.Bold.attributes());
    }

    @Test
    void unicodeEmojiAndEmojiCodeStringifyDifferently() {
        assertEquals("😀", new MfmUnicodeEmoji("😀").stringify());
        assertEquals(":blobcat:", new MfmEmojiCode("blobcat").stringify());
    }

    @Test
    void nestStringifiesAsChildrenConcatenationWithoutAnyDelimiter() {
        MfmNest nest = new MfmNest(new MfmText("a"), new MfmText("b"));
        assertEquals(MfmNodeType.Nest, nest.type());
        assertEquals("ab", nest.stringify());
    }

    @Test
    void searchStringifiesAsContentNotQuery() {
        MfmSearch search = new MfmSearch("cats", "cats [検索]");
        assertEquals("cats [検索]", search.stringify());
        assertFalse(search.stringify().equals(search.query()));
    }
}
