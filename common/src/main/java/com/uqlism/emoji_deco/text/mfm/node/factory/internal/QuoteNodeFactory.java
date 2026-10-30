package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.MfmQuote;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.FullParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/QuoteNodeFactory.kt}
 *
 * <p>Quoting allows arbitrary nested block/inline syntax, not just plain text, so this re-parses
 * the quoted lines from scratch with a fresh {@link FullParser}.
 */
public final class QuoteNodeFactory extends SimpleNodeFactoryBase<MfmQuote> {
  public static final QuoteNodeFactory INSTANCE = new QuoteNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Quote);

  private QuoteNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmQuote> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    List<String> contentLines = foundInfo.nestedInfos().stream()
        .map(info -> NodeFactory.substring(input, info.contentRange()))
        .collect(Collectors.toList());
    if (contentLines.isEmpty() || (contentLines.size() == 1 && contentLines.get(0).isEmpty())) {
      // disallow empty content if single line
      return IFactoryResult.failure();
    }

    // parse inner content
    IMfmParserContext finderContext = new IMfmParserContext.Impl();
    String contentText = String.join("\n", contentLines);
    IMfmParserResult contentFindResult = new FullParser().find(contentText, 0, finderContext);
    if (!contentFindResult.success()) {
      return IFactoryResult.failure();
    }

    List<IMfmNode> result = NodeFactory.createNodes(
        contentText,
        contentFindResult.foundInfo().nestedInfos(),
        context,
        MfmNodeAttribute.SET_OF_ALL
    );
    if (result.isEmpty()) {
      return IFactoryResult.failure();
    }

    return IFactoryResult.success(new MfmQuote(result), foundInfo);
  }
}
