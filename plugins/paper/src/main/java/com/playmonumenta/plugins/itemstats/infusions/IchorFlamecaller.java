package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorFlamecaller implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_FLAMECALLER_COOLDOWN = IchorListener.ITEM_NAME + " - Flamecaller";
	private static final double MAGIC_DAMAGE_PER = 0.02;
	private static final int MOB_CAP = 6;
	private static final int BUFF_DURATION = 8 * 20;
	private static final double RANGE = 8;
	private static final double SPEED = 0.1;
	private static final String EFFECT = "IchorFlameDamageEffect";
	private static final String SPEED_EFFECT = "IchorFlameSpeedEffect";
	public static final String DESCRIPTION = String.format("Gain %s%% magic damage per mob on fire within %s blocks (%s%% cap) for %s seconds. Gain %s%% speed for %s seconds instead if there are none. Cooldown: %s seconds.",
		StringUtils.multiplierToPercentage(MAGIC_DAMAGE_PER),
		RANGE,
		StringUtils.multiplierToPercentage(MOB_CAP * MAGIC_DAMAGE_PER),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.multiplierToPercentage(SPEED),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_FLAMECALLER;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Flamecaller";
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_FLAMECALLER_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_FLAMECALLER_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_FLAMECALLER_COOLDOWN));
		ichorFlamecaller(plugin, player, 1);
	}

	public static void ichorFlamecaller(Plugin plugin, Player player, double multiplier) {
		int fireCount = 0;
		for (LivingEntity e : EntityUtils.getNearbyMobs(player.getLocation(), RANGE)) {
			if (e.getFireTicks() > 0) {
				fireCount++;
				new PPCircle(Particle.FLAME, e.getLocation(), 1).ringMode(true).count(15).spawnAsPlayerPassive(player);
			}
		}
		if (fireCount > 0) {
			int cappedFireCount = FastMath.min(fireCount, MOB_CAP);
			double buffMultiplier = cappedFireCount * multiplier;
			plugin.mEffectManager.addEffect(player, EFFECT, new PercentDamageDealt(BUFF_DURATION, MAGIC_DAMAGE_PER * buffMultiplier, EnumSet.of(DamageEvent.DamageType.MAGIC)));

			player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.9f + 0.05f * cappedFireCount);
			player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.65f + 0.04f * cappedFireCount);
			player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 1.40f + 0.07f * cappedFireCount);
		} else {
			plugin.mEffectManager.addEffect(player, SPEED_EFFECT, new PercentSpeed(BUFF_DURATION, SPEED * multiplier, SPEED_EFFECT));
			player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.3f, 0.9f);
			new PPCircle(Particle.FLAME, player.getLocation(), 1).ringMode(true).count(15).spawnAsPlayerPassive(player);
		}
	}
}
