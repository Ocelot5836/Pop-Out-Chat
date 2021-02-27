package io.github.ocelot.popoutchat.fabric;

import net.minecraft.client.gui.screens.Screen;

@SuppressWarnings("unused")
public class PopoutChatImpl
{
    @SuppressWarnings("unused")
    public static boolean onGuiCharTypedPre(Screen screen, char codepoint, int mods)
    {
        return false;
    }

    @SuppressWarnings("unused")
    public static void onGuiCharTypedPost(Screen screen, char codepoint, int mods)
    {
    }
}
