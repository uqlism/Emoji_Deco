package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmMention;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.inline.MentionParser;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/MentionNodeFactory.kt}
 */
public final class MentionNodeFactory extends SimpleNodeFactoryBase<MfmMention> {
  public static final MentionNodeFactory INSTANCE = new MentionNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Mention);

  private MentionNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmMention> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    SubstringFoundInfo usernameInfo = foundInfo.get(MentionParser.SubIndex.Username);
    SubstringFoundInfo hostnameInfo = foundInfo.get(MentionParser.SubIndex.Hostname);

    String username = NodeFactory.substring(input, usernameInfo.contentRange());
    String hostname = hostnameInfo.equals(SubstringFoundInfo.EMPTY)
        ? null
        : NodeFactory.substring(input, hostnameInfo.contentRange());

    return IFactoryResult.success(new MfmMention(username, hostname), foundInfo);
  }
}
