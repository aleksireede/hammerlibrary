package io.github.aleksireede.hammershared;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SharedResourcePackManager {
    private final JavaPlugin plugin;
    private final ResourcePackRequest resourcePackRequest;
    private final boolean sendOnJoin;

    private SharedResourcePackManager(
            final JavaPlugin plugin,
            final ResourcePackRequest resourcePackRequest,
            final boolean sendOnJoin
    ) {
        this.plugin = plugin;
        this.resourcePackRequest = resourcePackRequest;
        this.sendOnJoin = sendOnJoin;
    }

    /**
     * Creates a manager from the {@code shared-resource-pack} section of the given config.
     *
     * <p>The section supports a {@code packs} list so that multiple resource packs can be
     * sent to players in a single request:
     * <pre>
     * shared-resource-pack:
     *   enabled: true
     *   send-on-join: true
     *   packs:
     *     - url: "https://example.com/pack1.zip"
     *       sha1: "aabbcc…"         # 40-char SHA-1
     *       prompt: "Optional prompt"
     *       required: true
     *     - url: "https://example.com/pack2.zip"
     *       sha1: "ddeeff…"
     *       required: false
     * </pre>
     */
    public static SharedResourcePackManager fromConfig(final JavaPlugin plugin, final FileConfiguration config) {
        final ConfigurationSection section = config.getConfigurationSection("shared-resource-pack");
        if (section == null || !section.getBoolean("enabled", false)) {
            return new SharedResourcePackManager(plugin, null, false);
        }

        final List<?> packList = section.getList("packs");
        if (packList == null || packList.isEmpty()) {
            plugin.getLogger().warning("shared-resource-pack is enabled but no packs are configured; skipping resource pack setup.");
            return new SharedResourcePackManager(plugin, null, false);
        }

        final List<ResourcePackInfo> infos = new ArrayList<>();
        Component sharedPrompt = null;

        for (final Object entry : packList) {
            if (!(entry instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            final Map<String, Object> raw = (Map<String, Object>) rawMap;

            final String url = String.valueOf(raw.getOrDefault("url", "")).strip();
            if (url.isBlank()) {
                plugin.getLogger().warning("A pack entry is missing 'url'; skipping it.");
                continue;
            }

            final String hash = String.valueOf(raw.getOrDefault("sha1", "")).strip().toLowerCase(Locale.ROOT);
            if (!hash.matches("^[a-f0-9]{40}$")) {
                plugin.getLogger().warning("Pack entry '" + url + "' has an invalid sha1; skipping it.");
                continue;
            }

            infos.add(ResourcePackInfo.resourcePackInfo().uri(URI.create(url)).hash(hash).build());

            // Use the first pack's prompt as the request-level prompt
            if (sharedPrompt == null) {
                final String prompt = String.valueOf(raw.getOrDefault("prompt", "")).strip();
                if (!prompt.isBlank()) {
                    sharedPrompt = SharedText.miniMessage(prompt);
                }
            }
        }

        if (infos.isEmpty()) {
            plugin.getLogger().warning("shared-resource-pack: no valid pack entries found; skipping resource pack setup.");
            return new SharedResourcePackManager(plugin, null, false);
        }

        final boolean required = section.getBoolean("required", false);
        final ResourcePackRequest.Builder requestBuilder = ResourcePackRequest.resourcePackRequest()
                .packs(infos)
                .required(required);

        if (sharedPrompt != null) {
            requestBuilder.prompt(sharedPrompt);
        }

        return new SharedResourcePackManager(plugin, requestBuilder.build(), section.getBoolean("send-on-join", true));
    }

    /** Returns {@code true} when a valid resource pack request is configured. */
    public boolean isEnabled() {
        return this.resourcePackRequest != null;
    }

    public boolean shouldSendOnJoin() {
        return this.sendOnJoin;
    }

    public void sendToPlayer(final Player player) {
        if (!this.isEnabled()) return;
        player.sendResourcePacks(this.resourcePackRequest);
    }

    public void logState() {
        if (!this.isEnabled()) {
            this.plugin.getLogger().info("Shared resource pack delivery is disabled.");
            return;
        }

        this.plugin.getLogger().info("Shared resource pack delivery is enabled.");
    }
}
