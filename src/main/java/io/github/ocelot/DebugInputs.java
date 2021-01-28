package io.github.ocelot;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_COMPONENTS;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;

/**
 * @author Ocelot
 */
@Mod.EventBusSubscriber(modid = PopoutChat.MOD_ID)
public class DebugInputs
{
    @SubscribeEvent
    public static void onEvent(InputEvent.KeyInputEvent event)
    {
        if (FMLLoader.isProduction() || Minecraft.getInstance().currentScreen != null)
            return;
        if (event.getKey() == GLFW.GLFW_KEY_P)
        {
            try
            {
                Path outputFolder = Paths.get(Minecraft.getInstance().gameDir.toURI()).resolve("debug-out");
                if (!Files.exists(outputFolder))
                    Files.createDirectories(outputFolder);

                for (int i = 0; i < 1024; i++)
                {
                    if (!glIsTexture(i))
                        continue;

                    Path outputFile = outputFolder.resolve(i + ".png");
                    if (!Files.exists(outputFile))
                        Files.createFile(outputFile);

                    glBindTexture(GL_TEXTURE_2D, i);

                    int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
                    int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
                    int components = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_COMPONENTS);
                    int componentsCount = components == GL_RGB ? 3 : 4;

                    ByteBuffer image = BufferUtils.createByteBuffer(width * height * componentsCount);
                    glGetTexImage(GL_TEXTURE_2D, 0, components, GL_UNSIGNED_BYTE, image);

                    Util.getRenderingService().execute(() -> stbi_write_png(outputFile.toString(), width, height, componentsCount, image, 0));
                }
                Util.getOSType().openFile(outputFolder.toFile());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
