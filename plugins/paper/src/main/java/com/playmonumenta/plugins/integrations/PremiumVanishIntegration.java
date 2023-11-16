package com.playmonumenta.plugins.integrations;

import de.myzelyam.api.vanish.VanishAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PremiumVanishIntegration {
	private static boolean DISABLED = true;

	public static void enable(Logger logger) {
		logger.info("Enabling PremiumVanish integration");
		DISABLED = false;
	}

	/**
	 * A list of the UUIDs of all online vanished players
	 */
	public static List<UUID> getInvisiblePlayers() {
		if (DISABLED) {
			return new ArrayList<>();
		}
		return VanishAPI.getInvisiblePlayers();
	}

	/**
	 * Player must be online for this to return true if MySQL is enabled
	 *
	 * @param p - the player.
	 * @return TRUE if the player is invisible, FALSE otherwise.
	 */
	public static boolean isInvisibleOrSpectator(Player p) {
		if (p.getGameMode() == GameMode.SPECTATOR) {
			return true;
		}
		if (DISABLED) {
			return false;
		}
		return VanishAPI.isInvisible(p);
	}

	/**
	 * Hides a player using PremiumVanish
	 *
	 * @param p - the player.
	 */
	public static void hidePlayer(Player p) {
		if (DISABLED) {
			return;
		}
		VanishAPI.hidePlayer(p);
	}

	/**
	 * * Shows a player using PremiumVanish
	 *
	 * @param p - the player.
	 */
	public static void showPlayer(Player p) {
		if (DISABLED) {
			return;
		}
		VanishAPI.showPlayer(p);
	}

	/**
	 * * Checks if a player is allowed to see another player
	 *
	 * @param viewer - the viewer
	 * @param viewed - the viewed player
	 * @return TRUE if viewed is not vanished or viewer has the permission to see viewed
	 */
	public static boolean canSee(Player viewer, Player viewed) {
		if (DISABLED) {
			return true;
		}
		return VanishAPI.canSee(viewer, viewed);
	}
}
