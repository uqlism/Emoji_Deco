package com.uqlism.emoji_deco.render.image;

public sealed interface BinarySource permits BinarySource.Url, BinarySource.Resource {

    record Url(String url, boolean diskCache, int ttlSeconds) implements BinarySource {}
    record Resource(String path) implements BinarySource {}
}
