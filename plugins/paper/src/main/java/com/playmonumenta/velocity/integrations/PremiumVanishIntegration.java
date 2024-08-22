package com.playmonumenta.velocity.integrations;

import com.velocitypowered.api.proxy.Player;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PremiumVanishIntegration {
	public static boolean mDisabled = true;

	public static void enable() {
		mDisabled = false;
	}

	/**
	 * A list of the UUIDs of all online vanished players
	 */
	public static List<UUID> getInvisiblePlayers() {
		if (mDisabled) {
			return new ArrayList<>();
		}
		return VelocityVanishAPI.getInvisiblePlayers();
	}

	/**
	 * Player must be online for this to return true if MySQL is enabled
	 *
	 * @param p - the player.
	 * @return TRUE if the player is invisible, FALSE otherwise.
	 */
	public static boolean isInvisible(Player p) {
		if (mDisabled) {
			return false;
		}
		return VelocityVanishAPI.isInvisible(p);
	}

	/**
	 * * Checks if a player is allowed to see another player
	 *
	 * @param viewer - the viewer
	 * @param viewed - the viewed player
	 * @return TRUE if viewed is not vanished or viewer has the permission to see viewed
	 */
	public static boolean canSee(Player viewer, Player viewed) {
		if (mDisabled) {
			return true;
		}
		return VelocityVanishAPI.canSee(viewer, viewed);
	}
}
