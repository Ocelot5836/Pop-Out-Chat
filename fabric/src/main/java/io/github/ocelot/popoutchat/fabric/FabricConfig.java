package io.github.ocelot.popoutchat.fabric;

import io.github.ocelot.popoutchat.PopoutChat;
import io.github.ocelot.popoutchat.client.Config;
import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Properties;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * <p>Fabric implementation of {@link Config}.</p>
 *
 * @author Ocelot
 */
public class FabricConfig implements Config
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path location;
    private final Properties properties;

    @SuppressWarnings("unchecked")
    public FabricConfig(Path location)
    {
        this.location = location.resolve(PopoutChat.MOD_ID + "-fabric.properties");
        this.properties = new Properties();
        this.readConfig();

        try
        {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            WatchKey key = location.register(watcher, ENTRY_MODIFY);
            Thread watchThread = new Thread(() ->
            {
                while (true)
                {
                    for (WatchEvent<?> event : key.pollEvents())
                    {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == OVERFLOW)
                            continue;

                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path file = ev.context();

                        if (!this.location.getFileName().equals(file))
                            continue;

                        LOGGER.debug("Detected file system changes, reloading config.");
                        Util.backgroundExecutor().execute(this::readConfig);
                    }

                    if (!key.reset())
                        return;
                }
            }, PopoutChat.MOD_ID + " Fabric Config Watcher");
            watchThread.setDaemon(true);
            watchThread.start();
            LOGGER.debug("Started " + watchThread.getName());
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to create watch service. Config will not be automatically reloaded.", e);
        }
    }

    private synchronized void readConfig()
    {
        for (Config.Entry entry : Config.Entry.values())
            this.properties.setProperty(entry.name().toLowerCase(Locale.ROOT), String.valueOf(entry.getDefault()));
        if (!Files.exists(this.location) || !Files.isRegularFile(this.location))
        {
            this.writeConfig();
            return;
        }
        try (FileInputStream stream = new FileInputStream(this.location.toFile()))
        {
            this.properties.load(stream);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to read config from '" + this.location + "'", e);
        }
    }

    private synchronized void writeConfig()
    {
        try
        {
            if (!Files.exists(this.location) || !Files.isRegularFile(this.location))
            {
                Files.deleteIfExists(this.location);
                Files.createDirectories(this.location.getParent());
                Files.createFile(this.location);
            }
            try (FileWriter writer = new FileWriter(this.location.toFile()))
            {
                this.properties.store(writer, "Fabric config values for " + PopoutChat.MOD_ID);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to write config to '" + this.location + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Entry entry)
    {
        String key = entry.name().toLowerCase(Locale.ROOT);
        if (!this.properties.containsKey(key))
            return (T) entry.getDefault();
        try
        {
            return (T) entry.parseStringValue(this.properties.getProperty(key));
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to parse config value: " + key + ". Resetting to default value.", e);
            this.set(entry, entry.getDefault());
            return (T) entry.getDefault();
        }
    }

    @Override
    public <T> void set(Entry entry, T value)
    {
        this.properties.setProperty(entry.name().toLowerCase(Locale.ROOT), String.valueOf(value));
        this.writeConfig();
    }
}
