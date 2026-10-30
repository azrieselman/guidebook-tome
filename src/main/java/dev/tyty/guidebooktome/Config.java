package dev.tyty.guidebooktome;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder
                .comment("When true, the first time a player joins they receive one Akashic Tome filled with the guidebooks loaded at startup. Joining again does not give another tome.")
                .define("enable", true);
        SPEC = builder.build();
    }

    private Config() {
    }
}
