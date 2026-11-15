package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmInlineCode;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/InlineCodeNodeFactory.kt}
 */
public final class InlineCodeNodeFactory extends SimpleNodeFactoryBase<MfmInlineCode> {
  public static final InlineCodeNodeFactory INSTANCE = new InlineCodeNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.InlineCode);

  private InlineCodeNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmInlineCode> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    return IFactoryResult.success(new MfmInlineCode(NodeFactory.substring(input, foundInfo.contentRange())), foundInfo);
  }
}
