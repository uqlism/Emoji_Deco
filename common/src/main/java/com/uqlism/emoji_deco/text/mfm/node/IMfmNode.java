package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/IMfmNode.kt}
 *
 * <p>Base interface for all nodes in the MFM AST.
 */
public interface IMfmNode {
  /** The type of this node. */
  MfmNodeType type();

  /** Re-serializes this node (and its descendants, if any) back into MFM syntax. */
  String stringify();
}
