package io.github.ocelot.popoutchat.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Deque;
import java.util.List;

/**
 * @author Ocelot
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor
{
    @Accessor
    List<GuiMessage<Component>> getAllMessages();

    @Accessor
    Deque<Component> getChatQueue();

    @Accessor
    long getLastMessage();

    @Accessor
    void setLastMessage(long lastMessage);
}
