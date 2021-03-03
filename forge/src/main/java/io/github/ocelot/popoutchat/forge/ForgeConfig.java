package io.github.ocelot.popoutchat.forge;

import io.github.ocelot.popoutchat.client.Config;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;

/**
 * <p>Forge implementation of {@link Config}.</p>
 *
 * @author Ocelot
 */
public class ForgeConfig implements Config
{
    private final ForgeConfigSpec.BooleanValue decoratedWindow;
    private final ForgeConfigSpec.IntValue windowAlpha;

    public ForgeConfig(ForgeConfigSpec.Builder builder)
    {
        this.decoratedWindow = builder.comment("Whether or not to use a window with a border and header.").define(Entry.DECORATED_WINDOW.name().toLowerCase(Locale.ROOT), true);
        this.windowAlpha = builder.comment("The transparency for the background of the window.").defineInRange(Entry.WINDOW_ALPHA.name().toLowerCase(Locale.ROOT), 255, 0, 255);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Entry entry)
    {
        switch (entry)
        {
            case DECORATED_WINDOW:
                return (T) this.decoratedWindow.get();
            case WINDOW_ALPHA:
                return (T) new Float(this.windowAlpha.get().floatValue() / 255F);
            default:
                throw new AssertionError();
        }
    }

    @Override
    public <T> void set(Entry entry, T value)
    {
        switch (entry)
        {
            case DECORATED_WINDOW:
                this.decoratedWindow.set((Boolean) value);
                break;
            case WINDOW_ALPHA:
                this.windowAlpha.set(((int) ((Float) value * 255)) & 0xFF);
                break;
            default:
                throw new AssertionError();
        }
    }
}
