package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.StringUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorWindwalker implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_WINDWALKER_COOLDOWN = IchorListener.ITEM_NAME + " - Windwalker";
	private static final double CDR = 0.05;
	private static final double SPEED_PER = 0.02;
	private static final int ABILITY_CAP = 5;
	private static final int DURATION = 8 * 20;
	private static final String EFFECT = "IchorWindSpeedEffect";
	public static final String DESCRIPTION = String.format("Reduce your ability cooldowns by %s%%, then gain %s%% speed per ability that got reduced for %s seconds (%s%% cap). Cooldown: %s seconds.",
		StringUtils.multiplierToPercentage(CDR),
		StringUtils.multiplierToPercentage(SPEED_PER),
		StringUtils.ticksToSeconds(DURATION),
		StringUtils.multiplierToPercentage(SPEED_PER * ABILITY_CAP),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_WINDWALKER;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Windwalker";
	}

	// Want it to run before other cdr, e.g. Temporal Bender
	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_WINDWALKER_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_WINDWALKER_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_WINDWALKER_COOLDOWN));
		ichorWindwalker(plugin, player, 1);
	}

	public static void ichorWindwalker(Plugin plugin, Player player, double multiplier) {
		int abilitiesReduced = plugin.mTimers.updateCooldownsPercent(player, CDR * multiplier);
		double speed = FastMath.min(abilitiesReduced, ABILITY_CAP) * SPEED_PER * multiplier;
		if (speed > 0) {
			plugin.mEffectManager.addEffect(player, EFFECT, new PercentSpeed(DURATION, speed, EFFECT));
		}

		player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.5f, 2f);
		player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.8f, 2f);

		new PartialParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 10, 1.25, 1.25, 1.25).spawnAsPlayerPassive(player);
		new PartialParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 10, 1.25, 1.25, 1.25).spawnAsPlayerPassive(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 10, 1.25, 1.25, 1.25).spawnAsPlayerPassive(player);
	}
}
