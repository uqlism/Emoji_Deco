package com.uqlism.emoji_deco.text.mfm.parser;

import com.uqlism.emoji_deco.text.mfm.parser.block.CenterTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.block.CodeBlockParser;
import com.uqlism.emoji_deco.text.mfm.parser.block.MathBlockParser;
import com.uqlism.emoji_deco.text.mfm.parser.block.QuoteParser;
import com.uqlism.emoji_deco.text.mfm.parser.block.SearchParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.CharSequenceTerminateParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.BigParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.BoldAstaParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.BoldTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.BoldUnderParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.EmojiCodeParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.FnParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.HashtagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.InlineCodeParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.ItalicAstaParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.ItalicTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.ItalicUnderParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.LinkParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.MathInlineParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.MentionParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.PlainTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.SmallTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.StrikeTagParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.StrikeWaveParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.UnicodeEmojiParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.UrlAltParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.UrlParser;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/FullParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting all syntax.
 */
public final class FullParser extends RecursiveParserBase {
  private static final List<IMfmParser> PARSERS = List.of(
      UnicodeEmojiParser.INSTANCE,
      CenterTagParser.INSTANCE,
      SmallTagParser.INSTANCE,
      PlainTagParser.INSTANCE,
      BoldTagParser.INSTANCE,
      ItalicTagParser.INSTANCE,
      StrikeTagParser.INSTANCE,
      UrlAltParser.INSTANCE,
      BigParser.INSTANCE,
      BoldAstaParser.INSTANCE,
      ItalicAstaParser.INSTANCE,
      BoldUnderParser.INSTANCE,
      ItalicUnderParser.INSTANCE,
      CodeBlockParser.INSTANCE,
      InlineCodeParser.INSTANCE,
      QuoteParser.INSTANCE,
      MathBlockParser.INSTANCE,
      MathInlineParser.INSTANCE,
      StrikeWaveParser.INSTANCE,
      FnParser.INSTANCE,
      MentionParser.INSTANCE,
      HashtagParser.INSTANCE,
      EmojiCodeParser.INSTANCE,
      LinkParser.INSTANCE,
      UrlParser.INSTANCE,
      SearchParser.INSTANCE
  );

  public FullParser() {
    this(CharSequenceTerminateParser.INSTANCE);
  }

  public FullParser(IMfmParser terminateParser) {
    super(terminateParser);
  }

  @Override
  protected List<IMfmParser> parsers() {
    return PARSERS;
  }

  @Override
  protected FoundType foundType() {
    return FoundType.Full;
  }
}
