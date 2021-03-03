package io.github.ocelot.popoutchat.fabric;

import io.github.ocelot.popoutchat.client.Config;
import net.minecraft.client.gui.screens.Screen;

@SuppressWarnings("unused")
public class PopoutChatImpl
{
    public static boolean onGuiCharTypedPre(Screen screen, char codepoint, int mods)
    {
        return false;
    }

    public static void onGuiCharTypedPost(Screen screen, char codepoint, int mods)
    {
    }

    public static Config getConfig()
    {
        return PopoutChatFabric.config;
    }
}
