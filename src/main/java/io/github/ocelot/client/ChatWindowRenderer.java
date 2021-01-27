package io.github.ocelot.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ocelot.DetachableChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.FramebufferConstants;
import net.minecraft.util.IReorderingProcessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    private static final ChatScreen CHAT_SCREEN = new ChatScreen("");
    private static int fbo;
    private static int colorBuffer;
    private static int depthBuffer;

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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
        GlStateManager.bindTexture(0);

        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        CHAT_SCREEN.init(Minecraft.getInstance(), ChatWindow.getScaledWidth(), ChatWindow.getScaledHeight());
    }

    /**
     * Attaches the buffers to the frame buffer.
     */
    public static void attach()
    {
        if (fbo == 0)
            fbo = GlStateManager.genFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
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
        GlStateManager.bindFramebuffer(GL_FRAMEBUFFER, fbo);
    }

    /**
     * Binds the framebuffer texture.
     */
    public static void bindTexture()
    {
        glBindTexture(GL_TEXTURE_2D, colorBuffer);
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

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && colorBuffer != 0 && depthBuffer != 0)
            CHAT_SCREEN.tick();
    }

    /**
     * Renders the chat screen into the window.
     *
     * @param matrixStack  The stack of transformations
     * @param scaledWidth  The scaled width of the window
     * @param scaledHeight The scaled height of the window
     * @param mouseX       The scaled x position of the mouse
     * @param mouseY       The scaled y position of the mouse
     * @param focused      Whether or not the window is focused
     */
    public static void render(MatrixStack matrixStack, int scaledWidth, int scaledHeight, int mouseX, int mouseY, boolean focused)
    {
        AbstractGui.fill(matrixStack, 0, 0, scaledWidth, scaledHeight, 0xFFC6C6C6);
        RenderSystem.pushMatrix();
        RenderSystem.translatef(0, scaledHeight - 48, 0);
        Minecraft.getInstance().ingameGUI.getChatGUI().func_238492_a_(matrixStack, Minecraft.getInstance().ingameGUI.getTicks());
        RenderSystem.popMatrix();
        CHAT_SCREEN.render(matrixStack, mouseX, mouseY, 1.0F);

//        Minecraft mc = Minecraft.getInstance();
//
//        AbstractGui.fill(matrixStack, 0, 0, scaledWidth, scaledHeight, 0xFFC6C6C6);
//        double d0 = mc.gameSettings.chatScale;
//        int k = MathHelper.ceil((double) scaledWidth / d0);
//        RenderSystem.pushMatrix();
//        RenderSystem.translatef(2.0F, scaledHeight, 0.0F);
//        RenderSystem.scaled(d0, d0, 1.0D);
//        double d1 = mc.gameSettings.chatOpacity * (double) 0.9F + (double) 0.1F;
//        double d2 = mc.gameSettings.accessibilityTextBackgroundOpacity;
//        double d3 = 9.0D * (mc.gameSettings.chatLineSpacing + 1.0D);
//        double d4 = -8.0D * (mc.gameSettings.chatLineSpacing + 1.0D) + 4.0D * mc.gameSettings.chatLineSpacing;
//
//        for (int i1 = 0; i1 < RENDER_LINES.size(); i1++)
//        {
//            IReorderingProcessor chatline = RENDER_LINES.get(i1);
//            if (chatline != null)
//            {
//                int l1 = (int) (255.0D * d1);
//                int i2 = (int) (255.0D * d2);
//                if (l1 > 3)
//                {
//                    double d6 = (double) (-i1) * d3;
//                    matrixStack.push();
//                    matrixStack.translate(0.0D, 0.0D, 50.0D);
//                    AbstractGui.fill(matrixStack, -2, (int) (d6 - d3), k + 4, (int) d6, i2 << 24);
//                    RenderSystem.enableBlend();
//                    matrixStack.translate(0.0D, 0.0D, 50.0D);
//                    mc.fontRenderer.func_238407_a_(matrixStack, chatline, 0.0F, (float) ((int) (d6 + d4)), 16777215 + (l1 << 24));
//                    RenderSystem.disableAlphaTest();
//                    RenderSystem.disableBlend();
//                    matrixStack.pop();
//                }
//            }
//        }
//
//        RenderSystem.popMatrix();
//

    }

    /**
     * Syncs the render lines with the chat text.
     */
    public static void updateText()
    {
        RENDER_LINES.clear();

        Minecraft.getInstance().ingameGUI.getChatGUI().chatLines.stream().flatMap(line ->
        {
            List<IReorderingProcessor> processors = RenderComponentsUtil.func_238505_a_(line.getLineString(), ChatWindow.getScaledWidth() - 4 * ChatWindow.getGuiScale(), Minecraft.getInstance().fontRenderer);
            Collections.reverse(processors);
            return processors.stream();
        }).forEach(RENDER_LINES::add);

        while (RENDER_LINES.size() > 100)
        {
            RENDER_LINES.remove(RENDER_LINES.size() - 1);
        }
    }

    /**
     * @return The GUI event listener
     */
    public static Screen getListener()
    {
        return CHAT_SCREEN;
    }
}
