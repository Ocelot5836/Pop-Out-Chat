package io.github.ocelot.popoutchat.mixin;

import io.github.ocelot.popoutchat.client.ChatWindow;
import io.github.ocelot.popoutchat.client.ChatWindowRenderer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Ocelot
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin
{
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;IIZ)V", at = @At("TAIL"))
    public void addMessage(Component component, int i, int j, boolean bl, CallbackInfo ci)
    {
        ChatWindowRenderer.updateText();
    }

    @Inject(method = "clearMessages", at = @At("TAIL"))
    public void clearMessages(boolean clearSentMsgHistory, CallbackInfo ci)
    {
        ChatWindowRenderer.updateText();
    }

    @Inject(method = "isChatHidden", at = @At("HEAD"), cancellable = true)
    public void isChatHidden(CallbackInfoReturnable<Boolean> cir)
    {
        if (ChatWindow.isOpen())
            cir.setReturnValue(true);
    }
}
