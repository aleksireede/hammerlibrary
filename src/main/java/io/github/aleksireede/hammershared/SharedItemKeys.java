package io.github.aleksireede.hammershared;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Centralised NamespacedKey definitions shared by all hammer-ecosystem plugins.
 * Initialised once by {@link HammerSharedLib#onEnable()}, but falls back to a
 * live Bukkit lookup if {@code init()} has not yet been called (e.g. if the
 * enabling order differs from the declared load order).
 */
public final class SharedItemKeys {
    private static JavaPlugin plugin;

    private SharedItemKeys() {}

    static void init(final JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    private static JavaPlugin resolvePlugin() {
        if (plugin == null) {
            plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("HammerSharedLib");
        }
        if (plugin == null) {
            throw new IllegalStateException(
                "HammerSharedLib is not loaded. Ensure it is installed and enabled before dependent plugins.");
        }
        return plugin;
    }

    public static NamespacedKey customIdKey() {
        return new NamespacedKey(resolvePlugin(), "custom_id");
    }

    public static NamespacedKey rarityKey() {
        return new NamespacedKey(resolvePlugin(), "rarity");
    }

    public static NamespacedKey itemTypeKey() {
        return new NamespacedKey(resolvePlugin(), "item_type");
    }

    public static NamespacedKey healthKey() {
        return new NamespacedKey(resolvePlugin(), "health");
    }
}
