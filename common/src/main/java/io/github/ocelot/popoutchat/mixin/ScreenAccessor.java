package io.github.ocelot.popoutchat.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Ocelot
 */
@Mixin(Screen.class)
public interface ScreenAccessor
{
    @Invoker
    <T extends AbstractWidget> T invokeAddButton(T abstractWidget);
}
