package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/MentionParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "mention" syntax. The string starting
 * with "@" will be the search result.
 *
 * <p>Notes: if the first character before the @ matches {@code [a-z0-9]} (case-insensitive) it
 * will not be recognized as a mention; username must be at least 1 letter;
 * {@code [a-z0-9_-]} (case-insensitive) can be used for the username; username cannot start or
 * end with "-"; host name must be at least 1 character; {@code [a-z0-9_-.]} (not case sensitive)
 * can be used in the hostname; the host name cannot start or end with '-' or '.'.
 *
 * <p>This class directly drives {@link java.util.regex.Matcher} rather than going through {@link
 * com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser}, mirroring how the Kotlin source calls
 * {@code Regex.find}/{@code MatchResult.groups} directly instead of using its own
 * {@code RegexParser} wrapper for this parser.
 */
public final class MentionParser implements IMfmParser {
  public static final MentionParser INSTANCE = new MentionParser();

  private static final Pattern REGEX_MENTION =
      Pattern.compile("^@(?<username>[a-zA-Z0-9_-]+)(@(?<hostname>[a-z0-9_.-]+))?");
  private static final Pattern REGEX_ALPHA_NUMERIC_TAIL = Pattern.compile("[0-9a-z]", Pattern.CASE_INSENSITIVE);
  private static final Pattern REGEX_DOT_HYPHEN_TAIL = Pattern.compile("[.-]+$");
  private static final Pattern REGEX_DOT_HYPHEN_HEAD = Pattern.compile("^[.-]");
  private static final Pattern REGEX_HYPHEN_TAIL = Pattern.compile("-+$");

  private MentionParser() {
  }

  private static IMfmParserResult findUsernameRange(String input, IntRange usernameRange) {
    String username = input.substring(usernameRange.start(), usernameRange.endExclusive());

    // 先頭がハイフンで始まることを許容しない
    if (username.charAt(0) == '-') {
      return failure();
    }

    // 末尾がハイフンで終わっているかを調べる(ハイフンがない場合は除外の必要なしなのでそのまま返す)
    Matcher hyphenTailMatch = REGEX_HYPHEN_TAIL.matcher(username);
    if (!hyphenTailMatch.find()) {
      return success(FoundType.Mention, usernameRange, usernameRange, usernameRange.next());
    }

    // ハイフンしか無い場合はユーザー名として認識しない
    String modifiedUsername = username.substring(0, hyphenTailMatch.start());
    if (modifiedUsername.isEmpty()) {
      return failure();
    }

    IntRange modifiedUsernameRange = new IntRange(usernameRange.start(), usernameRange.start() + modifiedUsername.length());
    return success(FoundType.Mention, modifiedUsernameRange, modifiedUsernameRange, modifiedUsernameRange.next());
  }

  private static IMfmParserResult findHostnameRange(String input, IntRange hostnameRange) {
    String hostname = input.substring(hostnameRange.start(), hostnameRange.endExclusive());

    // ドットまたはハイフンで始まっている場合は、ホスト名として認識しない
    if (REGEX_DOT_HYPHEN_HEAD.matcher(hostname).find()) {
      return failure();
    }

    // ドットまたはハイフンで終わっているかを調べる(ドットまたはハイフンがない場合は除外の必要なしなのでそのまま返す)
    Matcher dotHyphenTailMatch = REGEX_DOT_HYPHEN_TAIL.matcher(hostname);
    if (!dotHyphenTailMatch.find()) {
      return success(FoundType.Mention, hostnameRange, hostnameRange, hostnameRange.next());
    }

    // ドットまたはハイフンしか無い場合はホスト名として認識しない
    String modifiedHostname = hostname.substring(0, dotHyphenTailMatch.start());
    if (modifiedHostname.isEmpty()) {
      return failure();
    }

    IntRange modifiedHostnameRange = new IntRange(hostnameRange.start(), hostnameRange.start() + modifiedHostname.length());
    return success(FoundType.Mention, modifiedHostnameRange, modifiedHostnameRange, modifiedHostnameRange.next());
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    Matcher mentionMatch = REGEX_MENTION.matcher(input.substring(startAt));
    if (!mentionMatch.find()) {
      return failure();
    }

    // メンションの直前が英数字だった場合、メンションとして認識しない
    if (startAt >= 1) {
      String beforeChar = input.substring(startAt - 1, startAt);
      if (REGEX_ALPHA_NUMERIC_TAIL.matcher(beforeChar).find()) {
        return failure();
      }
    }

    int usernameStart = mentionMatch.start("username");
    if (usernameStart < 0) {
      return failure();
    }
    IntRange usernameRange = new IntRange(usernameStart + startAt, mentionMatch.end("username") + startAt);

    int hostnameStart = mentionMatch.start("hostname");
    boolean hasHostname = hostnameStart >= 0;
    IntRange hostnameRange = hasHostname
        ? new IntRange(hostnameStart + startAt, mentionMatch.end("hostname") + startAt)
        : IntRange.EMPTY;

    if (hasHostname) {
      IMfmParserResult hostnameResult = findHostnameRange(input, hostnameRange);
      if (!hostnameResult.success()) {
        return failure();
      }

      IMfmParserResult usernameResult = findUsernameRange(input, usernameRange);
      if (!usernameResult.success() || !usernameResult.foundInfo().contentRange().equals(usernameRange)) {
        // 範囲が変わっている＝ハイフンのぶんを切り詰められたということ
        // ホスト名が存在するときは、ユーザ名の後ろのハイフンを切り捨てられない。
        // （切り捨ててTextとして認識させたいが、ホスト名がある時にそれをやるとホスト名までText扱いになってしまうので）
        return failure();
      }

      IntRange overallRange = new IntRange(startAt, hostnameResult.foundInfo().contentRange().last() + 1);
      IntRange contentRange = new IntRange(usernameResult.foundInfo().contentRange().start(), hostnameResult.foundInfo().contentRange().last() + 1);
      return success(
          FoundType.Mention,
          overallRange,
          contentRange,
          overallRange.next(),
          List.of(
              new SubstringFoundInfo(
                  FoundType.Mention,
                  usernameResult.foundInfo().overallRange(),
                  usernameResult.foundInfo().contentRange(),
                  usernameResult.foundInfo().contentRange().next()
              ),
              new SubstringFoundInfo(
                  FoundType.Mention,
                  hostnameResult.foundInfo().overallRange(),
                  hostnameResult.foundInfo().contentRange(),
                  hostnameResult.foundInfo().contentRange().next()
              )
          )
      );
    } else {
      IMfmParserResult usernameResult = findUsernameRange(input, usernameRange);
      if (!usernameResult.success()) {
        return failure();
      }

      IntRange overallRange = new IntRange(startAt, usernameResult.foundInfo().contentRange().last() + 1);
      IntRange contentRange = usernameResult.foundInfo().contentRange();
      return success(
          FoundType.Mention,
          overallRange,
          contentRange,
          overallRange.next(),
          List.of(
              new SubstringFoundInfo(FoundType.Mention, contentRange, contentRange, contentRange.next()),
              SubstringFoundInfo.EMPTY
          )
      );
    }
  }

  public enum SubIndex implements ISubIndex {
    Username(0),
    Hostname(1);

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
