package io.github.ocelot.mixin;

import io.github.ocelot.client.ChatWindow;
import io.github.ocelot.client.ChatWindowRenderer;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Ocelot
 */
@Mixin(NewChatGui.class)
public class NewChatGuiMixin
{
    @Inject(method = "func_238493_a_", at = @At("TAIL"))
    public void func_238493_a_(ITextComponent p_238493_1_, int p_238493_2_, int p_238493_3_, boolean p_238493_4_, CallbackInfo ci)
    {
        ChatWindowRenderer.updateText();
    }

    @Inject(method = "clearChatMessages", at = @At("TAIL"))
    public void clearChatMessages(boolean clearSentMsgHistory, CallbackInfo ci)
    {
        ChatWindowRenderer.updateText();
    }

    @Inject(method = "func_238496_i_", at = @At("HEAD"), cancellable = true)
    public void func_238492_a_(CallbackInfoReturnable<Boolean> cir)
    {
        if (ChatWindow.isOpen())
            cir.setReturnValue(true);
    }
}
