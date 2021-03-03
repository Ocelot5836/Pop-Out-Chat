package io.github.ocelot.popoutchat.client;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>Abstract specification for a config.</p>
 *
 * @author Ocelot
 */
public interface Config
{
    /**
     * Fetches the config for the specified entry.
     *
     * @param entry The config entry to get
     * @param <T>   The type of object to return
     * @return The value of that config
     */
    <T> T get(Entry entry);

    /**
     * Sets the config for the specified entry.
     *
     * @param entry The config entry to set
     * @param value The new value for that config entry
     * @param <T>   The type of object to set
     */
    <T> void set(Entry entry, T value);

    /**
     * <p>Fields in the config file that can be changed.</p>
     *
     * @author Ocelot
     */
    enum Entry
    {
        DECORATED_WINDOW(() -> true, Boolean::valueOf), WINDOW_ALPHA(() -> 1.0F, Float::parseFloat);

        private final Supplier<?> defaultFactory;
        private final Function<String, Object> fromString;

        Entry(Supplier<?> defaultFactory, Function<String, Object> fromString)
        {
            this.defaultFactory = defaultFactory;
            this.fromString = fromString;
        }

        public Object parseStringValue(String value)
        {
            return this.fromString.apply(value);
        }

        /**
         * @return The default value of this config entry
         */
        public Object getDefault()
        {
            return this.defaultFactory.get();
        }
    }
}
