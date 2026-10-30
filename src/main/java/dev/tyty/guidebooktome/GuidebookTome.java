package dev.tyty.guidebooktome;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(GuidebookTome.MOD_ID)
public class GuidebookTome {
    public static final String MOD_ID = "guidebooktome";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GuidebookTome(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        StarterTomeHandler.register();
    }
}
