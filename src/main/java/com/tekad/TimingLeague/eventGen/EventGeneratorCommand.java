package com.tekad.TimingLeague.eventGen;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class EventGeneratorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("preset")) {
            if (args.length >= 4 && args[1].equalsIgnoreCase("create")) {
                String presetName = args[2];
                String source = args[3]; // expected: "activeEvent"

                if (source.equalsIgnoreCase("activeEvent")) {
                    return handlePresetSave(sender, presetName);
                }
            } else if (args.length >= 4) {
                String presetName = args[1];
                String newEventName = args[2];
                String trackName = args[3];

                return handlePresetLoad(sender, presetName, newEventName, trackName);
            } else {
                sender.sendMessage("Usage:");
                sender.sendMessage("/eventgen preset create <presetName> activeEvent");
                sender.sendMessage("/eventgen preset <presetName> <newEventName> <trackName>");
            }
            return true;
        }

        sender.sendMessage("Unknown subcommand. Use /eventgen for help.");
        return true;
    }

    private boolean handlePresetSave(CommandSender sender, String presetName) {
        // ⚠️ You must implement this logic using your active event system
        EventGenPreset preset = generatePresetFromActiveEvent();
        if (preset == null) {
            sender.sendMessage("No active event found to save.");
            return true;
        }

        File presetFile = new File("plugins/TimingLeague/presets", presetName + ".yml");
        preset.saveAsPreset(presetFile);
        sender.sendMessage("Preset '" + presetName + "' saved successfully.");
        return true;
    }

    private boolean handlePresetLoad(CommandSender sender, String presetName, String newEventName, String trackName) {
        File presetFile = new File("plugins/TimingLeague/presets", presetName + ".yml");
        if (!presetFile.exists()) {
            sender.sendMessage("Preset '" + presetName + "' not found.");
            return true;
        }

        EventGenPreset preset = EventGenPreset.loadFromFile(presetFile);

        // ⚠️ You must implement logic to convert this into a new event
        generateEventFromPreset(preset, newEventName, trackName);
        sender.sendMessage("Event '" + newEventName + "' generated using preset '" + presetName + "'.");
        return true;
    }

    private EventGenPreset generatePresetFromActiveEvent() {
        // TODO: Implement your own logic to convert an in-memory active event to a preset
        return null;
    }

    private void generateEventFromPreset(EventGenPreset preset, String newEventName, String trackName) {
        // TODO: Implement your own logic to generate a new event using a preset
    }

    public void showHelp(CommandSender sender) {
        sender.sendMessage("""
            === /eventgen Command Help ===
            /eventgen preset create <presetName> activeEvent - Save current event as a preset
            /eventgen preset <presetName> <newEventName> <trackName> - Create a new event using a preset
            """);
    }
}
