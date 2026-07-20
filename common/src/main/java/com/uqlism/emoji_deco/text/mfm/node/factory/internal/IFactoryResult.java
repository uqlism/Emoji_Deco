package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/IFactoryResult.kt}
 *
 * <p>The Kotlin top-level {@code success}/{@code failure} factory functions are ported as static
 * methods here, matching the convention already used by
 * {@link com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult}.
 */
public interface IFactoryResult<T extends IMfmNode> {
  boolean success();

  /** Throws {@link IllegalStateException} if {@link #success()} is {@code false}, as in Kotlin's {@code error(...)}. */
  T node();

  SubstringFoundInfo foundInfo();

  static <T extends IMfmNode> IFactoryResult<T> success(T node, SubstringFoundInfo foundInfo) {
    return new Impl<>(true, node, foundInfo);
  }

  static <T extends IMfmNode> IFactoryResult<T> failure() {
    return new Impl<>(false, null, SubstringFoundInfo.EMPTY);
  }

  final class Impl<T extends IMfmNode> implements IFactoryResult<T> {
    private final boolean success;
    private final T node;
    private final SubstringFoundInfo foundInfo;

    Impl(boolean success, T node, SubstringFoundInfo foundInfo) {
      this.success = success;
      this.node = node;
      this.foundInfo = foundInfo;
    }

    @Override
    public boolean success() {
      return success;
    }

    @Override
    public T node() {
      if (node == null) {
        throw new IllegalStateException("node is null");
      }
      return node;
    }

    @Override
    public SubstringFoundInfo foundInfo() {
      return foundInfo;
    }
  }
}
