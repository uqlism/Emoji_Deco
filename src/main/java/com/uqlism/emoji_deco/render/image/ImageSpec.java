package com.uqlism.emoji_deco.render.image;

import org.jetbrains.annotations.Nullable;

public sealed interface ImageSpec permits ImageSpec.Decoded, ImageSpec.Atlas, ImageSpec.Skin {

    record Decoded(@Nullable String format, BinarySource source) implements ImageSpec {}
    record Atlas(String atlas, String sprite)                    implements ImageSpec {}
    record Skin(String player)                                   implements ImageSpec {}
}
