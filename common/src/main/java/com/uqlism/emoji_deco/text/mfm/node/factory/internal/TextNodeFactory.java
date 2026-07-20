package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/TextNodeFactory.kt}
 *
 * <p>Unlike the other factories, this implements {@link INodeFactory} directly rather than
 * extending {@link SimpleNodeFactoryBase}: mfm.kt's version performs no
 * {@code supportFoundTypes}/nest-level check of its own, relying on the caller
 * ({@code NodeFactory}) only ever invoking it for {@code FoundType.Text} (whose nest-level guard
 * is already applied one level up, in {@code NodeFactory.createNodesImpl}).
 */
public final class TextNodeFactory implements INodeFactory<MfmText> {
  public static final TextNodeFactory INSTANCE = new TextNodeFactory();

  private TextNodeFactory() {
  }

  @Override
  public IFactoryResult<MfmText> create(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    return IFactoryResult.success(new MfmText(NodeFactory.substring(input, foundInfo.contentRange())), foundInfo);
  }
}
