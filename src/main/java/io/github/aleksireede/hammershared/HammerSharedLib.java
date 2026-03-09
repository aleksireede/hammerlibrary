package io.github.aleksireede.hammershared;

import org.bukkit.plugin.java.JavaPlugin;

public final class HammerSharedLib extends JavaPlugin {

    @Override
    public void onEnable() {
        SharedItemKeys.init(this);
        SharedItemUpdater.init(this);
        getLogger().info("HammerSharedLib enabled — shared item text and resource pack utilities are ready.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HammerSharedLib disabled.");
    }
}
