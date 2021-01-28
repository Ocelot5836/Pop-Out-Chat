package io.github.ocelot.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ocelot.PopoutChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * @author Ocelot
 */
public class PopOutButton extends Button
{
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(PopoutChat.MOD_ID, "textures/gui/pop_out.png");

    public PopOutButton(int x, int y, ITextComponent title, IPressable pressedAction, ITooltip onTooltip)
    {
        super(x, y, 20, 20, title, pressedAction, onTooltip);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, ((int) (255.0D * minecraft.gameSettings.accessibilityTextBackgroundOpacity) << 24));
        minecraft.getTextureManager().bindTexture(TEXTURE_LOCATION);
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
