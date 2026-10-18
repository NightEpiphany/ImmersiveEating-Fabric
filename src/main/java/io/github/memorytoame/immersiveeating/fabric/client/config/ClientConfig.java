package io.github.memorytoame.immersiveeating.fabric.client.config;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

@Environment(EnvType.CLIENT)
public final class ClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue HIDE_GUI_WHEN_EATING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("immersive_eating");
        HIDE_GUI_WHEN_EATING = builder.comment("Hide Your hotbar & status bar when eating").define("hideGUIWhenEating", true);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static void register() {
        ForgeConfigRegistry.INSTANCE.register("food", ModConfig.Type.CLIENT, CLIENT_SPEC, "immersive_eating_client.toml");
    }
}
