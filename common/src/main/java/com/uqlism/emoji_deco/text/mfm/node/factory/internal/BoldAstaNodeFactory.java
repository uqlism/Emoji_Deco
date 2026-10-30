package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmBold;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/BoldAstaNodeFactory.kt}
 */
public final class BoldAstaNodeFactory extends SimpleNodeFactoryBase<MfmBold> {
  public static final BoldAstaNodeFactory INSTANCE = new BoldAstaNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.BoldAsta);

  private BoldAstaNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmBold> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    List<IMfmNode> result = NodeFactory.createNodes(input, foundInfo.nestedInfos(), context, MfmNodeAttribute.SET_OF_INLINE);
    if (result.isEmpty()) {
      return IFactoryResult.failure();
    }

    return IFactoryResult.success(new MfmBold(result), foundInfo);
  }
}
