package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmPlain;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/PlainTagNodeFactory.kt}
 */
public final class PlainTagNodeFactory extends SimpleNodeFactoryBase<MfmPlain> {
  public static final PlainTagNodeFactory INSTANCE = new PlainTagNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.PlainTag);

  private PlainTagNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmPlain> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    MfmText textNode = new MfmText(NodeFactory.substring(input, foundInfo.contentRange()));
    return IFactoryResult.success(new MfmPlain(List.of(textNode)), foundInfo);
  }
}
