# Vision-plugins

Broncode van onze eigen plugins. Elke map `vision-*` is één plugin; ze delen dezelfde
build-instellingen (`build.gradle.kts`) en Paper-versie (`gradle.properties`).

## Nieuwe plugin maken

In een terminal in deze map (`src`):

```
powershell -ExecutionPolicy Bypass -File new-plugin.ps1 hub
```

Dat kopieert `vision-template` naar `vision-hub` met alles hernoemd
(package `nl.codebyvision.hub`, class `VisionHub`, command `/hub`, permissie `hub.admin`).
Daarna in IntelliJ: rechtsboven het Gradle-icoon → **Reload**.

Alleen kleine letters en cijfers in de naam (`hub`, `parkour`, `smp`).

## Bouwen

```
.\gradlew build
```

- Bouwt alle plugins en kopieert ze naar `../plugins/` als `vision-<naam>.jar`.
- Herstart daarna je lokale server om te testen.
- Commit de jar in `plugins/` mee: na de merge naar `main` staat hij op Etheron.
- Eén plugin bouwen: `.\gradlew :vision-hub:build`

De eerste keer downloadt Gradle zichzelf, Java 25 (als je die niet hebt) en de Paper-API.

## Wat zit er in de blauwdruk

| Bestand | Waarvoor |
|---|---|
| `VisionTemplate.java` | Hoofdclass: laadt config, registreert command en listener, `message(key)` voor teksten |
| `TemplateCommand.java` | Voorbeeld-command met permissie, `reload` en tab-complete |
| `TemplateListener.java` | Voorbeeld-event (bericht bij joinen) |
| `plugin.yml` | Naam, commands, permissies. Versie komt uit `build.gradle.kts` |
| `config.yml` | Instellingen en teksten (MiniMessage), herladen met `/<naam> reload` |

Nieuwe commands of permissies? Zet ze in `plugin.yml` en registreer ze in de hoofdclass.

## Paper-versie bijwerken

Pas `paperVersion` aan in `gradle.properties` zodat hij gelijk is aan de server
(regel `Implementing API version ...` in de console).

## Debuggen met hotswap

1. Start de server met `start-debug.bat` (in de hoofdmap).
2. IntelliJ: **Run → Edit Configurations → + → Remote JVM Debug**, poort `5005`, en start die met 🐞.
3. Code aangepast binnen een methode? **Build → Build Project** (Ctrl+F9) en de wijziging zit
   direct in de draaiende server. Nieuwe classes, methodes of `plugin.yml`-wijzigingen vragen
   een herstart.
