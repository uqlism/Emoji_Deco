package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmLink;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.inline.LinkParser;

import java.util.List;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/LinkNodeFactory.kt}
 */
public final class LinkNodeFactory extends SimpleNodeFactoryBase<MfmLink> {
  public static final LinkNodeFactory INSTANCE = new LinkNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Link);

  private LinkNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmLink> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    SubstringFoundInfo squareOpen = foundInfo.get(LinkParser.SubIndex.SquareOpen);
    SubstringFoundInfo label = foundInfo.get(LinkParser.SubIndex.Label);
    SubstringFoundInfo url = foundInfo.get(LinkParser.SubIndex.Url);

    boolean isSilent = NodeFactory.substring(input, squareOpen.contentRange()).equals("?[");
    String urlText = NodeFactory.substring(input, url.contentRange());

    List<IMfmNode> labelContents = NodeFactory.createNodes(input, label.nestedInfos(), context, MfmNodeAttribute.SET_OF_INLINE);
    if (labelContents.isEmpty()) {
      return IFactoryResult.failure();
    }

    return IFactoryResult.success(new MfmLink(isSilent, urlText, labelContents), foundInfo);
  }
}
