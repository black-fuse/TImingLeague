# TImingLeague

**TImingLeague** is a Minecraft plugin designed to work with the *Timing System* plugin. It helps server admins manage racing leagues: track multiple events, score drivers (and teams), generate leaderboards / standings, etc. All core functionality is now implemented; what remains is polishing, UX tweaks, and refinements.

---

## Features

- Support for **leagues** spanning multiple **racing events**.  
- Score drivers based on performance; customizable scoring systems (in progress).  
- Support for **teams**: drivers can belong to teams, team standings are tracked.  
- Driver / team standings across events.  
- Permissions system: e.g. `timingleague.admin` for full access, `timingleague.view` for viewing standings and team info.  
- Soft-dependency support for *Decent Holograms* for showing holographic outputs (if available).  

---

## Requirements

- Minecraft server (version compatible with Timing System).  
- The **Timing System** plugin installed (hard dependency).  
- Optional: **Decent Holograms** for holographic display of standings.  

---

## Installation

1. Download the plugin JAR (from releases or build it).  
2. Place the JAR in your server’s `plugins/` directory.  
3. Ensure `Timing System` plugin is also in `plugins/`.  
4. Restart or reload the server.  

---

## Configuration & Usage

- Permissions:

  | Permission             | Effect |
  |------------------------|--------|
  | `timingleague.admin`   | Full access: create leagues, manage teams, modify settings. |
  | `timingleague.view`    | Can view standings, team information. |

- Commands & GUI: *(to be finalized — e.g. how to create an event, how to add drivers, how to view standings, etc.)*

- Teams: Drivers can be grouped into teams; team owners (or admins) can manage drivers on their team.

- Scoring: (If implemented) support for different scoring presets or custom scoring systems.  

---

## To Be Polished / Upcoming

- Event generation based on presets.  
- Allow for completely custom scoring systems.  
- Reworking the team system (e.g. only team owners can edit their drivers).  
- Fix issue: teams not always persisting correctly in the database.  
- Store team colors in database.  
- Add feature: export standings, teams, etc., to CSV.  

---

## Contributing

If you want to help:

- Fork the repo, make changes, submit pull requests.  
- Coding standards: follow existing style (e.g. Java conventions).  
- Tests or sample worlds are helpful.  
- Bug reports & feature suggestions: open an issue.  
