package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.MfmFn;
import com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute;
import com.uqlism.emoji_deco.text.mfm.node.factory.NodeFactory;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.inline.FnParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/FnNodeFactory.kt}
 */
public final class FnNodeFactory extends SimpleNodeFactoryBase<MfmFn> {
  public static final FnNodeFactory INSTANCE = new FnNodeFactory();

  private static final Set<FoundType> SUPPORT_FOUND_TYPES = Set.of(FoundType.Fn);

  private FnNodeFactory() {
  }

  @Override
  protected Set<FoundType> supportFoundTypes() {
    return SUPPORT_FOUND_TYPES;
  }

  @Override
  protected IFactoryResult<MfmFn> doCreate(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    List<IMfmNode> contents = sliceContent(input, foundInfo, context);
    if (contents.isEmpty()) {
      return IFactoryResult.failure();
    }

    String name = sliceName(input, foundInfo);
    Map<String, Object> args = sliceArgs(input, foundInfo);

    return IFactoryResult.success(new MfmFn(name, args, contents), foundInfo);
  }

  private static String sliceName(String input, SubstringFoundInfo foundInfo) {
    SubstringFoundInfo name = foundInfo.get(FnParser.SubIndex.Name);
    return NodeFactory.substring(input, name.contentRange());
  }

  private static Map<String, Object> sliceArgs(String input, SubstringFoundInfo foundInfo) {
    SubstringFoundInfo args = foundInfo.get(FnParser.SubIndex.Args);
    Map<String, Object> result = new LinkedHashMap<>();
    for (SubstringFoundInfo arg : args.nestedInfos()) {
      // Kotlin's `"a=b=c".split("=")` yields ["a","b","c"]; only the first two parts are kept
      // (`it[0] to it[1]`), so a value containing "=" is intentionally truncated at the first "=".
      // `-1` (no limit) mirrors Kotlin's default split, which keeps trailing empty strings.
      String[] split = NodeFactory.substring(input, arg.contentRange()).split("=", -1);
      result.put(split[0], split.length == 1 ? MfmFn.NO_VALUE : split[1]);
    }
    return result;
  }

  private static List<IMfmNode> sliceContent(String input, SubstringFoundInfo foundInfo, INodeFactoryContext context) {
    SubstringFoundInfo content = foundInfo.get(FnParser.SubIndex.Content);
    return NodeFactory.createNodes(input, content.nestedInfos(), context, MfmNodeAttribute.SET_OF_INLINE);
  }
}
