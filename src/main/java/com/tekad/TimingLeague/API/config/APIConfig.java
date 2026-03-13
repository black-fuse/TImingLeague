package com.tekad.TimingLeague.API.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class APIConfig {
    private final Plugin plugin;
    private FileConfiguration config;

    private boolean enabled;
    private int port;
    private String readApiKey;
    private String adminApiKey;
    private int rateLimit;
    private boolean corsEnabled;
    private List<String> allowedOrigins;

    public APIConfig(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("api.enabled", true);
        port = config.getInt("api.port", 8080);
        readApiKey = config.getString("api.read-api-key", "");
        adminApiKey = config.getString("api.admin-api-key", "");
        rateLimit = config.getInt("api.rate-limit", 100);
        corsEnabled = config.getBoolean("api.cors-enabled", true);
        allowedOrigins = config.getStringList("api.allowed-origins");

        // Generate keys if empty
        boolean needsSave = false;
        if (readApiKey.isEmpty()) {
            readApiKey = generateApiKey();
            config.set("api.read-api-key", readApiKey);
            needsSave = true;
            plugin.getLogger().info("[API] Generated read API key: " + readApiKey);
        }

        if (adminApiKey.isEmpty()) {
            adminApiKey = generateApiKey();
            config.set("api.admin-api-key", adminApiKey);
            needsSave = true;
            plugin.getLogger().info("[API] Generated admin API key: " + adminApiKey);
        }

        if (needsSave) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[API] Failed to save config: " + e.getMessage());
            }
        }
    }

    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isEnabled() { return enabled; }
    public int getPort() { return port; }
    public String getReadApiKey() { return readApiKey; }
    public String getAdminApiKey() { return adminApiKey; }
    public int getRateLimit() { return rateLimit; }
    public boolean isCorsEnabled() { return corsEnabled; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
}