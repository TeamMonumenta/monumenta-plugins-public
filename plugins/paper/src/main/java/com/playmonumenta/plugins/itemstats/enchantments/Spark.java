package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.player.PartialParticle.DeltaVarianceGroup;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Spark implements Enchantment {

	private static final Particle.DustOptions COLOUR_YELLOW
		= new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOUR_FAINT_YELLOW
		= new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public String getName() {
		return "Spark";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SPARK;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 15;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE) {
			boolean doEffects = false;
			if (enemy instanceof Guardian || enemy instanceof IronGolem) {
				doEffects = true;
				event.setDamage(event.getDamage() + 1);
			}
			if (!(EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy)) && FastUtils.randomDoubleInRange(0, 1) > 0.5) {
				doEffects = true;
				EntityUtils.applyStun(plugin, Constants.TICKS_PER_SECOND / 2, enemy);
			}

			if (doEffects) {
				Location halfHeightLocation = LocationUtils.getHalfHeightLocation(enemy);
				double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
				// /particle dust 1 0.945 0.431 1 7053 78.9 7069 0.225 0.45 0.225 0 10
				PartialParticle partialParticle = new PartialParticle(
					Particle.REDSTONE,
					halfHeightLocation,
					10,
					widerWidthDelta,
					PartialParticle.getHeightDelta(enemy),
					widerWidthDelta,
					0,
					COLOUR_FAINT_YELLOW
				).spawnAsEnemy();
				// /particle dust 0.984 0.906 0.118 1 7053 78.9 7069 0.225 0.45 0.225 0 10
				partialParticle.mExtra = 1;
				partialParticle.mData = COLOUR_YELLOW;
				partialParticle.spawnAsEnemy();
				// /particle firework 7053 78.9 7069 0.225 0.45 0.225 0.5 0
				partialParticle.mParticle = Particle.FIREWORKS_SPARK;
				partialParticle.mCount = 15;
				partialParticle.mExtra = 0.4;
				partialParticle.mData = null;
				partialParticle.mDirectionalMode = true;
				partialParticle.mExtraVariance = 0.1;
				partialParticle.setDeltaVariance(DeltaVarianceGroup.VARY_X, true);
				partialParticle.setDeltaVariance(DeltaVarianceGroup.VARY_Z, true);
				partialParticle.mVaryPositiveY = true;
				partialParticle.spawnAsEnemy();

				World world = enemy.getWorld();
				Location enemyLocation = enemy.getLocation();
				// /playsound entity.firework_rocket.twinkle master @p ~ ~ ~ 0.5 1.5
				world.playSound(
					enemyLocation,
					Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
					SoundCategory.PLAYERS,
					0.5f,
					1.5f
				);
				// /playsound entity.firework_rocket.twinkle_far master @p ~ ~ ~ 0.5 1.2
				world.playSound(
					enemyLocation,
					Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
					SoundCategory.PLAYERS,
					0.5f,
					1.2f
				);
			}
		}
	}
}
