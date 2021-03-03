package io.github.ocelot.popoutchat.forge;

import io.github.ocelot.popoutchat.client.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;

@SuppressWarnings("unused")
public class PopoutChatImpl
{
    public static boolean onGuiCharTypedPre(Screen screen, char codepoint, int mods)
    {
        return ForgeHooksClient.onGuiCharTypedPre(screen, codepoint, mods);
    }

    public static void onGuiCharTypedPost(Screen screen, char codepoint, int mods)
    {
        ForgeHooksClient.onGuiCharTypedPost(screen, codepoint, mods);
    }

    public static Config getConfig()
    {
        return PopoutChatForge.config;
    }
}
