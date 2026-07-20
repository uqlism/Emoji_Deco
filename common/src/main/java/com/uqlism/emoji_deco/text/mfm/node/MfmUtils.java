package com.uqlism.emoji_deco.text.mfm.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/MfmUtils.kt}
 *
 * <p>The Kotlin file lives directly under the {@code api} package (a sibling of {@code node}), but
 * every one of its functions exists solely to support {@link IMfmNode#stringify()} for
 * children-holding nodes, so it is ported here into the {@code node} package instead of a
 * separate top-level file. The Kotlin extension functions ({@code IMfmNode.inspect}, etc.) are
 * ported as the equivalent static overloads instead.
 */
public final class MfmUtils {
  private MfmUtils() {
  }

  /**
   * Generates an MFM string from a list of nodes, inserting newlines at block/inline boundaries
   * the same way mfm.kt does: no newline between two inline nodes, a newline whenever a block node
   * is adjacent to any other node (on either side).
   */
  public static String stringify(Iterable<IMfmNode> nodes) {
    List<IMfmNode> resultNodes = new ArrayList<>();
    MfmNodeType prevNodeType = MfmNodeType.None;
    for (IMfmNode node : nodes) {
      if (node.type().attributes().contains(MfmNodeAttribute.Block)) {
        if (prevNodeType != MfmNodeType.None) {
          resultNodes.add(new MfmText("\n"));
        }
      } else {
        if (prevNodeType != MfmNodeType.None && !prevNodeType.attributes().contains(MfmNodeAttribute.Inline)) {
          resultNodes.add(new MfmText("\n"));
        }
      }

      resultNodes.add(node);
      prevNodeType = node.type();
    }

    StringBuilder sb = new StringBuilder();
    for (IMfmNode node : resultNodes) {
      sb.append(node.stringify());
    }
    return sb.toString();
  }

  /** Inspects the MfmNode tree, depth-first. */
  public static void inspect(IMfmNode node, Consumer<IMfmNode> action) {
    action.accept(node);
    if (node instanceof IMfmNodeChildrenHolder holder) {
      for (IMfmNode child : holder.children()) {
        inspect(child, action);
      }
    }
  }

  /** Inspects the MfmNode tree, depth-first. */
  public static void inspect(Iterable<IMfmNode> nodes, Consumer<IMfmNode> action) {
    for (IMfmNode node : nodes) {
      inspect(node, action);
    }
  }

  /** Inspects the MfmNode tree and returns the nodes that match {@code predicate}. */
  public static List<IMfmNode> extract(Iterable<IMfmNode> nodes, Predicate<IMfmNode> predicate) {
    List<IMfmNode> result = new ArrayList<>();
    inspect(nodes, node -> {
      if (predicate.test(node)) {
        result.add(node);
      }
    });
    return result;
  }

  /**
   * Ported from Kotlin stdlib's {@code CharSequence.prependIndent(indent)} (used by
   * {@link MfmQuote#stringify()}). Splits on {@code \r\n}, {@code \n}, or {@code \r}; blank lines
   * are left untouched unless shorter than {@code indent} (in which case they become exactly
   * {@code indent}), and all other lines are prefixed with {@code indent}. Lines are rejoined with
   * {@code \n} regardless of the original line separator, matching Kotlin's behavior.
   */
  public static String prependIndent(String text, String indent) {
    String[] lines = text.split("\r\n|\r|\n", -1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        sb.append('\n');
      }
      String line = lines[i];
      if (line.isBlank()) {
        sb.append(line.length() < indent.length() ? indent : line);
      } else {
        sb.append(indent).append(line);
      }
    }
    return sb.toString();
  }
}
