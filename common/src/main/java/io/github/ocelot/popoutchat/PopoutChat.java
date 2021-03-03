package io.github.ocelot.popoutchat;

import io.github.ocelot.popoutchat.client.ChatWindow;
import io.github.ocelot.popoutchat.client.Config;
import io.github.ocelot.popoutchat.client.util.PopOutButton;
import io.github.ocelot.popoutchat.mixin.ScreenAccessor;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * @author Ocelot
 */
public class PopoutChat
{
    public static final String MOD_ID = "popoutchat";

    private static Button popOutButton;

    /**
     * Adds the pop out button into the chat screen.
     */
    public static void addButton(ChatScreen screen)
    {
        ((ScreenAccessor) screen).invokeAddButton(popOutButton = new PopOutButton(screen.width - 22, 2, new TranslatableComponent("button." + PopoutChat.MOD_ID + ".pop_out"), button ->
        {
            if (!ChatWindow.isOpen())
            {
                button.visible = false;
                ChatWindow.create();
            }
        }, (button, matrixStack, mouseX, mouseY) -> screen.renderTooltip(matrixStack, button.getMessage(), mouseX, mouseY)));
        popOutButton.visible = !ChatWindow.isOpen();
    }

    /**
     * Called when the chat window closes to make the pop out button visible again.
     */
    public static void onChatWindowClose()
    {
        if (popOutButton != null)
        {
            popOutButton.visible = true;
        }
    }

    @SuppressWarnings("unused")
    @ExpectPlatform
    public static boolean onGuiCharTypedPre(Screen screen, char codepoint, int mods)
    {
        throw new AssertionError();
    }

    @SuppressWarnings("unused")
    @ExpectPlatform
    public static void onGuiCharTypedPost(Screen screen, char codepoint, int mods)
    {
        throw new AssertionError();
    }

    @SuppressWarnings("unused")
    @ExpectPlatform
    public static Config getConfig()
    {
        throw new AssertionError();
    }
}
