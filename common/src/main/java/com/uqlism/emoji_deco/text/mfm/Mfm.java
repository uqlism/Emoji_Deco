package com.uqlism.emoji_deco.text.mfm;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.INodeFactoryContext;
import com.uqlism.emoji_deco.text.mfm.parser.FullParser;
import com.uqlism.emoji_deco.text.mfm.parser.SimpleParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/Mfm.kt}
 *
 * <p>Top-level facade for turning an MFM string into an AST. This is the sole entry point callers
 * outside this package are expected to use.
 */
public final class Mfm {
  private static final int DEFAULT_MAX_NEST_LEVEL = 20;

  private Mfm() {
  }

  /** Generates an {@link IMfmNode} tree from the MFM string, recognizing all syntax. */
  public static List<IMfmNode> parse(String input) {
    return parse(input, DEFAULT_MAX_NEST_LEVEL);
  }

  /** Generates an {@link IMfmNode} tree from the MFM string, recognizing all syntax. */
  public static List<IMfmNode> parse(String input, int maxNestLevel) {
    IMfmParserContext finderContext = new IMfmParserContext.Impl();
    IMfmParserResult findResult = new FullParser().find(input, 0, finderContext);
    if (!findResult.success()) {
      return List.of();
    }

    INodeFactoryContext factoryContext = new INodeFactoryContext.Impl(maxNestLevel, 0);
    return NodeFactory.createNodes(
        input,
        findResult.foundInfo().nestedInfos(),
        factoryContext,
        MfmNodeAttribute.SET_OF_ALL
    );
  }

  /**
   * Generates an {@link IMfmNode} tree from the MFM string, recognizing only "simple" syntax
   * (unicode emoji / emoji code).
   */
  public static List<IMfmNode> parseSimple(String input) {
    return parseSimple(input, DEFAULT_MAX_NEST_LEVEL);
  }

  /**
   * Generates an {@link IMfmNode} tree from the MFM string, recognizing only "simple" syntax
   * (unicode emoji / emoji code).
   */
  public static List<IMfmNode> parseSimple(String input, int maxNestLevel) {
    IMfmParserContext finderContext = new IMfmParserContext.Impl();
    IMfmParserResult findResult = new SimpleParser().find(input, 0, finderContext);
    if (!findResult.success()) {
      return List.of();
    }

    INodeFactoryContext factoryContext = new INodeFactoryContext.Impl(maxNestLevel, 0);
    return NodeFactory.createNodes(
        input,
        findResult.foundInfo().nestedInfos(),
        factoryContext,
        MfmNodeAttribute.SET_OF_SIMPLE
    );
  }
}
