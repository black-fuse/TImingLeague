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

    public void updateHolograms(League league, int page, @Nullable Player player) {
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

        switch (subCommand) {
            case "create" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to create leagues.");
                    return true;
                }
                if (args.length < 2) { player.sendMessage("Usage: /league create <leagueName>"); return true; }
                String leagueName = args[1];
                if (leagues.containsKey(leagueName)) { player.sendMessage("A league with that name already exists."); return true; }
                leagues.put(leagueName, new League(leagueName, 20));
                player.sendMessage("Created league: " + leagueName);
                return true;
            }
            case "delete" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to delete leagues.");
                    return true;
                }
                if (args.length < 2) { player.sendMessage("Usage: /league delete <leagueName>"); return true; }
                if (!leagues.containsKey(args[1])) { player.sendMessage("League not found: " + args[1]); return true; }
                leagues.remove(args[1]);
                player.sendMessage("Deleted league: " + args[1]);
                return true;
            }
            case "help" -> { LeagueHelpCommand.showHelp(player); return true; }

            default -> {
                String leagueName = args[0];
                League league = leagues.get(leagueName);
                if (league == null) { player.sendMessage("League '" + leagueName + "' not found."); return true; }

                if (args.length < 2) {
                    sendLeaguePanel(player, league, leagueName);
                    return true;
                }

                String action = args[1].toLowerCase();

                return switch (action) {
                    case "adddriver"        -> LeagueDriverCommands.handleAddDriver(player, league, args);
                    case "points"           -> { boolean r = LeagueDriverCommands.handlePoints(player, league, args); hologramCommands.updateHolograms(league, 1, player); yield r; }
                    case "teampoints"       -> { boolean r = LeagueDriverCommands.handleTeamPoints(player, league, args); hologramCommands.updateHolograms(league, 1, player); yield r; }
                    case "update"           -> LeagueAdminCommands.handleUpdate(player, league);
                    case "addevent"         -> LeagueAdminCommands.handleAddEvent(player, league, args);
                    case "removeevent"       -> LeagueAdminCommands.handleRemoveEvent(player, league, args);
                    case "seteventcategory" -> LeagueAdminCommands.handleSetEventCategory(player, league, args);
                    case "seteventheat"     -> LeagueAdminCommands.handleSetEventHeat(player, league, args);
                    case "updatewithheat"   -> { boolean r = LeagueAdminCommands.handleUpdateWithHeat(player, league, args); hologramCommands.updateHolograms(league, 1, player); yield r; }
                    case "scoring"          -> LeagueAdminCommands.handleScoring(player, league, args);
                    case "predicteddrivers" -> LeagueAdminCommands.handlePredictedDrivers(player, league, args);
                    case "teammode"         -> LeagueAdminCommands.handleTeamMode(player, league, args);
                    case "teamconfig"       -> LeagueAdminCommands.handleTeamConfig(player, league, args);
                    case "customscale"      -> LeagueCustomScaleCommands.handleCustomScale(player, league, args);
                    case "mulligans"        -> LeagueMulliganCommands.handleMulligans(player, league, args);
                    case "breakdown"        -> LeagueMulliganCommands.handleBreakdown(player, league, leagueName, args);
                    case "category"         -> LeagueCategoryCommands.handle(player, league, leagueName, args);
                    case "standings"        -> {
                        // standings drivers/teams <true|false>  goes to toggle; otherwise normal display
                        if (args.length >= 4 && (args[2].equalsIgnoreCase("drivers") || args[2].equalsIgnoreCase("teams"))
                                && (args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("false"))) {
                            yield LeagueAdminCommands.handleStandingsToggle(player, league, args);
                        }
                        yield LeagueStandingsCommands.handleStandings(player, league, leagueName, args);
                    }
                    case "calendar"         -> LeagueStandingsCommands.handleCalendar(player, league);
                    case "holo"             -> hologramCommands.handleHolo(player, league, args);
                    case "team"             -> teamCommands.handleTeamCommand(player, league, leagueName, args);
                    default -> {
                        player.sendMessage("Unknown subcommand for league '" + leagueName + "'. Try /league help");
                        yield true;
                    }
                };
            }
        }
    }


    // League info panel  (/league <leagueName>)
    private void sendLeaguePanel(Player player, League league, String leagueName) {
        Theme theme = Theme.getTheme(player);
        boolean isAdmin = player.hasPermission("timingleague.admin");

        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(leagueName).color(theme.getSecondary()))));

        // Scoring system
        Component scoringValue = theme.getBrackets(EventCategory.scoringSystemNameOf(league.getScoringSystem()));
        if (isAdmin) {
            scoringValue = scoringValue
                    .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " scoring "))
                    .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT)));
        }
        player.sendMessage(Component.text("default scoring: ", TextColor.color(theme.getPrimary())).append(scoringValue));

        // Categories
        int catCount = league.getCategories().size();
        Component catValue = catCount > 0
                ? theme.getBrackets(catCount + " categor" + (catCount == 1 ? "y" : "ies"))
                : theme.getBrackets("none");
        catValue = catValue.clickEvent(ClickEvent.runCommand("/league " + leagueName + " category list"));
        player.sendMessage(Component.text("categories: ", TextColor.color(theme.getPrimary())).append(catValue));

        // ── Global mulligans (only show if no categories, or always show for admins) ──
        if (isAdmin || league.getCategories().isEmpty()) {
            String mullLabel = league.getCategories().isEmpty()
                    ? "mulligans: "
                    : "mulligans (uncategorised): ";
            Component mullValue = theme.getBrackets(String.valueOf(league.getMulliganCount()));
            if (isAdmin) {
                mullValue = mullValue
                        .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " mulligans "))
                        .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT)));
            }
            player.sendMessage(Component.text(mullLabel, TextColor.color(theme.getPrimary())).append(mullValue));
        }

        // ── Calendar ──────────────────────────────────────────────────────────────
        int eventCount = league.getCalendarEntries().size();
        Component calValue = theme.getBrackets(eventCount + " event" + (eventCount == 1 ? "" : "s"))
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " calendar"));
        player.sendMessage(Component.text("calendar: ", TextColor.color(theme.getPrimary())).append(calValue));

        // ── Teams ─────────────────────────────────────────────────────────────
        player.sendMessage(Component.text("teams: ", TextColor.color(theme.getPrimary()))
                .append(theme.getViewButton(player).clickEvent(ClickEvent.runCommand("/league " + leagueName + " team list"))));

        // ── Standings rows (conditional on enabled flags) ─────────────────────
        if (league.isDriverStandingsEnabled()) {
            player.sendMessage(Component.text("standings: ", TextColor.color(theme.getPrimary()))
                    .append(theme.getViewButton(player).clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"))));
        }
        if (league.isTeamStandingsEnabled()) {
            player.sendMessage(Component.text("team standings: ", TextColor.color(theme.getPrimary()))
                    .append(theme.getViewButton(player).clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings teams"))));
        }

        // ── Admin: standings toggle hints ─────────────────────────────────────
        if (isAdmin) {
            Component driverToggle = Component.text("  [drivers ", TextColor.color(theme.getPrimary()))
                    .append(theme.getBrackets(league.isDriverStandingsEnabled() ? "ON" : "OFF")
                            .color(league.isDriverStandingsEnabled() ? theme.getSuccess() : theme.getError())
                            .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings drivers " + !league.isDriverStandingsEnabled()))
                            .hoverEvent(HoverEvent.showText(Component.text("Toggle driver standings"))))
                    .append(Component.text("]", TextColor.color(theme.getPrimary())));

            Component teamToggle = Component.text("  [teams ", TextColor.color(theme.getPrimary()))
                    .append(theme.getBrackets(league.isTeamStandingsEnabled() ? "ON" : "OFF")
                            .color(league.isTeamStandingsEnabled() ? theme.getSuccess() : theme.getError())
                            .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings teams " + !league.isTeamStandingsEnabled()))
                            .hoverEvent(HoverEvent.showText(Component.text("Toggle team standings"))))
                    .append(Component.text("]", TextColor.color(theme.getPrimary())));

            player.sendMessage(Component.text("standings toggles:").color(theme.getPrimary())
                    .append(driverToggle).append(teamToggle));
        }
    }
}
