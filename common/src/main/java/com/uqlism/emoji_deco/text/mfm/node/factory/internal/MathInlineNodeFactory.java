package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmMathInline;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/MathInlineNodeFactory.kt}
 */
public final class MathInlineNodeFactory extends SimpleNodeFactoryBase<MfmMathInline> {
  public static final MathInlineNodeFactory INSTANCE = new MathInlineNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.MathInline);

  private MathInlineNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmMathInline> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    return IFactoryResult.success(new MfmMathInline(NodeFactory.substring(input, foundInfo.contentRange())), foundInfo);
  }
}
