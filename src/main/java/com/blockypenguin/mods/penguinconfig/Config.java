package com.blockypenguin.mods.penguinconfig;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class Config {
    private static final Logger LOGGER = PenguinConfig.createLogger("Config Manager");
    private static final Jankson JANKSON = Jankson.builder().build();
    private static final JsonGrammar GRAMMAR = JsonGrammar.builder().printCommas(false).bareSpecialNumerics(true).printUnquotedKeys(true).build();
    public static final JsonGrammar GRAMMAR_MINI = JsonGrammar.builder().withComments(false).printWhitespace(false).bareSpecialNumerics(true).printUnquotedKeys(true).build();

    private final JsonObject data = new JsonObject();
    private final JsonObject defaultConfig;
    private final String configName;
    private final File configFile;
    private boolean broken = false;

    static {
        LOGGER.info("Config files will be stored at {}", FabricLoader.getInstance().getConfigDir().toAbsolutePath().normalize());
    }

    public Config(String configName, InputStream defaultConfig) {
        LOGGER.info("Loading new config '{}'...", configName);
        this.configName = configName;
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve(configName + ".pengson").toFile();

        try {
            this.defaultConfig = JANKSON.load(defaultConfig);
        }catch(SyntaxError | IOException e) {
            LOGGER.error("Failed to load default config file for config '" + configName + "'", e);
            throw new RuntimeException(e);
        }

        this.syncConfig();
    }

    public void syncConfig() {
        LOGGER.info("Starting config sync... Current in-memory config: {}", toMiniPengson());

        if(!configFile.exists() || !configFile.isFile() || configFile.length() == 0)
            writeToFile();

        JsonObject tempJson = defaultConfig.clone();

        try {
            mergeJson(tempJson, JANKSON.load(configFile));
            this.broken = false;
        }catch(IOException | SyntaxError e) {
            LOGGER.error("Failed to load config '" + configName + "', considering it broken. Using default values.", e);
            this.broken = true;
        }

        if(!tempJson.equals(this.data)) {
            this.data.clear();
            this.data.putAll(tempJson);

            if(!this.broken)
                writeToFile();
        }

        LOGGER.info("Completed config sync. New in-memory config: {}", toMiniPengson());
    }

    private JsonObject mergeJson(JsonObject base, JsonObject overlay) {
        overlay.forEach((key, value) -> {
            base.merge(key, value, (oldValue, newValue) -> {
                if(oldValue instanceof JsonObject oldObj && newValue instanceof JsonObject newObj)
                    return mergeJson(oldObj, newObj);

                return newValue;
            });
        });

        return base;
    }

    private void writeToFile() {
        try(var fos = new FileOutputStream(configFile)) {
            fos.write(toPengson().getBytes());
            fos.flush();
        }catch(IOException e) {
            LOGGER.error("Failed to save config '" + configName + "'", e);
        }
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        return Optional.ofNullable(data.recursiveGet(clazz, key));
    }

    public String toPengson() { return this.data.toJson(GRAMMAR); }
    public String toMiniPengson() { return this.data.toJson(GRAMMAR_MINI); }

    public boolean isBroken() { return broken; }
}