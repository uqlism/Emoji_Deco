package com.uqlism.emoji_deco.text.mfm.node.factory;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmNest;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.BigNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.BoldAstaNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.BoldTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.BoldUnderNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.CenterTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.CodeBlockNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.EmojiCodeNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.FnNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.HashtagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.IFactoryResult;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.INodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.INodeFactoryContext;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.InlineCodeNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.ItalicAstaNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.ItalicTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.ItalicUnderNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.LinkNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.MathBlockNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.MathInlineNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.MentionNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.PlainTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.QuoteNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.SearchNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.SmallTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.StrikeTagNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.StrikeWaveNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.TextNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.UnicodeEmojiNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.UrlAltNodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.UrlNodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/NodeFactory.kt}
 *
 * <p><b>The nest-level guard in {@link #createNodesImpl} is a deliberate DoS mitigation and must
 * not be removed or weakened</b>: it is what stops adversarially-nested chat input (e.g. thousands
 * of nested {@code **}) from recursing without bound. Once {@code context.nestLevel()} exceeds
 * {@code context.maximumNestLevel()}, the remaining input is emitted verbatim as a single
 * {@link MfmText} instead of being parsed further.
 */
public final class NodeFactory {
  private NodeFactory() {
  }

  public static List<IMfmNode> createNodes(String input, List<SubstringFoundInfo> foundInfos, INodeFactoryContext context) {
    return createNodes(input, foundInfos, context, MfmNodeAttribute.SET_OF_ALL);
  }

  public static List<IMfmNode> createNodes(
      String input,
      List<SubstringFoundInfo> foundInfos,
      INodeFactoryContext context,
      Set<MfmNodeAttribute> allowNodeAttribute
  ) {
    List<IMfmNode> result = new ArrayList<>();
    for (SubstringFoundInfo foundInfo : foundInfos) {
      IFactoryResult<IMfmNode> r = createNodes(input, foundInfo, context, allowNodeAttribute);
      if (r.success()) {
        result.add(r.node());
      }
    }
    return result;
  }

  private static IFactoryResult<IMfmNode> createNodes(
      String input,
      SubstringFoundInfo foundInfo,
      INodeFactoryContext context,
      Set<MfmNodeAttribute> allowNodeAttribute
  ) {
    return deepenNest(context, () -> createNodesImpl(input, foundInfo, context, allowNodeAttribute));
  }

  private static IFactoryResult<IMfmNode> createNodesImpl(
      String input,
      SubstringFoundInfo foundInfo,
      INodeFactoryContext context,
      Set<MfmNodeAttribute> allowNodeAttribute
  ) {
    // ネストが上限に達している場合は入力をそのままMfmTextとして扱い、これ以上ネストさせない。
    // (信頼できないチャット入力に対するDoS対策。絶対に維持すること — クラスjavadoc参照)
    if (context.nestLevel() > context.maximumNestLevel()) {
      return IFactoryResult.success(new MfmText(substring(input, foundInfo.overallRange())), foundInfo);
    }

    FoundType foundType = foundInfo.type();
    if (foundType == FoundType.Simple || foundType == FoundType.Inline || foundType == FoundType.Full) {
      List<IFactoryResult<IMfmNode>> results = new ArrayList<>();
      for (SubstringFoundInfo nested : foundInfo.nestedInfos()) {
        results.add(createNodes(input, nested, context, allowNodeAttribute));
      }
      if (results.stream().anyMatch(r -> !r.success())) {
        return IFactoryResult.failure();
      }

      List<IMfmNode> children = results.stream().map(IFactoryResult::node).collect(Collectors.toList());
      return IFactoryResult.success(new MfmNest(children), foundInfo);
    }

    INodeFactory<? extends IMfmNode> factory = mappingToFactory(foundType);
    IFactoryResult<? extends IMfmNode> result = factory.create(input, foundInfo, context);
    if (!result.success()) {
      return IFactoryResult.failure();
    }

    IMfmNode node = result.node();
    boolean allowed = allowNodeAttribute.stream().anyMatch(attribute -> node.type().attributes().contains(attribute));
    if (!allowed) {
      return IFactoryResult.failure();
    }

    return IFactoryResult.success(node, foundInfo);
  }

  private static INodeFactory<? extends IMfmNode> mappingToFactory(FoundType foundType) {
    return switch (foundType) {
      case Big -> BigNodeFactory.INSTANCE;
      case BoldAsta -> BoldAstaNodeFactory.INSTANCE;
      case BoldTag -> BoldTagNodeFactory.INSTANCE;
      case BoldUnder -> BoldUnderNodeFactory.INSTANCE;
      case CenterTag -> CenterTagNodeFactory.INSTANCE;
      case CodeBlock -> CodeBlockNodeFactory.INSTANCE;
      case EmojiCode -> EmojiCodeNodeFactory.INSTANCE;
      case Fn -> FnNodeFactory.INSTANCE;
      case Hashtag -> HashtagNodeFactory.INSTANCE;
      case InlineCode -> InlineCodeNodeFactory.INSTANCE;
      case ItalicAsta -> ItalicAstaNodeFactory.INSTANCE;
      case ItalicTag -> ItalicTagNodeFactory.INSTANCE;
      case ItalicUnder -> ItalicUnderNodeFactory.INSTANCE;
      case Link -> LinkNodeFactory.INSTANCE;
      case MathBlock -> MathBlockNodeFactory.INSTANCE;
      case MathInline -> MathInlineNodeFactory.INSTANCE;
      case Mention -> MentionNodeFactory.INSTANCE;
      case PlainTag -> PlainTagNodeFactory.INSTANCE;
      case Quote -> QuoteNodeFactory.INSTANCE;
      case Search -> SearchNodeFactory.INSTANCE;
      case SmallTag -> SmallTagNodeFactory.INSTANCE;
      case StrikeTag -> StrikeTagNodeFactory.INSTANCE;
      case StrikeWave -> StrikeWaveNodeFactory.INSTANCE;
      case UnicodeEmoji -> UnicodeEmojiNodeFactory.INSTANCE;
      case Url -> UrlNodeFactory.INSTANCE;
      case UrlAlt -> UrlAltNodeFactory.INSTANCE;
      case Text -> TextNodeFactory.INSTANCE;
      default -> throw new IllegalArgumentException("Not Support FoundType: " + foundType);
    };
  }

  private static <T> T deepenNest(INodeFactoryContext context, java.util.function.Supplier<T> block) {
    context.setNestLevel(context.nestLevel() + 1);
    T result = block.get();
    context.setNestLevel(context.nestLevel() - 1);
    return result;
  }

  /**
   * Equivalent to Kotlin's {@code input.substring(range: IntRange)}, i.e.
   * {@code input.substring(range.start, range.last + 1)}. Since this codebase's ported
   * {@link IntRange} already stores {@code endExclusive == last + 1} (see its javadoc), this is
   * just {@code input.substring(range.start(), range.endExclusive())}.
   */
  public static String substring(String input, IntRange range) {
    return input.substring(range.start(), range.endExclusive());
  }
}
