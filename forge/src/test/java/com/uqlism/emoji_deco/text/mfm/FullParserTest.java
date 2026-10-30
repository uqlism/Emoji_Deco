// Ported from mfm.kt's api/src/test/kotlin/com/github/samunohito/mfm/api/FullParserTest.kt
package com.uqlism.emoji_deco.text.mfm;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmBlockCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmBold;
import com.uqlism.emoji_deco.text.mfm.node.MfmCenter;
import com.uqlism.emoji_deco.text.mfm.node.MfmEmojiCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmFn;
import com.uqlism.emoji_deco.text.mfm.node.MfmHashtag;
import com.uqlism.emoji_deco.text.mfm.node.MfmInlineCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmItalic;
import com.uqlism.emoji_deco.text.mfm.node.MfmLink;
import com.uqlism.emoji_deco.text.mfm.node.MfmMathBlock;
import com.uqlism.emoji_deco.text.mfm.node.MfmMathInline;
import com.uqlism.emoji_deco.text.mfm.node.MfmMention;
import com.uqlism.emoji_deco.text.mfm.node.MfmPlain;
import com.uqlism.emoji_deco.text.mfm.node.MfmQuote;
import com.uqlism.emoji_deco.text.mfm.node.MfmSearch;
import com.uqlism.emoji_deco.text.mfm.node.MfmSmall;
import com.uqlism.emoji_deco.text.mfm.node.MfmStrike;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.MfmUnicodeEmoji;
import com.uqlism.emoji_deco.text.mfm.node.MfmUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ported test-by-test (not mechanically transpiled) from mfm.kt's {@code FullParserTest.kt}.
 * Kotlin's {@code @Nested inner class X { @Test fun \`display name\`() {...} }} becomes JUnit 5's
 * {@code @Nested class X { @Test void displayName() {...} }}; backtick-named Kotlin test
 * functions get a camelCase Java name plus a {@link DisplayName} carrying the original text so
 * the intent isn't lost. Kotlin's {@code """...""".trimIndent()} multi-line quote fixtures are
 * pre-computed into their resulting {@code \n}-joined literal, since Java has no direct
 * equivalent of {@code trimIndent()}.
 */
class FullParserTest {

    private static void assertMfmNodeEquals(List<IMfmNode> expected, List<IMfmNode> actual) {
        assertEquals(expected, actual);
    }

    @Nested
    class Text {
        @Test
        @DisplayName("普通のテキストを入力すると1つのテキストノードが返される")
        void basic() {
            String input = "abc";
            List<IMfmNode> output = List.of(
                    new MfmText("abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Quote {
        @Test
        @DisplayName("1行の引用ブロックを使用できる")
        void singleLine() {
            String input = "> abc";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("複数行の引用ブロックを使用できる")
        void multipleLine() {
            String input = "> abc\n> 123";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmText("abc\n123"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("引用ブロックはブロックをネストできる")
        void nestableBlock() {
            String input = "> <center>\n> a\n> </center>";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmCenter(new MfmText("a")))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("引用ブロックはインライン構文を含んだブロックをネストできる")
        void nestableInlineBlock() {
            String input = "> <center>\n> I'm @ai@example.com, An bot of misskey!\n> </center>";
            List<IMfmNode> output = List.of(
                    new MfmQuote(
                            new MfmCenter(
                                    new MfmText("I'm "),
                                    new MfmMention("ai", "example.com"),
                                    new MfmText(", An bot of misskey!")
                            )
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("複数行の引用ブロックでは空行を含めることができる")
        void multipleLinesCanIncludeEmpty() {
            String input = "> abc\n>\n> 123";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmText("abc\n\n123"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("1行の引用ブロックを空行にはできない")
        void singleLinesCanNotEmpty() {
            String input = "> ";
            List<IMfmNode> output = List.of(
                    new MfmText("> ")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("引用ブロックの後ろの空行は無視される")
        void ignoreAfterEmptyLine() {
            String input = "> foo\n> bar\n\nhoge";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmText("foo\nbar")),
                    new MfmText("hoge")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("2つの引用行の間に空行がある場合は2つの引用ブロックが生成される")
        void generateMultipleQuoteBlock() {
            String input = "> foo\n\n> bar\n\nhoge";
            List<IMfmNode> output = List.of(
                    new MfmQuote(new MfmText("foo")),
                    new MfmQuote(new MfmText("bar")),
                    new MfmText("hoge")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Search {
        @Nested
        @DisplayName("検索構文を使用できる")
        class SearchFormat {
            @Test
            @DisplayName("Search")
            void test01() {
                String input = "MFM 書き方 123 Search";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("[Search]")
            void test02() {
                String input = "MFM 書き方 123 [Search]";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("search")
            void test03() {
                String input = "MFM 書き方 123 search";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("[search]")
            void test04() {
                String input = "MFM 書き方 123 [search]";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("検索")
            void test05() {
                String input = "MFM 書き方 123 検索";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("[検索]")
            void test06() {
                String input = "MFM 書き方 123 [検索]";
                List<IMfmNode> output = List.of(
                        new MfmSearch("MFM 書き方 123", input)
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Test
        @DisplayName("ブロックの前後にあるテキストが正しく解釈される")
        void beforeAndAfter() {
            String input = "abc\nhoge piyo bebeyo 検索\n123";
            List<IMfmNode> output = List.of(
                    new MfmText("abc"),
                    new MfmSearch("hoge piyo bebeyo", "hoge piyo bebeyo 検索"),
                    new MfmText("123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class CodeBlock {
        @Test
        @DisplayName("コードブロックを使用できる")
        void basic() {
            String input = "```\nabc\n```";
            List<IMfmNode> output = List.of(
                    new MfmBlockCode("abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("コードブロックには複数行のコードを入力できる")
        void basicMultiline() {
            String input = "```\na\nb\nc\n```";
            List<IMfmNode> output = List.of(
                    new MfmBlockCode("a\nb\nc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("コードブロックは言語を指定できる")
        void lang() {
            String input = "```js\nconst a = 1;\n```";
            List<IMfmNode> output = List.of(
                    new MfmBlockCode("const a = 1;", "js")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ブロックの前後にあるテキストが正しく解釈される")
        void beforeAfter() {
            String input = "abc\n```\nconst a = 1;\n```\n123";
            List<IMfmNode> output = List.of(
                    new MfmText("abc"),
                    new MfmBlockCode("const a = 1;"),
                    new MfmText("123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore internal marker")
        void ignoreInternalMarker() {
            String input = "```\naaa```bbb\n```";
            List<IMfmNode> output = List.of(
                    new MfmBlockCode("aaa```bbb")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("trim after line break")
        void trimAfterLineBreak() {
            String input = "```\nfoo\n```\nbar";
            List<IMfmNode> output = List.of(
                    new MfmBlockCode("foo"),
                    new MfmText("bar")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class MathBlock {
        @Test
        @DisplayName("1行の数式ブロックを使用できる")
        void basic() {
            String input = "\\[math1\\]";
            List<IMfmNode> output = List.of(
                    new MfmMathBlock("math1")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ブロックの前後にあるテキストが正しく解釈される")
        void beforeAfter() {
            String input = "abc\n\\[math1\\]\n123";
            List<IMfmNode> output = List.of(
                    new MfmText("abc"),
                    new MfmMathBlock("math1"),
                    new MfmText("123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("行末以外に閉じタグがある場合はマッチしない (末尾)")
        void notLineEnd() {
            String input = "\\[aaa\\]after";
            List<IMfmNode> output = List.of(
                    new MfmText("\\[aaa\\]after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("行末以外に閉じタグがある場合はマッチしない (先頭)")
        void notLineBegin() {
            String input = "before\\[aaa\\]";
            List<IMfmNode> output = List.of(
                    new MfmText("before\\[aaa\\]")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Center {
        @Test
        @DisplayName("single text")
        void singleText() {
            String input = "<center>abc</center>";
            List<IMfmNode> output = List.of(
                    new MfmCenter(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("multiple text")
        void multipleText() {
            String input = "before\n<center>\nabc\n123\n\npiyo\n</center>\nafter";
            List<IMfmNode> output = List.of(
                    new MfmText("before"),
                    new MfmCenter(new MfmText("abc\n123\n\npiyo")),
                    new MfmText("after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("nestable inline")
        void nestableInline() {
            String input = "<center>\nI'm @ai@example.com, An bot of misskey!</center>\n";
            List<IMfmNode> output = List.of(
                    new MfmCenter(
                            new MfmText("I'm "),
                            new MfmMention("ai", "example.com"),
                            new MfmText(", An bot of misskey!")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class EmojiCode {
        @Test
        @DisplayName("絵文字コードを使用できる")
        void basic() {
            String input = ":abc:";
            List<IMfmNode> output = List.of(
                    new MfmEmojiCode("abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("コロンの両端が半角英数だった場合は絵文字コードとして解釈されない")
        void beforeAfterAlphaAndNumeric() {
            String input = "a:abc:1";
            List<IMfmNode> output = List.of(
                    new MfmText("a:abc:1")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class UnicodeEmoji {
        @Test
        void basic() {
            String input = "今起きた😇";
            List<IMfmNode> output = List.of(
                    new MfmText("今起きた"),
                    new MfmUnicodeEmoji("😇")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("keycap number sign")
        void keycapNumberSign() {
            String input = "abc#️⃣123";
            List<IMfmNode> output = List.of(
                    new MfmText("abc"),
                    new MfmUnicodeEmoji("#️⃣"),
                    new MfmText("123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Big {
        @Test
        void basic() {
            String input = "***abc***";
            List<IMfmNode> output = List.of(
                    new MfmFn("tada", Map.of(), new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("内容にはインライン構文を利用できる")
        void usableInline() {
            String input = "***123**abc**123***";
            List<IMfmNode> output = List.of(
                    new MfmFn(
                            "tada", Map.of(), List.of(
                                    new MfmText("123"),
                                    new MfmBold(new MfmText("abc")),
                                    new MfmText("123")
                            )
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("内容は改行できる")
        void lineBreak() {
            String input = "***123\n**abc**\n123***";
            List<IMfmNode> output = List.of(
                    new MfmFn(
                            "tada", Map.of(), List.of(
                                    new MfmText("123\n"),
                                    new MfmBold(new MfmText("abc")),
                                    new MfmText("\n123")
                            )
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class BoldTag {
        @Test
        void basic() {
            String input = "<b>abc</b>";
            List<IMfmNode> output = List.of(
                    new MfmBold(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("inline syntax allowed inside")
        void inlineSyntaxAllowedInside() {
            String input = "<b>123~~abc~~456</b>";
            List<IMfmNode> output = List.of(
                    new MfmBold(
                            new MfmText("123"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("line breaks")
        void lineBreaks() {
            String input = "<b>123\n~~abc~~\n456</b>";
            List<IMfmNode> output = List.of(
                    new MfmBold(
                            new MfmText("123\n"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("\n456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Bold {
        @Test
        void basic() {
            String input = "**abc**";
            List<IMfmNode> output = List.of(
                    new MfmBold(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("inline syntax allowed inside")
        void inlineSyntaxAllowedInside() {
            String input = "**123~~abc~~456**";
            List<IMfmNode> output = List.of(
                    new MfmBold(
                            new MfmText("123"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("line breaks")
        void lineBreaks() {
            String input = "**123\n~~abc~~\n456**";
            List<IMfmNode> output = List.of(
                    new MfmBold(
                            new MfmText("123\n"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("\n456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class SmallTag {
        @Test
        void basic() {
            String input = "<small>abc</small>";
            List<IMfmNode> output = List.of(
                    new MfmSmall(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("inline syntax allowed inside")
        void inlineSyntaxAllowedInside() {
            String input = "<small>123~~abc~~456</small>";
            List<IMfmNode> output = List.of(
                    new MfmSmall(
                            new MfmText("123"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("line breaks")
        void lineBreaks() {
            String input = "<small>123\n~~abc~~\n456</small>";
            List<IMfmNode> output = List.of(
                    new MfmSmall(
                            new MfmText("123\n"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("\n456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class ItalicTag {
        @Test
        void basic() {
            String input = "<i>abc</i>";
            List<IMfmNode> output = List.of(
                    new MfmItalic(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("inline syntax allowed inside")
        void inlineSyntaxAllowedInside() {
            String input = "<i>123~~abc~~456</i>";
            List<IMfmNode> output = List.of(
                    new MfmItalic(
                            new MfmText("123"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("line breaks")
        void lineBreaks() {
            String input = "<i>123\n~~abc~~\n456</i>";
            List<IMfmNode> output = List.of(
                    new MfmItalic(
                            new MfmText("123\n"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("\n456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class ItalicAlt1 {
        @Test
        void basic() {
            String input = "*abc*";
            List<IMfmNode> output = List.of(
                    new MfmItalic(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basic2() {
            String input = "before *abc* after";
            List<IMfmNode> output = List.of(
                    new MfmText("before "),
                    new MfmItalic(new MfmText("abc")),
                    new MfmText(" after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore a italic syntax if the before char is neither a space nor an LF nor ^a-z0-9i")
        void ignoreItalicIfBeforeCharIsAlnum() {
            {
                String input = "before*abc*after";
                List<IMfmNode> output = List.of(
                        new MfmText("before*abc*after")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
            {
                String input = "あいう*abc*えお";
                List<IMfmNode> output = List.of(
                        new MfmText("あいう"),
                        new MfmItalic(new MfmText("abc")),
                        new MfmText("えお")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }
    }

    @Nested
    class ItalicAlt2 {
        @Test
        void basic() {
            String input = "_abc_";
            List<IMfmNode> output = List.of(
                    new MfmItalic(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basic2() {
            String input = "before _abc_ after";
            List<IMfmNode> output = List.of(
                    new MfmText("before "),
                    new MfmItalic(new MfmText("abc")),
                    new MfmText(" after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore a italic syntax if the before char is neither a space nor an LF nor ^a-z0-9i")
        void ignoreItalicIfBeforeCharIsAlnum() {
            {
                String input = "before_abc_after";
                List<IMfmNode> output = List.of(
                        new MfmText("before_abc_after")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
            {
                String input = "あいう_abc_えお";
                List<IMfmNode> output = List.of(
                        new MfmText("あいう"),
                        new MfmItalic(new MfmText("abc")),
                        new MfmText("えお")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }
    }

    @Nested
    class StrikeTag {
        @Test
        void basic() {
            String input = "<s>abc</s>";
            List<IMfmNode> output = List.of(
                    new MfmStrike(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("inline syntax allowed inside")
        void inlineSyntaxAllowedInside() {
            String input = "<s>123~~abc~~456</s>";
            List<IMfmNode> output = List.of(
                    new MfmStrike(
                            new MfmText("123"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("line breaks")
        void lineBreaks() {
            String input = "<s>123\n~~abc~~\n456</s>";
            List<IMfmNode> output = List.of(
                    new MfmStrike(
                            new MfmText("123\n"),
                            new MfmStrike(new MfmText("abc")),
                            new MfmText("\n456")
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class StrikeWave {
        @Test
        void basic() {
            String input = "~~abc~~";
            List<IMfmNode> output = List.of(
                    new MfmStrike(new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class InlineCode {
        @Test
        void basic() {
            String input = "`var x = \"Strawberry Pasta\";`";
            List<IMfmNode> output = List.of(
                    new MfmInlineCode("var x = \"Strawberry Pasta\";")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow line break")
        void disallowLineBreak() {
            String input = "`foo\nbar`";
            List<IMfmNode> output = List.of(
                    new MfmText("`foo\nbar`")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow ´")
        void disallowAccentChar() {
            String input = "`foo´bar`";
            List<IMfmNode> output = List.of(
                    new MfmText("`foo´bar`")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class MathInline {
        @Test
        void basic() {
            String input = "\\(x = {-b \\pm \\sqrt{b^2-4ac} \\over 2a}\\)";
            List<IMfmNode> output = List.of(
                    new MfmMathInline("x = {-b \\pm \\sqrt{b^2-4ac} \\over 2a}")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Mention {
        @Test
        void basic() {
            String input = "@abc";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc", null)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basic2() {
            String input = "before @abc after";
            List<IMfmNode> output = List.of(
                    new MfmText("before "),
                    new MfmMention("abc", null),
                    new MfmText(" after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basicRemote() {
            String input = "@abc@example.com";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc", "example.com")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basicRemote2() {
            String input = "before @abc@example.com after";
            List<IMfmNode> output = List.of(
                    new MfmText("before "),
                    new MfmMention("abc", "example.com"),
                    new MfmText(" after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basicRemote3() {
            String input = "before\n@abc@example.com\nafter";
            List<IMfmNode> output = List.of(
                    new MfmText("before\n"),
                    new MfmMention("abc", "example.com"),
                    new MfmText("\nafter")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore format of mail address")
        void ignoreFormatOfMailAddress() {
            String input = "abc@example.com";
            List<IMfmNode> output = List.of(
                    new MfmText("abc@example.com")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("detect as a mention if the before char is ^a-z0-9i")
        void detectAsMentionIfBeforeCharIsNotAlnum() {
            String input = "あいう@abc";
            List<IMfmNode> output = List.of(
                    new MfmText("あいう"),
                    new MfmMention("abc", null)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("invalid char only username")
        void invalidCharOnlyUsername() {
            String input = "@-";
            List<IMfmNode> output = List.of(
                    new MfmText("@-")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("invalid char only hostname")
        void invalidCharOnlyHostname() {
            String input = "@abc@.";
            List<IMfmNode> output = List.of(
                    new MfmText("@abc@.")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("allow \"-\" in username")
        void allowHyphenInUsername() {
            String input = "@abc-d";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc-d", null)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow \"-\" in head of username")
        void disallowHyphenInHeadOfUsername() {
            String input = "@-abc";
            List<IMfmNode> output = List.of(
                    new MfmText("@-abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow \"-\" in tail of username")
        void disallowHyphenInTailOfUsername() {
            String input = "@abc-";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc", null),
                    new MfmText("-")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow period in head of hostname")
        void disallowPeriodInHeadOfHostname() {
            String input = "@abc@.aaa";
            List<IMfmNode> output = List.of(
                    new MfmText("@abc@.aaa")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow period in tail of hostname")
        void disallowPeriodInTailOfHostname() {
            String input = "@abc@aaa.";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc", "aaa"),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow \"-\" in head of hostname")
        void disallowHyphenInHeadOfHostname() {
            String input = "@abc@-aaa";
            List<IMfmNode> output = List.of(
                    new MfmText("@abc@-aaa")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow \"-\" in tail of hostname")
        void disallowHyphenInTailOfHostname() {
            String input = "@abc@aaa-";
            List<IMfmNode> output = List.of(
                    new MfmMention("abc", "aaa"),
                    new MfmText("-")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Hashtag {
        @Test
        void basic() {
            String input = "#abc";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void basic2() {
            String input = "before #abc after";
            List<IMfmNode> output = List.of(
                    new MfmText("before "),
                    new MfmHashtag("abc"),
                    new MfmText(" after")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with keycap number sign")
        void withKeycapNumberSign() {
            String input = "#️⃣abc123 #abc";
            List<IMfmNode> output = List.of(
                    new MfmUnicodeEmoji("#️⃣"),
                    new MfmText("abc123 "),
                    new MfmHashtag("abc")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with keycap number sign 2")
        void withKeycapNumberSign2() {
            String input = "abc\n#️⃣123";
            List<IMfmNode> output = List.of(
                    new MfmText("abc\n"),
                    new MfmUnicodeEmoji("#️⃣"),
                    new MfmText("123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore a hashtag if the before char is neither a space nor an LF nor ^a-z0-9i")
        void ignoreHashtagIfBeforeCharIsAlnum() {
            {
                String input = "abc#abc";
                List<IMfmNode> output = List.of(
                        new MfmText("abc#abc")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
            {
                String input = "あいう#abc";
                List<IMfmNode> output = List.of(
                        new MfmText("あいう"),
                        new MfmHashtag("abc")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Test
        @DisplayName("ignore comma and period")
        void ignoreCommaAndPeriod() {
            String input = "Foo #bar, baz #piyo.";
            List<IMfmNode> output = List.of(
                    new MfmText("Foo "),
                    new MfmHashtag("bar"),
                    new MfmText(", baz "),
                    new MfmHashtag("piyo"),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore exclamation mark")
        void ignoreExclamationMark() {
            String input = "#Foo!";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("!")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore colon")
        void ignoreColon() {
            String input = "#Foo:";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText(":")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore single quote")
        void ignoreSingleQuote() {
            String input = "#Foo'";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("'")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore double quote")
        void ignoreDoubleQuote() {
            String input = "#Foo\"";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("\"")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore square bracket")
        void ignoreSquareBracket() {
            String input = "#Foo]";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("]")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore slash")
        void ignoreSlash() {
            String input = "#Foo/bar";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("/bar")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore angle bracket")
        void ignoreAngleBracket() {
            String input = "#Foo<bar>";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("Foo"),
                    new MfmText("<bar>")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("allow including number")
        void allowIncludingNumber() {
            String input = "#foo123";
            List<IMfmNode> output = List.of(
                    new MfmHashtag("foo123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets \"()\"")
        void withBracketsParens() {
            String input = "(#foo)";
            List<IMfmNode> output = List.of(
                    new MfmText("("),
                    new MfmHashtag("foo"),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets \"「」\"")
        void withBracketsJapanese() {
            String input = "「#foo」";
            List<IMfmNode> output = List.of(
                    new MfmText("「"),
                    new MfmHashtag("foo"),
                    new MfmText("」")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("mixed brackets")
        void mixedBrackets() {
            String input = "「#foo(bar)」";
            List<IMfmNode> output = List.of(
                    new MfmText("「"),
                    new MfmHashtag("foo(bar)"),
                    new MfmText("」")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets \"()\" (space before)")
        void withBracketsParensSpaceBefore() {
            String input = "(bar #foo)";
            List<IMfmNode> output = List.of(
                    new MfmText("(bar "),
                    new MfmHashtag("foo"),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets \"「」\" (space before)")
        void withBracketsJapaneseSpaceBefore() {
            String input = "「bar #foo」";
            List<IMfmNode> output = List.of(
                    new MfmText("「bar "),
                    new MfmHashtag("foo"),
                    new MfmText("」")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow number only")
        void disallowNumberOnly() {
            String input = "#123";
            List<IMfmNode> output = List.of(
                    new MfmText("#123")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow number only (with brackets)")
        void disallowNumberOnlyWithBrackets() {
            String input = "(#123)";
            List<IMfmNode> output = List.of(
                    new MfmText("(#123)")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Url {
        @Test
        void basic() {
            String input = "https://example.com/@ai";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/@ai", false)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with other texts")
        void withOtherTexts() {
            String input = "official instance: https://example.com/@ai";
            List<IMfmNode> output = List.of(
                    new MfmText("official instance: "),
                    new MfmUrl("https://example.com/@ai", false)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore trailing period")
        void ignoreTrailingPeriod() {
            String input = "https://example.com/@ai.";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/@ai", false),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("disallow period only")
        void disallowPeriodOnly() {
            String input = "https://.";
            List<IMfmNode> output = List.of(
                    new MfmText("https://.")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore trailing periods")
        void ignoreTrailingPeriods() {
            String input = "https://example.com/@ai...";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/@ai", false),
                    new MfmText("...")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with comma")
        void withComma() {
            String input = "https://example.com/foo?bar=a,b";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/foo?bar=a,b", false)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore trailing comma")
        void ignoreTrailingComma() {
            String input = "https://example.com/foo, bar";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/foo", false),
                    new MfmText(", bar")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets")
        void withBrackets() {
            String input = "https://example.com/foo(bar)";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://example.com/foo(bar)", false)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore parent brackets")
        void ignoreParentBrackets() {
            String input = "(https://example.com/foo)";
            List<IMfmNode> output = List.of(
                    new MfmText("("),
                    new MfmUrl("https://example.com/foo", false),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore parent brackets (2)")
        void ignoreParentBrackets2() {
            String input = "(foo https://example.com/foo)";
            List<IMfmNode> output = List.of(
                    new MfmText("(foo "),
                    new MfmUrl("https://example.com/foo", false),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore parent brackets with internal brackets")
        void ignoreParentBracketsWithInternalBrackets() {
            String input = "(https://example.com/foo(bar))";
            List<IMfmNode> output = List.of(
                    new MfmText("("),
                    new MfmUrl("https://example.com/foo(bar)", false),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore parent square brackets")
        void ignoreParentSquareBrackets() {
            String input = "foo [https://example.com/foo] bar";
            List<IMfmNode> output = List.of(
                    new MfmText("foo ["),
                    new MfmUrl("https://example.com/foo", false),
                    new MfmText("] bar")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("ignore non-ascii characters contained url without angle brackets")
        void ignoreNonAsciiUrlWithoutAngleBrackets() {
            String input = "https://本日は晴天なり.example.com";
            List<IMfmNode> output = List.of(
                    new MfmText("https://本日は晴天なり.example.com")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("match non-ascii characters contained url with angle brackets")
        void matchNonAsciiUrlWithAngleBrackets() {
            String input = "<https://本日は晴天なり.example.com>";
            List<IMfmNode> output = List.of(
                    new MfmUrl("https://本日は晴天なり.example.com", true)
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("prevent xss")
        void preventXss() {
            String input = "javascript:foo";
            List<IMfmNode> output = List.of(
                    new MfmText("javascript:foo")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Link {
        @Test
        void basic() {
            String input = "[official instance](https://example.com/@ai).";
            List<IMfmNode> output = List.of(
                    new MfmLink(false, "https://example.com/@ai", new MfmText("official instance")),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("silent flag")
        void silentFlag() {
            String input = "?[official instance](https://example.com/@ai).";
            List<IMfmNode> output = List.of(
                    new MfmLink(true, "https://example.com/@ai", new MfmText("official instance")),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with angle brackets url")
        void withAngleBracketsUrl() {
            String input = "[official instance](<https://example.com/@ai>).";
            List<IMfmNode> output = List.of(
                    new MfmLink(false, "https://example.com/@ai", new MfmText("official instance")),
                    new MfmText(".")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("prevent xss")
        void preventXss() {
            String input = "[click here](javascript:foo)";
            List<IMfmNode> output = List.of(
                    new MfmText("[click here](javascript:foo)")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Nested
        @DisplayName("cannot nest a url in a link label")
        class Nested1 {
            @Test
            void basic() {
                String input = "official instance: [https://example.com/@ai](https://example.com/@ue).";
                List<IMfmNode> output = List.of(
                        new MfmText("official instance: "),
                        new MfmLink(false, "https://example.com/@ue", new MfmText("https://example.com/@ai")),
                        new MfmText(".")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            void nested() {
                String input = "official instance: [https://example.com/@ai**https://example.com/@ue**](https://example.com/@o).";
                List<IMfmNode> output = List.of(
                        new MfmText("official instance: "),
                        new MfmLink(
                                false,
                                "https://example.com/@o",
                                List.of(
                                        new MfmText("https://example.com/@ai"),
                                        new MfmBold(new MfmText("https://example.com/@ue"))
                                )
                        ),
                        new MfmText(".")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Nested
        @DisplayName("cannot nest a link in a link label")
        class Nested2 {
            @Test
            void basic() {
                String input = "official instance: [[https://example.com/@ai](https://example.com/@ue)](https://example.com/@o).";
                List<IMfmNode> output = List.of(
                        new MfmText("official instance: "),
                        new MfmLink(false, "https://example.com/@ue", new MfmText("[https://example.com/@ai")),
                        new MfmText("]("),
                        new MfmUrl("https://example.com/@o", false),
                        new MfmText(").")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            void nested() {
                String input =
                        "official instance: [**[https://example.com/@ai](https://example.com/@ue)**](https://example.com/@o).";
                List<IMfmNode> output = List.of(
                        new MfmText("official instance: "),
                        new MfmLink(
                                false,
                                "https://example.com/@o",
                                new MfmBold(new MfmText("[https://example.com/@ai](https://example.com/@ue)"))
                        ),
                        new MfmText(".")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Nested
        @DisplayName("cannot nest a mention in a link label")
        class Nested3 {
            @Test
            void basic() {
                String input = "[@example](https://example.com)";
                List<IMfmNode> output = List.of(
                        new MfmLink(false, "https://example.com", new MfmText("@example"))
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            void nested() {
                String input = "[@example**@example**](https://example.com)";
                List<IMfmNode> output = List.of(
                        new MfmLink(
                                false,
                                "https://example.com",
                                List.of(
                                        new MfmText("@example"),
                                        new MfmBold(new MfmText("@example"))
                                )
                        )
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Test
        @DisplayName("with brackets")
        void withBrackets() {
            String input = "[foo](https://example.com/foo(bar))";
            List<IMfmNode> output = List.of(
                    new MfmLink(false, "https://example.com/foo(bar)", new MfmText("foo"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with parent brackets")
        void withParentBrackets() {
            String input = "([foo](https://example.com/foo(bar)))";
            List<IMfmNode> output = List.of(
                    new MfmText("("),
                    new MfmLink(false, "https://example.com/foo(bar)", new MfmText("foo")),
                    new MfmText(")")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with brackets before")
        void withBracketsBefore() {
            String input = "[test] foo [bar](https://example.com)";
            List<IMfmNode> output = List.of(
                    new MfmText("[test] foo "),
                    new MfmLink(false, "https://example.com", new MfmText("bar"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class Fn {
        @Test
        void basic() {
            String input = "$[tada abc]";
            List<IMfmNode> output = List.of(
                    new MfmFn("tada", Map.of(), new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with a string argument")
        void withAStringArgument() {
            String input = "$[spin.speed=1.1s a]";
            List<IMfmNode> output = List.of(
                    new MfmFn("spin", Map.of("speed", "1.1s"), new MfmText("a"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with a string argument 2")
        void withAStringArgument2() {
            String input = "$[position.x=-3 a]";
            List<IMfmNode> output = List.of(
                    new MfmFn("position", Map.of("x", "-3"), new MfmText("a"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("with a string argument 3")
        void withAStringArgument3() {
            String input = "$[position.x=-3,y=4 abc]";
            List<IMfmNode> output = List.of(
                    new MfmFn("position", Map.of("x", "-3", "y", "4"), new MfmText("abc"))
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        @DisplayName("invalid fn name")
        void invalidFnName() {
            String input = "$[関数 text]";
            List<IMfmNode> output = List.of(
                    new MfmText("$[関数 text]")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void nest() {
            String input = "$[spin.speed=1.1s $[shake a]]";
            List<IMfmNode> output = List.of(
                    new MfmFn(
                            "spin",
                            Map.of("speed", "1.1s"),
                            new MfmFn("shake", Map.of(), new MfmText("a"))
                    )
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class PlainTag {
        @Test
        void multipleLine() {
            String input = "a\n<plain>\n**Hello**\nworld\n</plain>\nb";
            List<IMfmNode> output = List.of(
                    new MfmText("a\n"),
                    new MfmPlain("**Hello**\nworld"),
                    new MfmText("\nb")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }

        @Test
        void singleLine() {
            String input = "a\n<plain>**Hello** world</plain>\nb";
            List<IMfmNode> output = List.of(
                    new MfmText("a\n"),
                    new MfmPlain("**Hello** world"),
                    new MfmText("\nb")
            );
            assertMfmNodeEquals(output, Mfm.parse(input));
        }
    }

    @Nested
    class NestingLimit {
        @Nested
        class Quote {
            @Test
            void basic() {
                String input = ">>> abc";
                List<IMfmNode> output = List.of(
                        new MfmQuote(new MfmQuote(new MfmText("> abc")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }

            @Test
            void basic2() {
                String input = ">> **abc**";
                List<IMfmNode> output = List.of(
                        new MfmQuote(new MfmQuote(new MfmText("**abc**")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Big {
            @Test
            void basic() {
                String input = "<b><b>***abc***</b></b>";
                List<IMfmNode> output = List.of(
                        new MfmBold(new MfmBold(new MfmText("***abc***")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Bold {
            @Test
            void basic() {
                String input = "<i><i>**abc**</i></i>";
                List<IMfmNode> output = List.of(
                        new MfmItalic(new MfmItalic(new MfmText("**abc**")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }

            @Test
            void tag() {
                String input = "<i><i><b>abc</b></i></i>";
                List<IMfmNode> output = List.of(
                        new MfmItalic(new MfmItalic(new MfmText("<b>abc</b>")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Small {
            @Test
            void basic() {
                String input = "<i><i><small>abc</small></i></i>";
                List<IMfmNode> output = List.of(
                        new MfmItalic(new MfmItalic(new MfmText("<small>abc</small>")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Italic {
            @Test
            void basic() {
                String input = "<b><b><i>abc</i></b></b>";
                List<IMfmNode> output = List.of(
                        new MfmBold(new MfmBold(new MfmText("<i>abc</i>")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Strike {
            @Test
            void basic() {
                String input = "<b><b>~~abc~~</b></b>";
                List<IMfmNode> output = List.of(
                        new MfmBold(new MfmBold(new MfmText("~~abc~~")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }

            @Test
            void tag() {
                String input = "<b><b><s>abc</s></b></b>";
                List<IMfmNode> output = List.of(
                        new MfmBold(new MfmBold(new MfmText("<s>abc</s>")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }

        @Nested
        class Hashtag {
            @Test
            @DisplayName("outside ()")
            void outsideParens() {
                String input = "(#abc)";
                List<IMfmNode> output = List.of(
                        new MfmText("("),
                        new MfmHashtag("abc"),
                        new MfmText(")")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("outside square brackets")
            void outsideSquareBrackets() {
                String input = "[#abc]";
                List<IMfmNode> output = List.of(
                        new MfmText("["),
                        new MfmHashtag("abc"),
                        new MfmText("]")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("outside 「」")
            void outsideJapaneseBrackets() {
                String input = "「#abc」";
                List<IMfmNode> output = List.of(
                        new MfmText("「"),
                        new MfmHashtag("abc"),
                        new MfmText("」")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }

            @Test
            @DisplayName("outside （）")
            void outsideFullWidthParens() {
                String input = "（#abc）";
                List<IMfmNode> output = List.of(
                        new MfmText("（"),
                        new MfmHashtag("abc"),
                        new MfmText("）")
                );
                assertMfmNodeEquals(output, Mfm.parse(input));
            }
        }

        @Nested
        class Fn {
            @Test
            void basic() {
                String input = "<b><b>$[a b]</b></b>";
                List<IMfmNode> output = List.of(
                        new MfmBold(new MfmBold(new MfmText("$[a b]")))
                );
                assertMfmNodeEquals(output, Mfm.parse(input, 2));
            }
        }
    }
}
