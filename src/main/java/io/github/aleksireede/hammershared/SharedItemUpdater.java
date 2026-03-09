package io.github.aleksireede.hammershared;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared item lore/name enrichment system used by all hammer-ecosystem plugins.
 *
 * <p>Each plugin registers its lore suppliers by custom-item ID in its {@code onEnable()},
 * then calls {@link #updateChecker(ItemStack)} (or the damage-aware overload) whenever an
 * item is created or interacted with.  The updater appends damage, health, enchantments,
 * and a rarity/type footer on top of the registered item-specific description lines.
 */
public final class SharedItemUpdater {

    /** Per-ID description lore suppliers registered by individual plugins. */
    private static final Map<Integer, Supplier<List<Component>>> LORE_REGISTRY = new HashMap<>();

    private static JavaPlugin plugin;

    private SharedItemUpdater() {}

    public static void init(final JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Register a lore supplier for the given custom-item ID.
     * Call this once per item ID in your plugin's {@code onEnable()}.
     */
    public static void registerLore(final int customId, final Supplier<List<Component>> loreSupplier) {
        LORE_REGISTRY.put(customId, loreSupplier);
    }

    /**
     * Update the lore of {@code item} if it carries a recognised custom-item ID.
     * Damage is assumed to be zero (e.g. for tools that have no combat-damage config).
     */
    public static void updateChecker(final ItemStack item) {
        updateChecker(item, 0.0);
    }

    /**
     * Update the lore of {@code item} if it carries a recognised custom-item ID.
     *
     * @param damage pre-computed attack damage to display; values ≤ 1 are not shown
     */
    public static void updateChecker(final ItemStack item, final double damage) {
        if (item == null || !item.hasItemMeta()) return;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(SharedItemKeys.customIdKey())) return;

        updateItem(item, damage);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static void updateItem(final ItemStack item, double damage) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        final Integer customId = meta.getPersistentDataContainer().get(
                SharedItemKeys.customIdKey(), PersistentDataType.INTEGER);
        if (customId == null) return;

        final List<Component> lore = new ArrayList<>();

        // Item-specific description lines from the registry
        final Supplier<List<Component>> supplier = LORE_REGISTRY.get(customId);
        if (supplier != null) {
            lore.addAll(supplier.get());
        }

        // Damage (prepended so it appears at the top)
        damage = (int) damage;
        if (damage > 1) {
            lore.addFirst(SharedText.miniMessage("<!i><white>Damage: <red>+" + (int) damage));
            lore.addFirst(SharedText.miniMessage(""));
        }

        // Health bonus
        if (meta.getPersistentDataContainer().has(SharedItemKeys.healthKey())) {
            final Integer health = meta.getPersistentDataContainer()
                    .get(SharedItemKeys.healthKey(), PersistentDataType.INTEGER);
            lore.add(SharedText.miniMessage(""));
            lore.add(SharedText.miniMessage("<!i><white>Health: <red>+" + health));
        }

        // Enchantments
        final List<String> enchantNames = getEnchantmentNames(item);
        if (!enchantNames.isEmpty()) {
            lore.add(SharedText.miniMessage(""));
            enchantNames.forEach(e -> lore.add(SharedText.miniMessage("<!i><blue>" + e)));
        }

        // Rarity + item-type footer
        final String rarity = getRarityString(meta);
        final String itemType = getItemTypeString(meta);
        lore.add(SharedText.miniMessage(""));
        lore.add(SharedText.miniMessage(getColorFromRarity(rarity) + rarity.toUpperCase() + " " + itemType));

        meta.lore(lore);

        // Ensure attribute/enchant tooltips are hidden
        if (!meta.hasAttributeModifiers()) {
            meta.addAttributeModifier(
                    Attribute.LUCK,
                    new AttributeModifier(
                            new NamespacedKey(plugin, "dummy"),
                            0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.HAND
                    )
            );
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }

    private static List<String> getEnchantmentNames(final ItemStack item) {
        final List<String> names = new ArrayList<>();
        final Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (enchantments.isEmpty()) {
            return names;
        }
        for (final Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            final String name = formatEnchantmentName(entry.getKey().getKey().getKey());
            final String level = toRoman(entry.getValue());
            names.add((name + " " + level).trim());
        }
        return names;
    }

    private static String formatEnchantmentName(final String key) {
        final String[] parts = key.replace("_", " ").split(" ");
        final StringBuilder sb = new StringBuilder();
        for (final String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static String toRoman(final int number) {
        if (number <= 0 || number > 3999) return String.valueOf(number);
        final String[] thousands = {"", "M", "MM", "MMM"};
        final String[] hundreds  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        final String[] tens      = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        final String[] ones      = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[number / 1000]
                + hundreds[(number % 1000) / 100]
                + tens[(number % 100) / 10]
                + ones[number % 10];
    }

    private static String getRarityString(final ItemMeta meta) {
        final String rarity = meta.getPersistentDataContainer()
                .get(SharedItemKeys.rarityKey(), PersistentDataType.STRING);
        return (rarity == null || rarity.isEmpty()) ? "Common" : rarity;
    }

    private static String getItemTypeString(final ItemMeta meta) {
        final String type = meta.getPersistentDataContainer()
                .get(SharedItemKeys.itemTypeKey(), PersistentDataType.STRING);
        return (type == null || type.isEmpty()) ? "" : type.toUpperCase();
    }

    // -------------------------------------------------------------------------
    // Public rarity utilities (used by item creation code in dependent plugins)
    // -------------------------------------------------------------------------

    /** Returns the MiniMessage colour tag that matches the given rarity name. */
    public static String getColorFromRarity(final String rarity) {
        if (rarity == null) return "<white>";
        return switch (rarity.toLowerCase()) {
            case "uncommon"  -> "<green>";
            case "rare"      -> "<blue>";
            case "epic"      -> "<dark_purple>";
            case "legendary" -> "<gold>";
            case "mythic"    -> "<red>";
            case "exotic"    -> "<aqua>";
            case "abyssal"   -> "<dark_aqua>";
            case "divine"    -> "<light_purple>";
            case "celestial" -> "<dark_blue>";
            case "eternal"   -> "<dark_green>";
            case "ancient"   -> "<dark_red>";
            default          -> "<white>";
        };
    }

    /** Returns the maximum durability for the given rarity. */
    public static int getDurabilityFromRarity(final String rarity) {
        if (rarity == null) return 0;
        return switch (rarity.toLowerCase()) {
            case "common", "uncommon"          -> 500;
            case "rare"                        -> 1000;
            case "epic"                        -> 1500;
            case "legendary"                   -> 2000;
            case "mythic", "exotic"            -> 3000;
            case "abyssal", "divine", "celestial" -> 4000;
            case "eternal", "ancient"          -> 5000;
            default                            -> 0;
        };
    }
}
