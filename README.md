# vision-smp
The Big Vision SMP RP

## Werkwijze

| Branch | Draait op |
|---|---|
| `main` | De Etheron-server (live). Elke push naar `main` wordt automatisch uitgerold. |
| jouw eigen branch | Je lokale testserver |

0. Eenmalig: download **Paper 26.2** van https://papermc.io/downloads/paper (bij "older versions"),
   zet hem in de map van de repo en hernoem hem naar `server.jar`. Gebruik geen 26.3: de custom models werken daar niet.
1. Maak een eigen branch vanaf `main`: `git switch main && git pull && git switch -c mijn-feature`
2. Start je lokale server met `start.bat` en test je wijziging.
3. Push je branch en open een pull request naar `main`.
4. Na de merge zet GitHub Actions de plugins en configs op Etheron (`.github/workflows/deploy-etheron.yml`)
   en herstart de server, als de herstart is ingesteld.

## Wat staat wel/niet in git

- **Wel:** plugin-jars en hun configs, Paper-configs (`bukkit.yml`, `spigot.yml`, `config/`).
- **Niet:** werelden, spelersdata (balances, hearts, ops, whitelist), logs en alles wat Paper zelf downloadt.
  Zie `.gitignore`. Bouwen doe je op de Etheron-server; daar staan ook de backups.

Je lokale server maakt bij de eerste start een eigen wereld aan. Wil je met de echte wereld testen,
download dan een backup uit het Etheron-paneel en zet die in `world/`.

## Builds met schematics

Grote builds (zoals de hub) maken we als schematic en plakken we met WorldEdit.

1. Download **WorldEdit** voor Paper 26.2 (https://modrinth.com/plugin/worldedit of
   https://hangar.papermc.io/EngineHub/WorldEdit) en zet de jar in `plugins/`.
2. Zet je `.schem`-bestand in `plugins/WorldEdit/schematics/`.
   Schematics in die map staan in git en gaan na een merge naar `main` ook naar Etheron.
3. In-game, op de plek waar de build moet komen:
   ```
   //schem load <naam>
   //paste -a
   ```
   (`-a` slaat lucht over; `//undo` als het niet goed staat.)
4. Maak de deuren of poorten tot portals: ga in de opening op de ene hoek staan `/hub pos1`,
   de andere hoek `/hub pos2`, dan `/hub portal create <naam> <modus>`.
5. Zet de spawn van de hub: `/mv setspawn` (of `/setworldspawn`) op de plek waar spelers moeten beginnen.

Werelden zitten niet in git: gebouwd op je lokale server? Upload de wereldmap één keer naar
Etheron (server uit, via het paneel of SFTP). Of plak de schematic direct op Etheron.

## Eigen plugins

De broncode staat in `src/`. Een nieuwe plugin maak je met `src/new-plugin.ps1`, bouwen met
`src/gradlew build` (zet de jar in `plugins/`). Zie `src/README.md`.

## Let op

- Een plugin weghalen uit git haalt hem **niet** van de server. Verwijder de jar ook in het paneel.
- `server.properties` wordt wel uitgerold (poort en IP zet het paneel zelf goed). De server-jar niet; die beheer je in het paneel
  (Startup: MINECRAFT VERSION `26.2`).
- Plugin-code aangepast? Dan is een herstart nodig. `/reload` niet gebruiken.
