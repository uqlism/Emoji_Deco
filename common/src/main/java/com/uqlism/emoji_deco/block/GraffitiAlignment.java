package com.uqlism.emoji_deco.block;

import net.minecraft.util.StringRepresentable;

public enum GraffitiAlignment implements StringRepresentable {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    private final String name;

    GraffitiAlignment(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public GraffitiAlignment next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static GraffitiAlignment byName(String name) {
        for (GraffitiAlignment a : values()) {
            if (a.name.equals(name)) return a;
        }
        return LEFT;
    }
}
