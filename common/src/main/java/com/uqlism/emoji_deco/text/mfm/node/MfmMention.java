package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmMention.kt}
 *
 * <p>The Kotlin {@code Props} keeps {@code acct} as a separately-stored field (recomputed on every
 * {@code username}/{@code host} mutation) because its serialization framework drops computed
 * getters. That mutation concern doesn't apply to an immutable Java record, so {@link #acct()} is
 * a plain derived accessor instead of a stored component. This is semantically equivalent for
 * {@code equals}/{@code hashCode}/{@code toString}: two mentions with the same
 * {@code (username, host)} always produce the same {@code acct}.
 *
 * @param host Nullable, matching the Kotlin {@code host: String?} property.
 */
public record MfmMention(String username, String host) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.Mention;
  }

  @Override
  public String stringify() {
    return acct();
  }

  public String acct() {
    return host != null ? "@" + username + "@" + host : "@" + username;
  }
}
