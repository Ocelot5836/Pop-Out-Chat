package io.github.ocelot.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ocelot.DetachableChat;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * <p>Manages the main window for chat to appear in.</p>
 *
 * @author Ocelot
 */
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
    private static boolean focused;
    private static double mouseX;
    private static double mouseY;

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

    private static void setWindowSize(int width, int height)
    {
        framebufferWidth = width;
        framebufferHeight = height;
        guiScale = calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().gameSettings.forceUnicodeFont);
        scaledWidth = width / guiScale;
        scaledHeight = height / guiScale;
        ChatWindowRenderer.updateSize(framebufferWidth, framebufferHeight);
        ChatWindowRenderer.updateText();
    }

    /**
     * Opens the chat window.
     */
    public static void create()
    {
        if (handle != NULL)
        {
            LOGGER.error("Only one Chat Window is supported");
            return;
        }
        if (Minecraft.getInstance().player == null)
        {
            LOGGER.warn("No chat is present");
            return;
        }

        MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        handle = glfwCreateWindow(640, 480, "Test", NULL, mainWindow.getHandle());
        if (handle == NULL)
            throw new RuntimeException("Failed to create Chat Window");

        closing = false;
        focused = false;
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            glfwGetCursorPos(handle, x, y);
            mouseX = x.get();
            mouseY = y.get();
        }

        glfwSetFramebufferSizeCallback(handle, (window, width, height) -> setWindowSize(width, height));
        glfwSetWindowCloseCallback(handle, window -> closing = true);
        glfwSetWindowFocusCallback(handle, (window, f) -> focused = f);
        glfwSetKeyCallback(handle, (window, key, scancode, action, mods) ->
        {
            switch (action)
            {
                case GLFW_PRESS:
                case GLFW_REPEAT:
                    ChatWindowRenderer.getListener().keyPressed(key, scancode, mods);
                    break;
                case GLFW_RELEASE:
                    ChatWindowRenderer.getListener().keyReleased(key, scancode, mods);
                    break;
            }
            Minecraft.getInstance().setGameFocused(true);
        });
        glfwSetCharModsCallback(handle, (window, codepoint, mods) ->
        {
            Screen iguieventlistener = ChatWindowRenderer.getListener();
            if (Character.charCount(codepoint) == 1)
            {
                Screen.wrapScreenError(() ->
                {
                    if (ForgeHooksClient.onGuiCharTypedPre(iguieventlistener, (char) codepoint, mods))
                        return;
                    if (iguieventlistener.charTyped((char) codepoint, mods))
                        return;
                    ForgeHooksClient.onGuiCharTypedPost(iguieventlistener, (char) codepoint, mods);
                }, "charTyped event handler", iguieventlistener.getClass().getCanonicalName());
            }
            else
            {
                for (char c0 : Character.toChars(codepoint))
                {
                    Screen.wrapScreenError(() ->
                    {
                        if (ForgeHooksClient.onGuiCharTypedPre(iguieventlistener, c0, mods))
                            return;
                        if (iguieventlistener.charTyped(c0, mods))
                            return;
                        ForgeHooksClient.onGuiCharTypedPost(iguieventlistener, c0, mods);
                    }, "charTyped event handler", iguieventlistener.getClass().getCanonicalName());
                }
            }
            Minecraft.getInstance().setGameFocused(true);
        });
        glfwSetCursorPosCallback(handle, (window, xpos, ypos) ->
        {
            mouseX = xpos;
            mouseY = ypos;
            ChatWindowRenderer.getListener().mouseMoved(xpos, ypos);
        });
        glfwSetMouseButtonCallback(handle, (window, button, action, mods) ->
        {
            switch (action)
            {
                case GLFW_PRESS:
                    ChatWindowRenderer.getListener().mouseClicked(mouseX, mouseY, button);
                    break;
                case GLFW_RELEASE:
                    ChatWindowRenderer.getListener().mouseReleased(mouseX, mouseY, button);
                    break;
            }
            Minecraft.getInstance().setGameFocused(true);
        });

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        RenderSystem.setupDefaultState(0, 0, framebufferWidth, framebufferHeight);
        glfwMakeContextCurrent(mainWindow.getHandle());

        setWindowSize(640, 480);
        ChatWindowRenderer.attach();
    }

    /**
     * Destroys the chat window and all resources associated with it.
     */
    public static void destroy()
    {
        if (handle == NULL)
            return;

        ChatWindowRenderer.free();
        glfwDestroyWindow(handle);
        handle = NULL;
    }

    @SuppressWarnings("deprecation")
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
        ChatWindowRenderer.render(matrixStack, scaledWidth, scaledHeight, (int) (mouseX / guiScale), (int) (mouseY / guiScale), focused);
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

    @SubscribeEvent
    public static void onEvent(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        destroy();
    }

    /**
     * @return The scaled width of the screen
     */
    public static int getScaledWidth()
    {
        return scaledWidth;
    }

    /**
     * @return The scaled height of the screen
     */
    public static int getScaledHeight()
    {
        return scaledHeight;
    }

    /**
     * @return The scale factor for the GUI
     */
    public static int getGuiScale()
    {
        return guiScale;
    }
}
