package com.uqlism.emoji_deco;

import com.uqlism.emoji_deco.text.parse.RichTextParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RichTextParserTest extends MinecraftTestBase {

    // ── helper ────────────────────────────────────────────────────────────────

    /** Parses the input and returns the plain-text string of the result. */
    private static String str(String input) {
        return RichTextParser.parse(input).toComponent().getString();
    }

    // ── literal text ──────────────────────────────────────────────────────────

    @Test void plainText()    { assertEquals("hello world", str("hello world")); }
    @Test void emptyString()  { assertEquals("",            str(""));            }

    // ── unregistered tokens (pass-through) ────────────────────────────────────

    @Test void unknownDecorator() { assertEquals("#unknown[text]", str("#unknown[text]")); }
    @Test void unknownShortcode() { assertEquals(":unknown:",      str(":unknown:"));      }

    // ── escape sequences ─────────────────────────────────────────────────────

    @Test void escapeHash()     { assertEquals("#bold[text]", str("\\#bold[text]")); }
    @Test void escapeColon()    { assertEquals(":heart:",     str("\\:heart\\:"));   }
    @Test void escapeBackslash(){ assertEquals("\\",          str("\\\\"));          }
    @Test void escapeBracketClose() { assertEquals("a]b",    str("a\\]b"));          }
    @Test void escapeBracketOpen()  { assertEquals("a[b",    str("a\\[b"));          }
    @Test void escapeDot()      { assertEquals("hello.world", str("hello\\.world")); }
    @Test void escapeComma()    { assertEquals("a,b",         str("a\\,b"));         }

    @Test void escapeInMixedText() {
        assertEquals("cost: #5", str("cost: \\#5"));
    }

    @Test void nonEscapableBackslash() {
        // \n は対象外 → バックスラッシュごとそのまま
        assertEquals("\\n", str("\\n"));
    }

    @Test void escapedHashDoesNotTriggerDecorator() {
        // 通常 #bold は登録デコレーターだが、\ でエスケープされたら素通り
        assertEquals("#bold[hello]", str("\\#bold[hello]"));
    }

    @Test void closingBracketInsideDecorator() {
        // \] がデコレーターを早期終了させないことを確認
        // #unknown は未登録なのでそのまま通過するが、内部で \] が ] になる
        assertEquals("#unknown[a]b]", str("#unknown[a\\]b]"));
    }
}
