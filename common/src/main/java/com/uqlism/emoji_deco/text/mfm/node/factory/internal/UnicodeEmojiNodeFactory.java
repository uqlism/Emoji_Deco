package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmUnicodeEmoji;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/UnicodeEmojiNodeFactory.kt}
 */
public final class UnicodeEmojiNodeFactory extends SimpleNodeFactoryBase<MfmUnicodeEmoji> {
  public static final UnicodeEmojiNodeFactory INSTANCE = new UnicodeEmojiNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.UnicodeEmoji);

  private UnicodeEmojiNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmUnicodeEmoji> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    return IFactoryResult.success(new MfmUnicodeEmoji(NodeFactory.substring(input, foundInfo.contentRange())), foundInfo);
  }
}
