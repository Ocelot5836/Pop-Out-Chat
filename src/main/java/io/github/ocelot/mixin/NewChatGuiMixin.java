package io.github.ocelot.mixin;

import io.github.ocelot.client.ChatWindowRenderer;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NewChatGui.class)
public class NewChatGuiMixin
{
    @Inject(method = "func_238493_a_", at = @At("TAIL"))
    public void func_238493_a_(ITextComponent p_238493_1_, int p_238493_2_, int p_238493_3_, boolean p_238493_4_, CallbackInfo ci)
    {
        ChatWindowRenderer.updateText();
    }
}
