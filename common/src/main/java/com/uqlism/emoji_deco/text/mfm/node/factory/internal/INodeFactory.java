package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/INodeFactory.kt}
 *
 * <p>Creates an {@link IMfmNode} from an {@link com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser} result.
 */
public interface INodeFactory<T extends IMfmNode> {
  IFactoryResult<T> create(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context);
}
