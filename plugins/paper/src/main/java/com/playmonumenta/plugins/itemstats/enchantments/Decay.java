package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public class Decay implements Enchantment {

	public static final int DURATION = 20 * 4;
	public static final double DAMAGE = 2;
	public static final String DOT_EFFECT_NAME = "DecayDamageOverTimeEffect";

	@Override
	public String getName() {
		return "Decay";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DECAY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 16;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			DamageType type = event.getType();
			int duration = (int) (DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			apply(plugin, enemy, duration, value, player, type);
		}
	}

	public static void apply(Plugin plugin, LivingEntity enemy, int duration, double decayLevel, Player player, DamageType type) {
		double desiredPeriod = 40 / decayLevel;
		if (desiredPeriod > DURATION) { // Can happen with enchantment reductions from region scaling
			return;
		}
		// The DoT effect only runs every 5 ticks, so select the period as a multiple of 5 ticks and adjust damage instead to match expected DPS
		int adjustedPeriod = (int) Math.ceil(desiredPeriod / 5) * 5;
		double damage = DAMAGE * adjustedPeriod / desiredPeriod;
		plugin.mEffectManager.addEffect(enemy, DOT_EFFECT_NAME,
			new CustomDamageOverTime(DURATION, damage, adjustedPeriod, player, plugin.mItemStatManager.getPlayerItemStatsCopy(player), null, DamageType.AILMENT)
				.setVisuals(entity -> {
					double xRad = entity.getBoundingBox().getWidthX() / 2;
					double yRad = entity.getBoundingBox().getHeight() / 2;
					double zRad = entity.getBoundingBox().getWidthZ() / 2;
					// TODO: When we get 1.20.5, use Infested particles
					// Rotting green -> black transition from centre
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getEntityCenter(entity), 10,
						xRad + 0.12, yRad, zRad + 0.12,
						0.1)
						.data(new Particle.DustTransition(
							Color.fromRGB(30, 65, 10), Color.fromRGB(0, 5, 0), 0.9f))
						.spawnAsEnemyBuff();
					// Rising smoke
					new PPCircle(Particle.SMOKE_NORMAL, entity.getLocation(), 1.1)
						.axes(new Vector(xRad, 0, 0), new Vector(0, 0, zRad))
						.directionalMode(true)
						.ringMode(true)
						.count(14)
						.delta(0.04, 0.14 * yRad, 0.04)
						.deltaVariance(true, true, true, false, true, true)
						.extra(1)
						.spawnAsEnemyBuff();
					// Soul particles
					new PPCircle(Particle.SCULK_SOUL, entity.getLocation(), 0.9)
						.axes(new Vector(xRad, 0, 0), new Vector(0, 0, zRad))
						.directionalMode(true)
						.ringMode(false)
						.count(6)
						.delta(0.04, 0.1 * (yRad + 0.5), 0.04)
						.extra(1)
						.deltaVariance(true, true, true, false, true, true)
						.spawnAsEnemyBuff();
					// Dust scattered over the enemy's head
					new PartialParticle(Particle.FALLING_DUST, entity.getEyeLocation(), 4,
						xRad,
						0.3,
						zRad,
						Material.WARPED_HYPHAE.createBlockData())
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.FALLING_DUST, entity.getEyeLocation(), 4,
						xRad,
						0.3,
						zRad,
						Material.OBSIDIAN.createBlockData())
						.spawnAsPlayerActive(player);
				}));

		if (type == DamageType.MELEE) {
			World world = enemy.getWorld();
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.35f, 0.9f);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_WITHER_SHOOT, 0.05f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.4f, 0.7f);
		}
	}
}
