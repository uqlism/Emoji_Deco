package com.uqlism.emoji_deco.render.image;

import org.jetbrains.annotations.Nullable;
import java.util.Map;

public sealed interface BinarySource permits BinarySource.Url, BinarySource.Resource {

    record Url(String url, boolean diskCache, int ttlSeconds,
               String method, Map<String, String> headers, @Nullable String body)
            implements BinarySource {}

    record Resource(String path) implements BinarySource {}
}
