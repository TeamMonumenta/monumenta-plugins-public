package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;



public class Spark implements BaseEnchantment {
	private static final String METADATA_SPARK_KEY = "spark_arrow";
	private static final String METADATA_STUN_KEY = "spark_stun_arrow";

	private static final Particle.DustOptions COLOUR_YELLOW
		= new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOUR_FAINT_YELLOW
		= new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public @NotNull String getProperty() {
		return "Spark";
	}

	@Override
	public @NotNull EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public void onLaunchProjectile(
		@NotNull Plugin plugin,
		@NotNull Player player,
		int level,
		@NotNull Projectile projectile,
		@NotNull ProjectileLaunchEvent projectileLaunchEvent
	) {
		if (EntityUtils.isSomeArrow(projectile)) {
			// Eligible arrow is always a Spark arrow for extra damage
			projectile.setMetadata(METADATA_SPARK_KEY, new FixedMetadataValue(plugin, 1));

			//TODO change event in EntityListener, from ProjectileLaunchEvent.
			// Then can safely & accurately check if bow or crossbow.
			// Crossbows just happen to have the right chance here due to higher arrow launch speed

			// Whether Spark arrow is also stun arrow, scaling to 50% with bow draw.
			// Need to precalculate bow draw based on launch velocity, velocity will change later
			double bowDraw = PlayerUtils.calculateBowDraw((AbstractArrow)projectile);
			if (bowDraw / 2 > FastUtils.RANDOM.nextDouble()) {
				// Spark only supports a single level. It does not use enchant levels,
				// & attempting to offhand double Spark as a higher level should be ignored
				projectile.setMetadata(METADATA_STUN_KEY, new FixedMetadataValue(plugin, 1));
			}
		}
	}

	//TODO some kind of onProjectileDamage() as part of PlayerListener for these numerous manual
	// onShootAttack() calls
	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(
		@NotNull Plugin plugin,
		@NotNull Projectile projectile,
		@NotNull LivingEntity enemy,
		@NotNull EntityDamageByEntityEvent entityDamageByEntityEvent
	) {
		if (projectile.hasMetadata(METADATA_SPARK_KEY)) {
			boolean doEffects = false;
			if (enemy instanceof Guardian || enemy instanceof IronGolem) {
				doEffects = true;
				entityDamageByEntityEvent.setDamage(entityDamageByEntityEvent.getDamage() + 1);
			}
			if (
				projectile.hasMetadata(METADATA_STUN_KEY)
				&& !(EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy))
			) {
				doEffects = true;
				EntityUtils.applyStun(plugin, Constants.TICKS_PER_SECOND / 2, enemy);
			}

			if (doEffects) {
				@NotNull Location halfHeightLocation = LocationUtils.getHalfHeightLocation(enemy);
				double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
				//TODO pass in the shooter of the projectile,
				// then can safely spawn as that player's own active
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
				partialParticle.mIsDirectional = true;
				partialParticle.mExtraVariance = 0.1;
				partialParticle.spawnAsEnemy();

				@NotNull World world = enemy.getWorld();
				@NotNull Location enemyLocation = enemy.getLocation();
				// /playsound entity.firework_rocket.twinkle_far master @p ~ ~ ~ 0.3 1.2
				world.playSound(
					enemyLocation,
					Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
					SoundCategory.PLAYERS,
					0.3f,
					1f
				);
				// /playsound entity.firework_rocket.twinkle master @p ~ ~ ~ 0.3 1.5
				world.playSound(
					enemyLocation,
					Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
					SoundCategory.PLAYERS,
					0.3f,
					1.5f
				);
			}
		}
	}
}