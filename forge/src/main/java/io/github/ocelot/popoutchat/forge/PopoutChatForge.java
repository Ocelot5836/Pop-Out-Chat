package io.github.ocelot.popoutchat.forge;

import io.github.ocelot.popoutchat.PopoutChat;
import io.github.ocelot.popoutchat.client.Config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Ocelot
 */
@Mod(PopoutChat.MOD_ID)
public class PopoutChatForge
{
    static Config config;

    public PopoutChatForge()
    {
        Pair<Config, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(ForgeConfig::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpecPair.getRight());
        config = clientSpecPair.getLeft();
    }
}
