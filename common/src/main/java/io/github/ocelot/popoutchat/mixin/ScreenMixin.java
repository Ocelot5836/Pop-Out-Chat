package io.github.ocelot.popoutchat.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ocelot.popoutchat.client.ChatWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Ocelot
 */
@Mixin(Screen.class)
public class ScreenMixin
{
    @Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
    private static void hasControlDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (!ChatWindow.isOpen() || !ChatWindow.hasFocus())
            return;
        if (Minecraft.ON_OSX)
        {
            cir.setReturnValue(InputConstants.isKeyDown(ChatWindow.getHandle(), 343) || InputConstants.isKeyDown(ChatWindow.getHandle(), 347));
        }
        else
        {
            cir.setReturnValue(InputConstants.isKeyDown(ChatWindow.getHandle(), 341) || InputConstants.isKeyDown(ChatWindow.getHandle(), 345));
        }
    }

    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private static void hasShiftDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (!ChatWindow.isOpen() || !ChatWindow.hasFocus())
            return;
        cir.setReturnValue(InputConstants.isKeyDown(ChatWindow.getHandle(), 340) || InputConstants.isKeyDown(ChatWindow.getHandle(), 344));
    }

    @Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
    private static void hasAltDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (!ChatWindow.isOpen() || !ChatWindow.hasFocus())
            return;
        cir.setReturnValue(InputConstants.isKeyDown(ChatWindow.getHandle(), 342) || InputConstants.isKeyDown(ChatWindow.getHandle(), 346));
    }
}
