package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.*;
import com.tekad.TimingLeague.commands.subcommands.*;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Hover;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class leagueCommand implements CommandExecutor {

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();
    private final Map<String, Map<String, Set<String>>> pendingInvites = new HashMap<>();
    
    private final LeagueTeamCommands teamCommands;
    private final LeagueHologramCommands hologramCommands;

    public leagueCommand() {
        this.teamCommands = new LeagueTeamCommands(pendingInvites);
        this.hologramCommands = new LeagueHologramCommands();
    }

    public void updateHolograms(League league, int page, @Nullable Player player){
        hologramCommands.updateHolograms(league, 1, null);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }

        if (!sender.hasPermission("timingleague.view")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length == 0) {
            LeagueHelpCommand.showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Top-level commands
        switch (subCommand) {
            case "create" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to create leagues.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("Usage: /league create <leagueName>");
                    return true;
                }

                String leagueName = args[1];
                if (leagues.containsKey(leagueName)) {
                    player.sendMessage("A league with that name already exists.");
                    return true;
                }

                League league = new League(leagueName, 20);
                leagues.put(leagueName, league);
                player.sendMessage("Created league: " + leagueName);
                return true;
            }

            case "delete" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to delete leagues.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("Usage: /league delete <leagueName>");
                    return true;
                }

                String leagueName = args[1];
                if (!leagues.containsKey(leagueName)) {
                    player.sendMessage("A league with that name does not exist.");
                    return true;
                }

                leagues.remove(leagueName);
                player.sendMessage("Deleted league: " + leagueName);
                return true;
            }

            case "help" -> {
                LeagueHelpCommand.showHelp(player);
                return true;
            }

            default -> {
                // Assume first argument is league name
                String leagueName = args[0];
                League league = leagues.get(leagueName);

                if (league == null) {
                    player.sendMessage("League '" + leagueName + "' not found.");
                    return true;
                }

                if (args.length < 2) {
                    // doing a lot of copying from timing system so this might look like a mess
                    Theme theme = Theme.getTheme(sender);

                    sender.sendMessage("");
                    sender.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/league " + leagueName))
                            .append(Component.space())
                            .append(theme.getTitleLine(Component.text(leagueName).color(theme.getSecondary())
                                    .append(Component.space())
                            )));

                    Component scoringMessage;
                    if (sender.hasPermission("timingleague.admin")){
                        scoringMessage = Component.text("scoring system: ", TextColor.color(theme.getSecondary())).append(theme.getBrackets(league.getScoringSystem().getName()).clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " scoring " )).hoverEvent(HoverEvent.showText(Text.get(sender, Hover.CLICK_TO_EDIT))));
                    }
                    else{
                        scoringMessage = Component.text("scoring system: ", TextColor.color(theme.getSecondary())).append(theme.getBrackets(league.getScoringSystem().getName()));
                    }
                    sender.sendMessage(scoringMessage);

                    Component mulliganMessage;
                    if (sender.hasPermission("timingleague.admin")){
                        mulliganMessage = Component.text("mulligans: ", TextColor.color(theme.getPrimary())).append(theme.getBrackets(String.valueOf(league.getMulliganCount())).clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " mulligans " )).hoverEvent(HoverEvent.showText(Text.get(sender, Hover.CLICK_TO_EDIT))));
                    }
                    else{
                        mulliganMessage = Component.text("mulligans: ", TextColor.color(theme.getPrimary())).append(theme.getBrackets(String.valueOf(league.getMulliganCount())));
                    }
                    sender.sendMessage(mulliganMessage);

                    Component teamMessage;
                    teamMessage = Component.text("teams: ", TextColor.color(theme.getPrimary())).append(theme.getViewButton(sender).clickEvent(ClickEvent.runCommand("/league " + leagueName + " team list")));
                    sender.sendMessage(teamMessage);

                    sender.sendMessage(Component.text("standings: ", TextColor.color(theme.getPrimary()))
                            .append(theme.getViewButton(sender)
                            .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"))));

                    sender.sendMessage(Component.text("team standings: ", TextColor.color(theme.getPrimary()))
                            .append(theme.getViewButton(sender)
                                    .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings teams"))));

                    return true;
                }

                String action = args[1].toLowerCase();

                // Route to appropriate subcommand handler
                return switch (action) {
                    case "adddriver" -> LeagueDriverCommands.handleAddDriver(player, league, args);
                    case "points" -> {
                        boolean result = LeagueDriverCommands.handlePoints(player, league, args);
                        hologramCommands.updateHolograms(league, 1, player);
                        yield result;
                    }
                    case "teampoints" -> {
                        boolean result = LeagueDriverCommands.handleTeamPoints(player, league, args);
                        hologramCommands.updateHolograms(league, 1, player);
                        yield result;
                    }
                    case "update" -> LeagueAdminCommands.handleUpdate(player, league);
                    case "addevent" -> LeagueAdminCommands.handleAddEvent(player, league, args);
                    case "updatewithheat" -> {
                        boolean result = LeagueAdminCommands.handleUpdateWithHeat(player, league, args);
                        hologramCommands.updateHolograms(league, 1, player);
                        yield result;
                    }
                    case "scoring" -> LeagueAdminCommands.handleScoring(player, league, args);
                    case "predicteddrivers" -> LeagueAdminCommands.handlePredictedDrivers(player, league, args);
                    case "teammode" -> LeagueAdminCommands.handleTeamMode(player, league, args);
                    case "teamconfig" -> LeagueAdminCommands.handleTeamConfig(player, league, args);
                    case "customscale" -> LeagueCustomScaleCommands.handleCustomScale(player, league, args);
                    case "mulligans" -> LeagueMulliganCommands.handleMulligans(player, league, args);
                    case "breakdown" -> LeagueMulliganCommands.handleBreakdown(player, league, leagueName, args);
                    case "standings" -> LeagueStandingsCommands.handleStandings(player, league, leagueName, args);
                    case "calendar" -> LeagueStandingsCommands.handleCalendar(player, league);
                    case "holo" -> hologramCommands.handleHolo(player, league, args);
                    case "team" -> teamCommands.handleTeamCommand(player, league, leagueName, args);
                    default -> {
                        player.sendMessage("Unknown subcommand for league '" + leagueName + "'. Try /league help");
                        yield true;
                    }
                };
            }
        }
    }
}
