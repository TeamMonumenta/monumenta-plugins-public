package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.VengefulTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Vengeful implements Infusion {

	private static final double DAMAGE_MLT_PER_LVL = 0.02;
	private static final int DURATION = 30 * 20;
	private static final String EFFECT_NAME = "VengefulEffect";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1f);

	@Override
	public String getName() {
		return "Vengeful";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.VENGEFUL;
	}

	@Override
	public double getPriorityAmount() {
		return 29;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		if (checkLastDamage(player, enemy)) {
			event.setDamage(event.getDamage() * getDamageDealtMultiplier(modifiedLevel));
			Location halfHeightLocation = LocationUtils.getHalfHeightLocation(enemy);
			double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
			PartialParticle partialParticle = new PartialParticle(
				Particle.REDSTONE,
				halfHeightLocation,
				8,
				widerWidthDelta,
				PartialParticle.getHeightDelta(enemy),
				widerWidthDelta,
				0,
				COLOR
			).spawnAsEnemy();
			partialParticle.mExtra = 1;
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getSource() != null) {
			plugin.mEffectManager.clearEffects(player, EFFECT_NAME + player.getName());
			plugin.mEffectManager.addEffect(player, EFFECT_NAME + player.getName(), new VengefulTag(DURATION, player, event.getSource()));
			plugin.mEffectManager.addEffect(event.getSource(), EFFECT_NAME + player.getName(), new VengefulTag(DURATION, player, event.getSource()));
		}

	}

	private boolean checkLastDamage(Player player, LivingEntity enemy) {
		NavigableSet<Effect> playerEffs = Plugin.getInstance().mEffectManager.getEffects(player, EFFECT_NAME + player.getName());
		NavigableSet<Effect> enemyEffs = Plugin.getInstance().mEffectManager.getEffects(enemy, EFFECT_NAME + player.getName());

		Player effPlayerA;
		LivingEntity effEntityA;
		Player effPlayerB;
		LivingEntity effEntityB;

		if (playerEffs != null) {
			VengefulTag pEff = (VengefulTag) playerEffs.last();
			effPlayerA = pEff.getPlayer();
			effEntityA = pEff.getEnemy();
		} else {
			return false;
		}

		if (enemyEffs != null) {
			VengefulTag eEff = (VengefulTag) enemyEffs.last();
			effPlayerB = eEff.getPlayer();
			effEntityB = eEff.getEnemy();
		} else {
			return false;
		}

		if (effPlayerA == null || effPlayerB == null || effEntityA == null || effEntityB == null) {
			return false;
		} else {
			return (effPlayerA == effPlayerB && effEntityA == effEntityB);
		}
	}

	public static double getDamageDealtMultiplier(double level) {
		return 1 + DAMAGE_MLT_PER_LVL * level;
	}

}
