package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerScalingBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_playerscaling";

	@BossParam(help = "Scales health and damage of the boss based of player count")
	public static class Parameters extends BossParameters {

		@BossParam(help = "If true it scales based on the amount of players in range, otherwise scales with the world player count")
		public boolean DO_LOCAL_SCALING = false;
		@BossParam(help = "Range for local scaling")
		public int LOCAL_SCALING_RANGE = 100;

		@BossParam(help = "Base exponent, increase to increase health scaling. range = [0 < X < 1]")
		public double HEALTH_BASE_EXPONENT = 0.4;
		@BossParam(help = "Player count exponent, decrease to increase health scaling. range = [0 < X < 1]")
		public double HEALTH_PLAYER_EXPONENT = 0.3;

		@BossParam(help = "Base exponent, increase to increase damage scaling. range = [0 < X < 1]")
		public double DAMAGE_BASE_EXPONENT = 0.15;
		@BossParam(help = "Player count exponent, decrease to increase damage scaling. range = [0 < X < 1]")
		public double DAMAGE_PLAYER_EXPONENT = 0.4;
	}

	private final double mDamageMultiplier;
	private final double mHealthMultiplier;

	public PlayerScalingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		PlayerScalingBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new PlayerScalingBoss.Parameters());

		int playerCount;
		if (p.DO_LOCAL_SCALING) {
			playerCount = PlayerUtils.playersInRange(boss.getLocation(), p.LOCAL_SCALING_RANGE, true).size();
		} else {
			playerCount = 0;

			for (Player player : boss.getWorld().getPlayers()) {
				if (player.getGameMode() != GameMode.SPECTATOR) {
					playerCount++;
				}
			}
		}

		mDamageMultiplier = BossUtils.healthScalingCoef(playerCount, p.DAMAGE_BASE_EXPONENT, p.DAMAGE_PLAYER_EXPONENT);
		mHealthMultiplier = BossUtils.healthScalingCoef(playerCount, p.HEALTH_BASE_EXPONENT, p.HEALTH_PLAYER_EXPONENT);

	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getType() == DamageType.TRUE) {
			return;
		}
		event.setFlatDamage(event.getDamage() * mDamageMultiplier);
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setFlatDamage(event.getFlatDamage() / mHealthMultiplier);
	}

}
