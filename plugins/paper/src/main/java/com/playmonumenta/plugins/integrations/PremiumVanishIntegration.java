package com.playmonumenta.plugins.integrations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.myzelyam.api.vanish.VanishAPI;

public class PremiumVanishIntegration {
	private static @Nullable PremiumVanishIntegration INSTANCE = null;

	public PremiumVanishIntegration(Logger logger) {
		logger.info("Enabling PremiumVanish integration");
		INSTANCE = this;
	}

	/**
	 * @return A list of the UUIDs of all online vanished players
	 */
	public static List<UUID> getInvisiblePlayers() {
		if (INSTANCE == null) {
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
	public static boolean isInvisible(Player p) {
		if (INSTANCE == null) {
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
		if (INSTANCE == null) {
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
		if (INSTANCE == null) {
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
		if (INSTANCE == null) {
			return true;
		}
		return VanishAPI.canSee(viewer, viewed);
	}
}
