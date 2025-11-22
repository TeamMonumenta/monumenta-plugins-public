package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.GearDamageIncrease;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.particle.PPLine;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class Fervor implements Infusion {
	private static final int DURATION = TICKS_PER_SECOND * 3;
	private static final int BUFF_DURATION_THRESHOLD = TICKS_PER_SECOND * 5;
	public static final double PERCENT_DAMAGE_PER_LEVEL = 0.015;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "FervorPercentDamageEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeProjectileAndMagicTypes();

	@Override
	public String getName() {
		return "Fervor";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.FERVOR;
	}

	@Override
	public void onCustomEffectApply(Plugin plugin, Player player, double value, CustomEffectApplyEvent event) {
		Effect effect = event.getEffect();
		if (effect.isBuff() && effect.getDuration() >= BUFF_DURATION_THRESHOLD && !event.getSource().startsWith("PatronShrine")) {
			Location playerLoc = player.getLocation().clone();
			new PPLine(Particle.REDSTONE, playerLoc.clone().add(0, 1, 0).add(1, 1, 1), playerLoc.clone().add(0, 1, 0).add(-1, -1, -1))
				.count(8)
				.data(new Particle.DustOptions(Color.fromRGB(0, 204, 0), 1.0f))
				.spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, playerLoc.clone().add(0, 1, 0).add(1, 1, -1), playerLoc.clone().add(0, 1, 0).add(-1, -1, 1))
				.count(8)
				.data(new Particle.DustOptions(Color.fromRGB(153, 76, 37), 1.0f))
				.spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, playerLoc.clone().add(0, 1, 0).add(-1, 1, 1), playerLoc.clone().add(0, 1, 0).add(1, -1, -1))
				.count(8)
				.data(new Particle.DustOptions(Color.fromRGB(0, 204, 0), 1.0f))
				.spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, playerLoc.clone().add(0, 1, 0).add(-1, 1, -1), playerLoc.clone().add(0, 1, 0).add(1, -1, 1))
				.count(8)
				.data(new Particle.DustOptions(Color.fromRGB(153, 76, 37), 1.0f))
				.spawnAsPlayerActive(player);

			double percentDamage = getDamageDealtMultiplier(value) - 1;
			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_EFFECT_NAME,
				new GearDamageIncrease(DURATION, percentDamage).damageTypes(AFFECTED_DAMAGE_TYPES));
		}
	}

	public static double getDamageDealtMultiplier(double level) {
		return 1 + PERCENT_DAMAGE_PER_LEVEL * level;
	}
}
