package io.github.ocelot.mixin;

import io.github.ocelot.client.ChatWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
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
        if (Minecraft.IS_RUNNING_ON_MAC)
        {
            cir.setReturnValue(InputMappings.isKeyDown(ChatWindow.getHandle(), 343) || InputMappings.isKeyDown(ChatWindow.getHandle(), 347));
        }
        else
        {
            cir.setReturnValue(InputMappings.isKeyDown(ChatWindow.getHandle(), 341) || InputMappings.isKeyDown(ChatWindow.getHandle(), 345));
        }
    }

    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private static void hasShiftDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (!ChatWindow.isOpen() || !ChatWindow.hasFocus())
            return;
        cir.setReturnValue(InputMappings.isKeyDown(ChatWindow.getHandle(), 340) || InputMappings.isKeyDown(ChatWindow.getHandle(), 344));
    }

    @Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
    private static void hasAltDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (!ChatWindow.isOpen() || !ChatWindow.hasFocus())
            return;
        cir.setReturnValue(InputMappings.isKeyDown(ChatWindow.getHandle(), 342) || InputMappings.isKeyDown(ChatWindow.getHandle(), 346));
    }
}
