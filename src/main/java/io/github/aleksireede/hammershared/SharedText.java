package io.github.aleksireede.hammershared;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class SharedText {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private SharedText() {
    }

    public static Component miniMessage(final String value, final TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(value, resolvers);
    }
}
