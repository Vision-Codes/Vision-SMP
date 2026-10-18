package nl.codebyvision.hub;

import java.util.List;
import org.bukkit.GameMode;

/** A game mode (hub, survival, smp, ...): its worlds share one inventory. The first world is the entry point. */
record Mode(String name, List<String> worlds, GameMode gameMode, boolean protectedWorld) {
}
