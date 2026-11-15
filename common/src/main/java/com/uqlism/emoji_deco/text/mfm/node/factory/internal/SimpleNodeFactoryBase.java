package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/SimpleNodeFactoryBase.kt}
 */
public abstract class SimpleNodeFactoryBase<T extends IMfmNode> implements INodeFactory<T> {
  protected abstract Set<FoundType> supportFoundTypes();

  @Override
  public final IFactoryResult<T> create(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    if (!supportFoundTypes().contains(foundInfo.type())) {
      return IFactoryResult.failure();
    }

    if (context.nestLevel() > context.maximumNestLevel()) {
      return IFactoryResult.failure();
    }

    return doCreate(input, foundInfo, context);
  }

  protected abstract IFactoryResult<T> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context);
}
