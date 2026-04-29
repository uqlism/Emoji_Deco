package com.uqlism.emoji_deco.render.image;

public sealed interface BinarySource permits BinarySource.Url, BinarySource.Resource {

    record Url(String url)       implements BinarySource {}
    record Resource(String path) implements BinarySource {}
}
