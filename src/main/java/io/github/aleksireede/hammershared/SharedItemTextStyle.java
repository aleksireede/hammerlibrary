package io.github.aleksireede.hammershared;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class SharedItemTextStyle {
    private static final String DEFAULT_NAME_TEMPLATE = "<!italic><white><name></white>";
    private static final String DEFAULT_LORE_TEMPLATE = "<!italic><gray><name></gray>";

    private final String nameTemplate;
    private final List<String> loreTemplates;

    private SharedItemTextStyle(final String nameTemplate, final List<String> loreTemplates) {
        this.nameTemplate = nameTemplate;
        this.loreTemplates = loreTemplates;
    }

    public static SharedItemTextStyle fromConfig(final FileConfiguration config) {
        final ConfigurationSection section = config.getConfigurationSection("shared-item-text");

        if (section == null) {
            return new SharedItemTextStyle(DEFAULT_NAME_TEMPLATE, List.of(DEFAULT_LORE_TEMPLATE));
        }

        final String nameTemplate = section.getString("name-template", DEFAULT_NAME_TEMPLATE);
        final List<String> loreTemplates = section.getStringList("lore-templates");

        if (loreTemplates.isEmpty()) {
            return new SharedItemTextStyle(nameTemplate, List.of(DEFAULT_LORE_TEMPLATE));
        }

        return new SharedItemTextStyle(nameTemplate, loreTemplates);
    }

    public Component formatName(final String name) {
        return SharedText.miniMessage(this.nameTemplate, Placeholder.unparsed("name", name));
    }

    public List<Component> formatLore(final String name) {
        final List<Component> lines = new ArrayList<>(this.loreTemplates.size());
        for (final String loreTemplate : this.loreTemplates) {
            lines.add(SharedText.miniMessage(loreTemplate, Placeholder.unparsed("name", name)));
        }

        return lines;
    }
}
