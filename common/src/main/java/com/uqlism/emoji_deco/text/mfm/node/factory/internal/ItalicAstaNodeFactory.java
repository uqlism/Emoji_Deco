package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmItalic;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.List;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/ItalicAstaNodeFactory.kt}
 */
public final class ItalicAstaNodeFactory extends SimpleNodeFactoryBase<MfmItalic> {
  public static final ItalicAstaNodeFactory INSTANCE = new ItalicAstaNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.ItalicAsta);

  private ItalicAstaNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmItalic> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    String contents = NodeFactory.substring(input, foundInfo.contentRange());
    return IFactoryResult.success(new MfmItalic(List.of(new MfmText(contents))), foundInfo);
  }
}
