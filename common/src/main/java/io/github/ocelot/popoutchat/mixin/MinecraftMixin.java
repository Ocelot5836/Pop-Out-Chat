package io.github.ocelot.popoutchat.mixin;

import io.github.ocelot.popoutchat.client.ChatWindow;
import io.github.ocelot.popoutchat.client.ChatWindowRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Ocelot
 */
@Mixin(Minecraft.class)
public class MinecraftMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci)
    {
        ChatWindowRenderer.tick();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 4, shift = At.Shift.AFTER))
    public void runTick(boolean bl, CallbackInfo ci)
    {
        ChatWindow.render();
    }
}
