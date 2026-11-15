package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmHashtag;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/HashtagNodeFactory.kt}
 */
public final class HashtagNodeFactory extends SimpleNodeFactoryBase<MfmHashtag> {
  public static final HashtagNodeFactory INSTANCE = new HashtagNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Hashtag);

  private HashtagNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmHashtag> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    return IFactoryResult.success(new MfmHashtag(NodeFactory.substring(input, foundInfo.contentRange())), foundInfo);
  }
}
