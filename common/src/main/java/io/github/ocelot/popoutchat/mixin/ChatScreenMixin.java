package io.github.ocelot.popoutchat.mixin;

import io.github.ocelot.popoutchat.PopoutChat;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Ocelot
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin
{
    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci)
    {
        PopoutChat.addButton((ChatScreen) (Object) this);
    }
}
