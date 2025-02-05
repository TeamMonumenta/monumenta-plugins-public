package com.playmonumenta.plugins.cosmetics.punches;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlayerPunches {
	private static final ImmutableMap<String, PlayerPunch> PUNCHES =
		ImmutableMap.<String, PlayerPunch>builder()
			.put(ExplosivePunch.NAME, new ExplosivePunch())
			.put(RobberyPunch.NAME, new RobberyPunch())
			.put(TranscendentPunch.NAME, new TranscendentPunch())
			.build();

	public static void activatePunch(Player bully, Player victim, String punchName, boolean isRemotePunch) {
		PlayerPunch punch = PUNCHES.get(punchName);
		if (punch != null) {
			punch.run(bully, victim);
			punch.broadcastPunchMessage(bully, victim, victim.getWorld().getPlayers(), isRemotePunch);
		}
	}

	public static Material getDisplayItem(String punchName) {
		PlayerPunch punch = PUNCHES.get(punchName);
		if (punch != null) {
			return punch.getDisplayItem();
		} else {
			return Material.FEATHER;
		}
	}

	public static boolean isOnWhitelistedShard() {
		return
			ServerProperties.getShardName().equals("guildplots") ||
			ServerProperties.getShardName().equals("playerplots") ||
			ServerProperties.getShardName().equals("plots");
	}

	public static boolean canAccess(Player player) {
		return ((ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1) || player.hasPermission("group.dev")) &&
			!player.hasPermission("monumenta.cosmetics.punchoptout");
	}

	public static boolean canBePunched(Player player) {
		return ((ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1) || player.hasPermission("group.dev")) &&
			!player.hasPermission("monumenta.cosmetics.punchoptout");
	}

	public static void handleLogin(Player player) {
		if (canAccess(player)) {
			for (String name : getNameSet()) {
				CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.PLAYER_PUNCH, name);
			}
		} else {
			CosmeticsManager.getInstance().clearCosmetics(player, CosmeticType.PLAYER_PUNCH);
		}
	}

	public static String[] getNames() {
		return PUNCHES.keySet().toArray(String[]::new);
	}

	public static Set<String> getNameSet() {
		return Set.copyOf(PUNCHES.keySet());
	}
}
