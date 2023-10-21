package com.blockypenguin.mods.penguinconfig;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PenguinConfig implements ModInitializer {
    static Logger createLogger(String... path) {
        return LoggerFactory.getLogger("PenguinConfig" + ':' + String.join("/", path));
    }

    @Override
    public void onInitialize() { }
}