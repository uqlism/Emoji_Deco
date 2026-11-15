package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmFn;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/BigNodeFactory.kt}
 */
public final class BigNodeFactory extends SimpleNodeFactoryBase<MfmFn> {
  public static final BigNodeFactory INSTANCE = new BigNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Big);

  private BigNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmFn> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    List<IMfmNode> result = NodeFactory.createNodes(input, foundInfo.nestedInfos(), context, MfmNodeAttribute.SET_OF_INLINE);
    if (result.isEmpty()) {
      return IFactoryResult.failure();
    }

    MfmFn node = new MfmFn("tada", Map.of(), result);
    return IFactoryResult.success(node, foundInfo);
  }
}
