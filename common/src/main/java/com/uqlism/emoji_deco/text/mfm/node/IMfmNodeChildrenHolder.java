package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/IMfmNodeChildrenHolder.kt}
 *
 * <p>Marks a node that holds child nodes.
 *
 * <p>The Kotlin source models {@code children} as a mutable list and exposes {@code addChild} to
 * append to it after construction. Node implementations here are Java {@code record}s built with
 * their full, final child list up front (see the plan's decision to drop the {@code Props}
 * indirection), so no ported node ever needs to mutate its children after construction — a repo
 * search of mfm.kt confirms {@code addChild} is never called outside of its own declaration.
 * Both methods are therefore kept as {@code default} methods for interface fidelity, with the
 * multi-node overload throwing {@link UnsupportedOperationException} rather than being
 * implemented per-record.
 */
public interface IMfmNodeChildrenHolder extends IMfmNode {
  /** The child nodes held by this node. */
  List<IMfmNode> children();

  /** Ported from mfm.kt's default {@code addChild(node)} which delegates to the plural overload. */
  default void addChild(IMfmNode node) {
    addChild(List.of(node));
  }

  /**
   * Kotlin declares this as abstract; ported as unsupported here since record-based nodes are
   * immutable and this method is unused throughout mfm.kt (see class javadoc).
   */
  default void addChild(Iterable<IMfmNode> nodes) {
    throw new UnsupportedOperationException("MFM AST nodes are immutable records; children are fixed at construction time");
  }
}
