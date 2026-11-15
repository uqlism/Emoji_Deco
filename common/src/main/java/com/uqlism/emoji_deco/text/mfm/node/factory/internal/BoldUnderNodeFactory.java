package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmBold;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/BoldUnderNodeFactory.kt}
 */
public final class BoldUnderNodeFactory extends SimpleNodeFactoryBase<MfmBold> {
  public static final BoldUnderNodeFactory INSTANCE = new BoldUnderNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.BoldUnder);

  private BoldUnderNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmBold> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    String contents = NodeFactory.substring(input, foundInfo.contentRange());
    MfmText textNode = new MfmText(contents);
    return IFactoryResult.success(new MfmBold(List.of(textNode)), foundInfo);
  }
}
