package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.InlineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.AlternateParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/LinkParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "link" syntax. Strings that match the
 * pattern {@code [linkText](https://example.com/)} will be the search results.
 *
 * <p>Notes: applies {@link InlineParser} to the content again to recursively detect inline
 * syntax; URLs, links, and mentions are not allowed in the display text (the part enclosed in
 * "[]"); any character and newline can be used in the content; the content cannot be left empty.
 */
public final class LinkParser implements IMfmParser {
  public static final LinkParser INSTANCE = new LinkParser();

  private static final RegexParser SQUARE_OPEN = new RegexParser(Pattern.compile("\\??\\["));
  private static final StringParser SQUARE_CLOSE = new StringParser("]");
  private static final StringParser ROUND_OPEN = new StringParser("(");
  private static final StringParser ROUND_CLOSE = new StringParser(")");
  private static final SequentialParser LINK_PARSER = new SequentialParser(
      SQUARE_OPEN,
      new InlineParser(new AlternateParser(SQUARE_CLOSE, NewLineParser.INSTANCE)),
      SQUARE_CLOSE,
      ROUND_OPEN,
      new AlternateParser(UrlAltParser.INSTANCE, UrlParser.INSTANCE),
      ROUND_CLOSE
  );
  private static final Set<Class<? extends IMfmParser>> EXCLUDE_PARSER_CLASSES = Set.of(
      HashtagParser.class,
      LinkParser.class,
      MentionParser.class,
      UrlParser.class,
      UrlAltParser.class
  );

  private LinkParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    // リンク書式のラベル内はハッシュタグ、メンション、リンク、URL、URL(<>付き)を検索しない（テキストとして認識する）
    context.setExcludeParsers(EXCLUDE_PARSER_CLASSES);
    IMfmParserResult result = LINK_PARSER.find(input, startAt, context);
    context.setExcludeParsers(Set.of());

    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo squareOpen = result.foundInfo().get(0);
    SubstringFoundInfo label = result.foundInfo().get(1);
    SubstringFoundInfo url = result.foundInfo().get(4);

    return success(
        FoundType.Link,
        result.foundInfo().overallRange(),
        result.foundInfo().contentRange(),
        result.foundInfo().overallRange().next(),
        List.of(squareOpen, label, url)
    );
  }

  public enum SubIndex implements ISubIndex {
    SquareOpen(0),
    Label(1),
    Url(2);

    private final int index;

    SubIndex(int index) {
      this.index = index;
    }

    @Override
    public int index() {
      return index;
    }
  }
}
