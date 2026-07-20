package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.MfmBlockCode;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.block.CodeBlockParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/CodeBlockNodeFactory.kt}
 */
public final class CodeBlockNodeFactory extends SimpleNodeFactoryBase<MfmBlockCode> {
  public static final CodeBlockNodeFactory INSTANCE = new CodeBlockNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.CodeBlock);

  private CodeBlockNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmBlockCode> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    SubstringFoundInfo langResult = foundInfo.get(CodeBlockParser.SubIndex.Lang);
    SubstringFoundInfo codeResult = foundInfo.get(CodeBlockParser.SubIndex.Code);

    String code = NodeFactory.substring(input, codeResult.contentRange());
    MfmBlockCode node = langResult.equals(SubstringFoundInfo.EMPTY)
        ? new MfmBlockCode(code)
        : new MfmBlockCode(code, NodeFactory.substring(input, langResult.contentRange()));

    return IFactoryResult.success(node, foundInfo);
  }
}
