package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmFn.kt}
 *
 * <p>Kotlin represents a flag-only argument (e.g. {@code $[spin.x]}, where {@code x} has no
 * {@code =value}) as {@code Unit} in the {@code Map<String, Any>}. Java has no equivalent
 * singleton usable as a generic map value, so {@link #NO_VALUE} is a dedicated sentinel that plays
 * the same role; {@code args} values are either a {@link String} or exactly {@link #NO_VALUE}.
 */
public record MfmFn(String name, Map<String, Object> args, List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  /** Sentinel marking a flag-only argument, mirroring Kotlin's {@code Unit} in {@code args}. */
  public static final Object NO_VALUE = new Object() {
    @Override
    public String toString() {
      return "";
    }
  };

  public MfmFn {
    args = Map.copyOf(args);
    children = List.copyOf(children);
  }

  public MfmFn(String name, Map<String, Object> args, IMfmNode... children) {
    this(name, args, List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Fn;
  }

  @Override
  public String stringify() {
    String argFields = args.entrySet().stream()
        .map(e -> e.getValue() == NO_VALUE ? e.getKey() : e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(","));
    String argsPart = argFields.isEmpty() ? "" : "." + argFields;

    return "$[" + name + argsPart + " " + MfmUtils.stringify(children) + "]";
  }
}
