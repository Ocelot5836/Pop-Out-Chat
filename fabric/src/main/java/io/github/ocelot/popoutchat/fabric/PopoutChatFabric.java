package io.github.ocelot.popoutchat.fabric;

import io.github.ocelot.popoutchat.client.Config;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/**
 * @author Ocelot
 */
public class PopoutChatFabric implements ClientModInitializer
{
    static Config config;

    @Override
    public void onInitializeClient()
    {
        config = new FabricConfig(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
    }
}
