package io.github.ocelot.client;

import io.github.ocelot.DetachableChat;
import io.github.ocelot.client.util.PopOutButton;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author Ocelot
 */
@Mod.EventBusSubscriber(modid = DetachableChat.MOD_ID, value = Dist.CLIENT)
public class ClientEvents
{
    private static Button popOut;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START)
            return;

        ChatWindowRenderer.tick();
    }

    @SubscribeEvent
    public static void tick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        ChatWindow.render();
    }

    @SubscribeEvent
    public static void onEvent(GuiScreenEvent.InitGuiEvent event)
    {
        Screen screen = event.getGui();
        if (screen instanceof ChatScreen)
        {
            event.addWidget(popOut = new PopOutButton(screen.width - 22, 2, new TranslationTextComponent("button." + DetachableChat.MOD_ID + ".toggle"), button ->
            {
                if (!ChatWindow.isOpen())
                {
                    button.visible = false;
                    ChatWindow.create();
                }
            }, (button, matrixStack, mouseX, mouseY) -> screen.renderTooltip(matrixStack, new TranslationTextComponent("button." + DetachableChat.MOD_ID + "." + (ChatWindow.isOpen() ? "close" : "open")), mouseX, mouseY)));
            popOut.visible = !ChatWindow.isOpen();
        }
        else
        {
            popOut = null;
        }
    }

    /**
     * Called when the chat window closes to make the pop out button visible again.
     */
    public static void onChatWindowClose()
    {
        if (popOut != null)
        {
            popOut.visible = true;
        }
    }
}
