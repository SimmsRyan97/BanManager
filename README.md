# BanManager

BanManager is a plugin for configurable warning and ban workflows that work for online and offline players.

## Features

- Configurable rule system (`config.yml`) with per-warning escalation.
- Supports actions: `MESSAGE`, `BROADCAST`, `KICK`, `TEMP_BAN`, `BAN`, `IP_BAN`.
- Auto-kick based on:
  - too many warnings inside a configurable time window
  - repeated breaks of the same rule in a short period
- Online and offline moderation support.
- Persistent player moderation log in `plugins/BanManager/data.yml`:
  - UUID
  - last known username
  - last known IP
  - warning history
  - sanctions history
  - active bans
- Custom pre-login ban check so this system takes precedence over default vanilla ban behavior for players managed by this plugin.

## Build (IntelliJ + Maven)

1. Open this folder in IntelliJ IDEA.
2. Make sure project SDK is Java 17.
3. In Maven tool window, run `Lifecycle -> package`.
4. The plugin JAR will be at `target/banmanager-[version].jar`.

Or build from command line:

```bash
mvn clean package
```

## Commands

- `/warn <player> <rule> [reason]`
- `/unwarn <player> <count|all>`
- `/kick <player> [reason]`
- `/tempban <player> <duration> [reason]`
- `/ban <player> [reason]`
- `/ipban <player> [reason]`
- `/unban <player>`
- `/clearpunishments <player> <count|all>`
- `/bmhistory <player>`
- `/bmreload`

`/bmreload` reloads both config and `data.yml` from disk, so manual edits to `data.yml` are applied after reload.

Duration format examples for `tempban`: `30s`, `30m`, `12h`, `1d`, `2w`.

## Configuration

Rules are configured in `src/main/resources/config.yml`.

Example:

```yaml
rules:
  griefing:
    display-name: "Griefing"
    same-rule-auto-kick-threshold: 2
    punishments:
      1:
        type: MESSAGE
        message: "&ePlease stop griefing."
      2:
        type: KICK
        message: "&cKicked for repeated griefing."
      3:
        type: TEMP_BAN
        duration: "1d"
        message: "&cTemp banned for griefing."
      4:
        type: BAN
        message: "&4Permanently banned for griefing."
```

You can define any number of rules and escalation levels.
