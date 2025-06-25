package com.tekad.TimingLeague.eventGen;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
@Setter
public class EventGenPreset {
    private String name;
    private List<String> heats = new ArrayList<>(); // Optional: flat list of heat IDs
    private Map<String, List<String>> rounds = new LinkedHashMap<>(); // Round name -> list of heats

    public EventGenPreset(String name) {
        this.name = name;
    }

    public void addRound(String roundName) {
        rounds.putIfAbsent(roundName, new ArrayList<>());
    }

    public void addHeatToRound(String roundName, String heatId) {
        rounds.computeIfAbsent(roundName, k -> new ArrayList<>()).add(heatId);
        heats.add(heatId);
    }

    public void saveAsPreset(File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", name);
        config.set("heats", heats);
        config.set("rounds", rounds);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static EventGenPreset loadFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = config.getString("name");
        List<String> heats = config.getStringList("heats");
        Map<String, List<String>> rounds = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("rounds");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                rounds.put(key, config.getStringList("rounds." + key));
            }
        }

        EventGenPreset preset = new EventGenPreset(name);
        preset.setHeats(heats);
        preset.setRounds(rounds);
        return preset;
    }
}
