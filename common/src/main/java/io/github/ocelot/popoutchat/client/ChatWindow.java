package io.github.ocelot.popoutchat.client;

import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import io.github.ocelot.popoutchat.PopoutChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * <p>Manages the main window for chat to appear in.</p>
 *
 * @author Ocelot
 */
public final class ChatWindow
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static long handle;
    private static int framebufferWidth;
    private static int framebufferHeight;
    private static int scaledWidth;
    private static int scaledHeight;
    private static int guiScale;
    private static int lastGuiScaleSetting;
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
        guiScale = calcGuiScale(Minecraft.getInstance().options.guiScale, Minecraft.getInstance().options.forceUnicodeFont);
        scaledWidth = width / guiScale;
        scaledHeight = height / guiScale;
        ChatWindowRenderer.updateSize(framebufferWidth, framebufferHeight);
        ChatWindowRenderer.updateText();
    }

    private static void setWindowIcon()
    {
        VanillaPackResources vanillaPack = Minecraft.getInstance().getClientPackSource().getVanillaPack();
        try (MemoryStack memorystack = MemoryStack.stackPush())
        {
            InputStream iconStream16X = vanillaPack.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_16x16.png"));
            InputStream iconStream32X = vanillaPack.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_32x32.png"));

            IntBuffer intbuffer = memorystack.mallocInt(1);
            IntBuffer intbuffer1 = memorystack.mallocInt(1);
            IntBuffer intbuffer2 = memorystack.mallocInt(1);
            GLFWImage.Buffer buffer = GLFWImage.mallocStack(2, memorystack);
            ByteBuffer bytebuffer = loadIcon(iconStream16X, intbuffer, intbuffer1, intbuffer2);
            if (bytebuffer == null)
            {
                throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
            }

            buffer.position(0);
            buffer.width(intbuffer.get(0));
            buffer.height(intbuffer1.get(0));
            buffer.pixels(bytebuffer);
            ByteBuffer bytebuffer1 = loadIcon(iconStream32X, intbuffer, intbuffer1, intbuffer2);
            if (bytebuffer1 == null)
            {
                throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
            }

            buffer.position(1);
            buffer.width(intbuffer.get(0));
            buffer.height(intbuffer1.get(0));
            buffer.pixels(bytebuffer1);
            buffer.position(0);
            glfwSetWindowIcon(handle, buffer);
            STBImage.stbi_image_free(bytebuffer);
            STBImage.stbi_image_free(bytebuffer1);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to set icon", e);
        }
    }

    private static ByteBuffer loadIcon(InputStream textureStream, IntBuffer x, IntBuffer y, IntBuffer channelInFile) throws IOException
    {
        RenderSystem.assertThread(RenderSystem::isInInitPhase);
        ByteBuffer bytebuffer = null;

        ByteBuffer bytebuffer1;
        try
        {
            bytebuffer = TextureUtil.readResource(textureStream);
            bytebuffer.rewind();
            bytebuffer1 = STBImage.stbi_load_from_memory(bytebuffer, x, y, channelInFile, 0);
        }
        finally
        {
            if (bytebuffer != null)
            {
                MemoryUtil.memFree(bytebuffer);
            }
        }

        return bytebuffer1;
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

        Window mainWindow = Minecraft.getInstance().getWindow();
        double windowWidth = ChatComponent.getWidth(1.0F) * mainWindow.getGuiScale();
        double windowHeight = 3.0 * windowWidth / 4.0;
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        handle = glfwCreateWindow((int) windowWidth, (int) windowHeight, I18n.get("title." + PopoutChat.MOD_ID + ".chat"), NULL, mainWindow.getWindow());
        if (handle == NULL)
            throw new RuntimeException("Failed to create Chat Window");

        setWindowIcon();

        /* Set position to center of main window */
        if (!mainWindow.isFullscreen())
        {
            try (MemoryStack memoryStack = MemoryStack.stackPush())
            {
                IntBuffer x = memoryStack.callocInt(1);
                IntBuffer y = memoryStack.callocInt(1);
                glfwGetWindowPos(mainWindow.getWindow(), x, y);
                glfwSetWindowPos(handle, (int) (x.get() + (mainWindow.getWidth() - windowWidth) / 2.0), (int) (y.get() + (mainWindow.getHeight() - windowHeight) / 2.0));
            }
        }

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

        /* Keyboard Input */
        InputConstants.setupKeyboardCallbacks(handle, (window, key, scancode, action, mods) ->
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
        }, (window, codepoint, mods) ->
        {
            Screen iguieventlistener = ChatWindowRenderer.getListener();
            if (Character.charCount(codepoint) == 1)
            {
                Screen.wrapScreenError(() ->
                {
                    if (PopoutChat.onGuiCharTypedPre(iguieventlistener, (char) codepoint, mods))
                        return;
                    if (iguieventlistener.charTyped((char) codepoint, mods))
                        return;
                    PopoutChat.onGuiCharTypedPost(iguieventlistener, (char) codepoint, mods);
                }, "charTyped event handler", iguieventlistener.getClass().getCanonicalName());
            }
            else
            {
                for (char c0 : Character.toChars(codepoint))
                {
                    Screen.wrapScreenError(() ->
                    {
                        if (PopoutChat.onGuiCharTypedPre(iguieventlistener, c0, mods))
                            return;
                        if (iguieventlistener.charTyped(c0, mods))
                            return;
                        PopoutChat.onGuiCharTypedPost(iguieventlistener, c0, mods);
                    }, "charTyped event handler", iguieventlistener.getClass().getCanonicalName());
                }
            }
        });

        /* Mouse Input */
        InputConstants.setupMouseCallbacks(handle, (window, xpos, ypos) ->
        {
            mouseX = xpos;
            mouseY = ypos;
            ChatWindowRenderer.getListener().mouseMoved(xpos, ypos);
        }, (window, button, action, mods) ->
        {
            switch (action)
            {
                case GLFW_PRESS:
                    ChatWindowRenderer.getListener().mouseClicked(mouseX / guiScale, mouseY / guiScale, button);
                    break;
                case GLFW_RELEASE:
                    ChatWindowRenderer.getListener().mouseReleased(mouseX / guiScale, mouseY / guiScale, button);
                    break;
            }
        }, (window, xoffset, yoffset) -> ChatWindowRenderer.getListener().mouseScrolled(mouseX / guiScale, mouseY / guiScale, yoffset), (window, count, names) ->
        {
        });

        /* Close and Focus callbacks */
        glfwSetFramebufferSizeCallback(handle, (window, width, height) -> setWindowSize(width, height));
        glfwSetWindowCloseCallback(handle, window -> closing = true);
        glfwSetWindowFocusCallback(handle, (window, f) -> focused = f);

        /* Setup default state for the chat OpenGL context */
        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        RenderSystem.setupDefaultState(0, 0, framebufferWidth, framebufferHeight);
        glfwMakeContextCurrent(mainWindow.getWindow());

        /* Update listeners with window size */
        setWindowSize((int) windowWidth, (int) windowHeight);
        ChatWindowRenderer.attach();
    }

    /**
     * Destroys the chat window and all resources associated with it.
     */
    public static void destroy()
    {
        if (handle == NULL)
            return;

        PopoutChat.onChatWindowClose();
        ChatWindowRenderer.free();
        glfwDestroyWindow(handle);
        handle = NULL;
    }

    /**
     * Renders the chat into the chat window.
     */
    @SuppressWarnings("deprecation")
    public static void render()
    {
        if (handle == NULL)
            return;

        if (Minecraft.getInstance().player == null || closing)
        {
            destroy();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        PoseStack matrixStack = new PoseStack();

        if (mc.options.guiScale != lastGuiScaleSetting)
        {
            lastGuiScaleSetting = mc.options.guiScale;
            guiScale = calcGuiScale(mc.options.guiScale, mc.options.forceUnicodeFont);
            scaledWidth = framebufferWidth / guiScale;
            scaledHeight = framebufferHeight / guiScale;
            ChatWindowRenderer.getListener().init(Minecraft.getInstance(), scaledWidth, scaledHeight);
        }

        ChatWindowRenderer.bind();
        GlStateManager._viewport(0, 0, framebufferWidth, framebufferHeight);
        GlStateManager._clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.matrixMode(GL_PROJECTION);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, scaledWidth, scaledHeight, 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(GL_MODELVIEW);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        Lighting.setupFor3DItems();
        ChatWindowRenderer.render(matrixStack, scaledWidth, scaledHeight, (int) (mouseX / guiScale), (int) (mouseY / guiScale), Minecraft.getInstance().getFrameTime());
        mc.getMainRenderTarget().bindWrite(true);

        glFinish();
        glfwMakeContextCurrent(handle);
        GlStateManager._clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        GlStateManager._viewport(0, 0, framebufferWidth, framebufferHeight);

        ChatWindowRenderer.bindTexture();
        glEnable(GL_TEXTURE_2D);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(GL_QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1, -1, 0).uv(0, 0).endVertex();
        builder.vertex(-1, 1, 0).uv(0, 1).endVertex();
        builder.vertex(1, 1, 0).uv(1, 1).endVertex();
        builder.vertex(1, -1, 0).uv(1, 0).endVertex();
        Tesselator.getInstance().end();

        glfwSwapBuffers(handle);
        glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());
    }

    /**
     * @return The id of the chat window or <code>NULL</code> if there is no window open
     */
    public static long getHandle()
    {
        return handle;
    }

    /**
     * @return The width of the screen
     */
    public static int getWidth()
    {
        return framebufferWidth;
    }

    /**
     * @return The height of the screen
     */
    public static int getHeight()
    {
        return framebufferHeight;
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

    /**
     * @return Whether or not the chat window is the currently focused window
     */
    public static boolean hasFocus()
    {
        return focused;
    }

    /**
     * @return Whether or not the chat window is opened
     */
    public static boolean isOpen()
    {
        return handle != NULL;
    }
}
