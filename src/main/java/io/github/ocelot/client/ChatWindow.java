package io.github.ocelot.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ocelot.DetachableChat;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

@Mod.EventBusSubscriber(modid = DetachableChat.MOD_ID, value = Dist.CLIENT)
public final class ChatWindow
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static long handle;
    private static int framebufferWidth;
    private static int framebufferHeight;
    private static int scaledWidth;
    private static int scaledHeight;
    private static int guiScale;
    private static boolean closing;

    private ChatWindow()
    {
    }

    private static int calcGuiScale(int guiScaleIn, boolean forceUnicode)
    {
        int i;
        for (i = 1; i != guiScaleIn && i < framebufferWidth && i < framebufferHeight && framebufferWidth / (i + 1) >= 320 && framebufferHeight / (i + 1) >= 240; ++i)
        {
        }

        if (forceUnicode && i % 2 != 0)
        {
            ++i;
        }

        return i;
    }

    public static void create()
    {
        if (handle != NULL)
        {
            LOGGER.error("Only one Chat Window is supported");
            return;
        }

        MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        handle = glfwCreateWindow(640, 480, "Test", NULL, mainWindow.getHandle());
        if (handle == NULL)
            throw new RuntimeException("Failed to create Chat Window");

        closing = false;
        glfwSetFramebufferSizeCallback(handle, (window, width, height) -> setWindowSize(width, height));
        glfwSetWindowCloseCallback(handle, window -> closing = true);

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        RenderSystem.setupDefaultState(0, 0, framebufferWidth, framebufferHeight);
        glfwMakeContextCurrent(mainWindow.getHandle());

        setWindowSize(640, 480);
        ChatWindowRenderer.attach();
    }

    private static void setWindowSize(int width, int height)
    {
        framebufferWidth = width;
        framebufferHeight = height;
        guiScale = calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().gameSettings.forceUnicodeFont);
        scaledWidth = width / guiScale;
        scaledHeight = height / guiScale;
        ChatWindowRenderer.updateSize(framebufferWidth, framebufferHeight);
    }

    public static void destroy()
    {
        if (handle == NULL)
            return;

        ChatWindowRenderer.free();
        glfwDestroyWindow(handle);
        handle = NULL;
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event)
    {
        if (handle == NULL || event.phase == TickEvent.Phase.START)
            return;

        if (closing)
        {
            destroy();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        MatrixStack matrixStack = new MatrixStack();

        ChatWindowRenderer.bind();
        GlStateManager.viewport(0, 0, framebufferWidth, framebufferHeight);
        GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, Minecraft.IS_RUNNING_ON_MAC);
        RenderSystem.matrixMode(GL_PROJECTION);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, scaledWidth, scaledHeight, 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(GL_MODELVIEW);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        RenderHelper.setupGui3DDiffuseLighting();
        ChatWindowRenderer.render(matrixStack, scaledWidth, scaledHeight, mc.ingameGUI.getChatGUI().chatLines.stream().flatMap(line ->
        {
            List<IReorderingProcessor> processors = RenderComponentsUtil.func_238505_a_(line.getLineString(), scaledWidth - 4 * guiScale, mc.fontRenderer);
            Collections.reverse(processors);
            return processors.stream();
        }).collect(Collectors.toList()));
        mc.getFramebuffer().bindFramebuffer(true);

        glFinish();
        glfwMakeContextCurrent(handle);
        GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, Minecraft.IS_RUNNING_ON_MAC);
        GlStateManager.viewport(0, 0, framebufferWidth, framebufferHeight);

        ChatWindowRenderer.bindTexture();
        glEnable(GL_TEXTURE_2D);
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(-1, -1, 0).tex(0, 0).endVertex();
        builder.pos(-1, 1, 0).tex(0, 1).endVertex();
        builder.pos(1, 1, 0).tex(1, 1).endVertex();
        builder.pos(1, -1, 0).tex(1, 0).endVertex();
        Tessellator.getInstance().draw();

        glfwSwapBuffers(handle);
        glfwMakeContextCurrent(Minecraft.getInstance().getMainWindow().getHandle());
    }
}
