package io.github.ocelot.popoutchat.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.popoutchat.PopoutChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Ocelot
 */
public class PopOutButton extends Button
{
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(PopoutChat.MOD_ID, "textures/gui/pop_out.png");

    public PopOutButton(int x, int y, Component title, OnPress pressedAction, OnTooltip onTooltip)
    {
        super(x, y, 20, 20, title, pressedAction, onTooltip);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, ((int) (255.0D * minecraft.options.textBackgroundOpacity) << 24));
        minecraft.getTextureManager().bind(TEXTURE_LOCATION);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.renderBg(matrixStack, minecraft, mouseX, mouseY);
        RenderSystem.color4f(1.0F, 1.0F, this.isHovered() ? 0.0F : 1.0F, this.alpha);
        blit(matrixStack, this.x + 2, this.y + 2, 16, 16, 0, 0, 1, 1, 1, 1);

        if (this.isHovered())
            this.renderToolTip(matrixStack, mouseX, mouseY);
    }
}
