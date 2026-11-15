package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmSearch;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.block.SearchParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/SearchNodeFactory.kt}
 */
public final class SearchNodeFactory extends SimpleNodeFactoryBase<MfmSearch> {
  public static final SearchNodeFactory INSTANCE = new SearchNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Search);

  private SearchNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmSearch> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    String query = NodeFactory.substring(input, foundInfo.get(SearchParser.SubIndex.Query).contentRange());
    String space = NodeFactory.substring(input, foundInfo.get(SearchParser.SubIndex.Space).contentRange());
    String button = NodeFactory.substring(input, foundInfo.get(SearchParser.SubIndex.Button).contentRange());

    return IFactoryResult.success(new MfmSearch(query, query + space + button), foundInfo);
  }
}
