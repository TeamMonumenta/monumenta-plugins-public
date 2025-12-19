package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class IceAspect implements Enchantment {
	public static final int ICE_ASPECT_DURATION = 20 * 4;
	public static final double SLOW_PER_LEVEL = 0.1;
	public static final float BONUS_DAMAGE = 1.0f;
	private static final Particle.DustOptions COLOR_LIGHT_BLUE = new Particle.DustOptions(Color.fromRGB(85, 170, 255), 0.75f);

	@Override
	public String getName() {
		return "Ice Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 12;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ICE_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			int duration = (int) (ICE_ASPECT_DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			if (type == DamageType.PROJECTILE) {
				double widthDelta = PartialParticle.getWidthDelta(enemy);
				double widerWidthDelta = widthDelta * 1.5;
				double doubleWidthDelta = widthDelta * 2;
				double heightDelta = PartialParticle.getHeightDelta(enemy);
				// /particle falling_dust light_blue_concrete 7053 78.9 7069 0.225 0.45 0.225 1 15
				new PartialParticle(
					Particle.FALLING_DUST,
					LocationUtils.getHalfHeightLocation(enemy),
					15,
					widerWidthDelta,
					heightDelta,
					widerWidthDelta,
					1,
					Material.LIGHT_BLUE_CONCRETE.createBlockData()
				).spawnAsEnemy();
				// /particle dust 0.333 0.667 1 0.75 7053 78.45 7069 0.3 0.225 0.3 1 10
				PartialParticle partialParticle = new PartialParticle(
					Particle.REDSTONE,
					LocationUtils.getHeightLocation(enemy, 0.25),
					10,
					doubleWidthDelta,
					heightDelta / 2,
					doubleWidthDelta,
					1,
					COLOR_LIGHT_BLUE
				).spawnAsEnemy();
				// /particle dolphin 7053 78 7069 0.3 0.225 0.3 0 50
				partialParticle.mParticle = Particle.DOLPHIN;
				partialParticle.mLocation = enemy.getLocation();
				// Dolphin particles are small
				partialParticle.mCount *= 5;
				partialParticle.mExtra = 0;
				partialParticle.mData = null;
				partialParticle.spawnAsEnemy();
				// /particle item_snowball 7053 78 7069 0.3 0.225 0.3 0 10
				partialParticle.mParticle = Particle.SNOWBALL;
				partialParticle.mCount = 10;
				partialParticle.spawnAsEnemy();

				World world = enemy.getWorld();
				Location enemyLocation = enemy.getLocation();
				world.playSound(enemyLocation, Sound.BLOCK_SOUL_SAND_PLACE, SoundCategory.PLAYERS, 1f, 0.5f);
				world.playSound(enemyLocation, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.75f, 1.1f);
				world.playSound(enemyLocation, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 1.3f);
			}

			apply(plugin, player, level, duration, enemy, type == DamageType.MELEE);

			if (enemy instanceof Blaze) {
				event.addUnmodifiableDamage(BONUS_DAMAGE);
			}
		}
	}

	public static void apply(Plugin plugin, Player player, double level, int duration, LivingEntity enemy, boolean particles) {
		if (!EntityUtils.isSlowed(plugin, enemy) && !enemy.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW)) {
			MetadataUtils.checkOnceThisTick(plugin, enemy, Constants.ENTITY_SLOWED_NONCE_METAKEY);
		}
		EntityUtils.applySlow(plugin, duration, level * SLOW_PER_LEVEL, enemy);
		if (particles) {
			new PartialParticle(Particle.SNOWBALL, enemy.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerBuff(player);
			World world = enemy.getWorld();
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.45f, 1.1f);
			world.playSound(loc, Sound.ENTITY_TURTLE_HURT_BABY, SoundCategory.PLAYERS, 0.45f, 0.7f);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_TURTLE_HURT_BABY, 0.6f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ITEM_TRIDENT_HIT, 0.6f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_VEX_HURT, 2.0f, 0.6f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_GLASS_BREAK, 0.4f, 0.7f);
		}
	}
}
