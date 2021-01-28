package io.github.ocelot.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ocelot.DetachableChat;
import io.github.ocelot.client.util.ScrollHandler;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.FramebufferConstants;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * <p>Manages the rendering of chat into a new window.</p>
 *
 * @author Ocelot
 */
@Mod.EventBusSubscriber(modid = DetachableChat.MOD_ID, value = Dist.CLIENT)
public class ChatWindowRenderer
{
    private static final List<IReorderingProcessor> RENDER_LINES = new LinkedList<>();
    private static final Screen CHAT_SCREEN = new ChatBoxScreen();
    private static final ScrollHandler SCROLL_HANDLER = new ScrollHandler(0, 0).setScrollSpeed(9);
    private static int fbo;
    private static int colorBuffer;
    private static int depthBuffer;
    private static double lastChatLineSpacing;

    /**
     * Updates the size of the color and depth buffers.
     *
     * @param width  The new width of the window
     * @param height The new height of the window
     */
    public static void updateSize(int width, int height)
    {
        if (colorBuffer == 0)
            colorBuffer = GlStateManager.genTexture();
        if (depthBuffer == 0)
            depthBuffer = glGenRenderbuffers();

        GlStateManager.bindTexture(colorBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
        GlStateManager.bindTexture(0);

        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        CHAT_SCREEN.init(Minecraft.getInstance(), ChatWindow.getScaledWidth(), ChatWindow.getScaledHeight());
        SCROLL_HANDLER.setVisibleHeight(ChatWindow.getScaledHeight() - 48);
    }

    /**
     * Attaches the buffers to the frame buffer.
     */
    public static void attach()
    {
        if (fbo == 0)
            fbo = GlStateManager.genFramebuffers();
        GlStateManager.bindFramebuffer(FramebufferConstants.GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(FramebufferConstants.GL_FRAMEBUFFER, FramebufferConstants.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);
        glFramebufferRenderbuffer(FramebufferConstants.GL_FRAMEBUFFER, FramebufferConstants.GL_DEPTH_ATTACHMENT, FramebufferConstants.GL_RENDERBUFFER, depthBuffer);
        int i = GlStateManager.checkFramebufferStatus(FramebufferConstants.GL_FRAMEBUFFER);
        if (i != FramebufferConstants.GL_FRAMEBUFFER_COMPLETE)
        {
            if (i == FramebufferConstants.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            if (i == FramebufferConstants.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            if (i == FramebufferConstants.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            if (i == FramebufferConstants.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER)
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
        }
    }

    /**
     * Binds the framebuffer.
     */
    public static void bind()
    {
        GlStateManager.bindFramebuffer(FramebufferConstants.GL_FRAMEBUFFER, fbo);
    }

    /**
     * Binds the framebuffer texture.
     */
    public static void bindTexture()
    {
        glBindTexture(GL_TEXTURE_2D, colorBuffer);
    }

    /**
     * Syncs the render lines with the chat text.
     */
    public static void updateText()
    {
        Minecraft mc = Minecraft.getInstance();
        RENDER_LINES.clear();

        mc.ingameGUI.getChatGUI().chatLines.stream().flatMap(line ->
        {
            List<IReorderingProcessor> processors = RenderComponentsUtil.func_238505_a_(line.getLineString(), ChatWindow.getScaledWidth() - 4 * ChatWindow.getGuiScale(), Minecraft.getInstance().fontRenderer);
            Collections.reverse(processors);
            return processors.stream();
        }).forEach(RENDER_LINES::add);

        while (RENDER_LINES.size() > 100)
        {
            RENDER_LINES.remove(RENDER_LINES.size() - 1);
        }

        SCROLL_HANDLER.setHeight((int) (RENDER_LINES.size() * 9.0D * (mc.gameSettings.chatLineSpacing + 1.0D)) - 9);
    }

    /**
     * Frees the framebuffer resources.
     */
    public static void free()
    {
        if (fbo != 0)
        {
            GlStateManager.deleteFramebuffers(fbo);
            fbo = 0;
        }
        if (colorBuffer != 0)
        {
            GlStateManager.deleteTexture(colorBuffer);
            colorBuffer = 0;
        }
        if (depthBuffer != 0)
        {
            glDeleteRenderbuffers(depthBuffer);
            depthBuffer = 0;
        }
    }

    /**
     * Updates the chat window state.
     */
    public static void tick()
    {
        if (colorBuffer != 0 && depthBuffer != 0)
        {
            GameSettings gameSettings = Minecraft.getInstance().gameSettings;
            if (gameSettings.chatLineSpacing != lastChatLineSpacing)
            {
                SCROLL_HANDLER.setHeight((int) (RENDER_LINES.size() * 9.0D * (gameSettings.chatLineSpacing + 1.0D)) - 9);
                lastChatLineSpacing = gameSettings.chatLineSpacing;
            }

            SCROLL_HANDLER.update();
            CHAT_SCREEN.tick();
        }
    }

    /**
     * Renders the chat screen into the window.
     *
     * @param matrixStack  The stack of transformations
     * @param scaledWidth  The scaled width of the window
     * @param scaledHeight The scaled height of the window
     * @param mouseX       The scaled x position of the mouse
     * @param mouseY       The scaled y position of the mouse
     * @param partialTicks The percentage from last update to this update
     */
    @SuppressWarnings("deprecation")
    public static void render(MatrixStack matrixStack, int scaledWidth, int scaledHeight, int mouseX, int mouseY, float partialTicks)
    {
        AbstractGui.fill(matrixStack, 0, 0, scaledWidth, scaledHeight, 0xFFC6C6C6);
        RenderSystem.enableDepthTest();
        RenderSystem.pushMatrix();
        RenderSystem.translatef(0, scaledHeight - 48, -10);

        Minecraft mc = Minecraft.getInstance();
        int height = scaledHeight - 48;
        float scrollPos = Math.max(0, SCROLL_HANDLER.getInterpolatedScroll(partialTicks));
        double d0 = Minecraft.getInstance().gameSettings.chatScale;

        int i = (int) Math.ceil(height / 9.0) + 2;
        int j = RENDER_LINES.size();
        Minecraft.getInstance().ingameGUI.getChatGUI().func_238498_k_();
        if (j > 0)
        {
            int k = MathHelper.ceil((double) scaledWidth / d0);
            RenderSystem.pushMatrix();
            RenderSystem.translatef(2.0F, 8.0F, 0.0F);
            RenderSystem.scaled(d0, d0, 1.0D);
            double d1 = mc.gameSettings.chatOpacity * (double) 0.9F + (double) 0.1F;
            double d2 = mc.gameSettings.accessibilityTextBackgroundOpacity;
            double d3 = 9.0D * (mc.gameSettings.chatLineSpacing + 1.0D);
            double d4 = -8.0D * (mc.gameSettings.chatLineSpacing + 1.0D) + 4.0D * mc.gameSettings.chatLineSpacing;

            glEnable(GL_SCISSOR_TEST);
            glScissor(0, 40 * ChatWindow.getGuiScale(), ChatWindow.getWidth(), ChatWindow.getHeight() - 40 * ChatWindow.getGuiScale());
            for (int i1 = 0; i1 + scrollPos / d3 < RENDER_LINES.size() && i1 < i; ++i1)
            {
                IReorderingProcessor chatline = RENDER_LINES.get((int) (i1 + scrollPos / d3));
                if (chatline != null)
                {
                    int l1 = (int) (255.0D * d1);
                    int i2 = (int) (255.0D * d2);
                    if (l1 > 3)
                    {
                        double d6 = (double) (-i1) * d3;
                        matrixStack.push();
                        matrixStack.translate(0.0D, scrollPos % d3, 10.0D);
                        AbstractGui.fill(matrixStack, -2, (int) (d6 - d3), k + 4, (int) d6, i2 << 24);
                        RenderSystem.enableBlend();
                        matrixStack.translate(0.0D, 0.0D, 10.0D);
                        mc.fontRenderer.func_238407_a_(matrixStack, chatline, 0.0F, (float) ((int) (d6 + d4)), 16777215 + (l1 << 24));
                        RenderSystem.disableAlphaTest();
                        RenderSystem.disableBlend();
                        matrixStack.pop();
                    }
                }
            }
            glDisable(GL_SCISSOR_TEST);

            if (!mc.ingameGUI.getChatGUI().field_238489_i_.isEmpty())
            {
                int k2 = (int) (128.0D * d1);
                int i3 = (int) (255.0D * d2);
                matrixStack.push();
                matrixStack.translate(0.0D, 0.0D, 50.0D);
                AbstractGui.fill(matrixStack, -2, 0, k + 4, 9, i3 << 24);
                RenderSystem.enableBlend();
                matrixStack.translate(0.0D, 0.0D, 50.0D);
                mc.fontRenderer.func_243246_a(matrixStack, new TranslationTextComponent("chat.queue", mc.ingameGUI.getChatGUI().field_238489_i_.size()), 0.0F, 1.0F, 16777215 + (k2 << 24));
                matrixStack.pop();
                RenderSystem.disableAlphaTest();
                RenderSystem.disableBlend();
            }

            RenderSystem.popMatrix();
        }

        RenderSystem.popMatrix();
        matrixStack.push();
        matrixStack.translate(0, 0, 50);
        CHAT_SCREEN.render(matrixStack, mouseX, mouseY, 1.0F);
        matrixStack.push();
        RenderSystem.disableDepthTest();
    }

    /**
     * Fetches the hovered line of text at the specified position.
     *
     * @param mouseX       The x position of the mouse
     * @param mouseY       The y position of the mouse
     * @param partialTicks The percentage from last update and this update
     * @return The style of text at the hovered position or <code>null</code> if the text has no style
     */
    @Nullable
    public static Style getHoveredComponent(double mouseX, double mouseY, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();
        float scrollPos = Math.max(0, ChatWindowRenderer.getScrollHandler().getInterpolatedScroll(partialTicks));
        double d0 = mouseX - 2.0D;
        double d1 = ChatWindow.getScaledHeight() - mouseY - 40.0D;
        d0 = MathHelper.floor(d0 / ChatWindow.getGuiScale());
        d1 = MathHelper.floor(d1 / (ChatWindow.getGuiScale() * (mc.gameSettings.chatLineSpacing + 1.0D)));
        if (!(d0 < 0.0D) && !(d1 < 0.0D))
        {
            int i = Math.min((int) Math.ceil((ChatWindow.getScaledHeight() - 48) / 9.0) + 2, RENDER_LINES.size());
            if (d0 <= (double) MathHelper.floor((double) ChatWindow.getScaledWidth() / ChatWindow.getGuiScale()) && d1 < (double) (9 * i + i))
            {
                int j = (int) (d1 / 9.0D + (double) scrollPos);
                if (j >= 0 && j < RENDER_LINES.size())
                {
                    return mc.fontRenderer.getCharacterManager().func_243239_a(RENDER_LINES.get(j), (int) d0);
                }
            }
        }
        return null;
    }

    /**
     * @return The GUI event listener
     */
    public static Screen getListener()
    {
        return CHAT_SCREEN;
    }

    /**
     * @return The scrolling manager
     */
    public static ScrollHandler getScrollHandler()
    {
        return SCROLL_HANDLER;
    }
}
