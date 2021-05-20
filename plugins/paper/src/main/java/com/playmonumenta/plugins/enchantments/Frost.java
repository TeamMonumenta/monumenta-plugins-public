package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;



public class Frost implements BaseEnchantment {
	private static final String METADATA_KEY = "frost_arrow";
	private static final String SOURCE_SLOWNESS = "frost_slowness";

	private static final Particle.DustOptions COLOUR_LIGHT_BLUE
		= new Particle.DustOptions(Color.fromRGB(85, 170, 255), 0.75f);

	@Override
	public @NotNull String getProperty() {
		return "Frost";
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
			projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, 1));
		}
	}

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
		if (projectile.hasMetadata(METADATA_KEY)) {
			if (enemy instanceof Blaze) {
				entityDamageByEntityEvent.setDamage(entityDamageByEntityEvent.getDamage() + 1);
			}

			plugin.mEffectManager.addEffect(
				enemy,
				SOURCE_SLOWNESS,
				new PercentSpeed(
					4 * Constants.TICKS_PER_SECOND,
					-0.2,
					SOURCE_SLOWNESS
				)
			);

			double widthDelta = PartialParticle.getWidthDelta(enemy);
			double widerWidthDelta = widthDelta * 1.5;
			double doubleWidthDelta = widthDelta * 2;
			double heightDelta = PartialParticle.getHeightDelta(enemy);
			//TODO pass in the shooter of the projectile,
			// then can safely spawn as that player's own active
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
				COLOUR_LIGHT_BLUE
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

			@NotNull World world = enemy.getWorld();
			@NotNull Location enemyLocation = enemy.getLocation();
			// /playsound block.soul_sand.place master @p ~ ~ ~ 1 0.5
			world.playSound(
				enemyLocation,
				Sound.BLOCK_SOUL_SAND_PLACE,
				SoundCategory.PLAYERS,
				1f,
				0.5f
			);
			// /playsound block.glass.break master @p ~ ~ ~ 0.75 1.1
			world.playSound(
				enemyLocation,
				Sound.BLOCK_GLASS_BREAK,
				SoundCategory.PLAYERS,
				0.75f,
				1.1f
			);
			// /playsound block.glass.break master @p ~ ~ ~ 0.5 1.3
			world.playSound(
				enemyLocation,
				Sound.BLOCK_GLASS_BREAK,
				SoundCategory.PLAYERS,
				0.5f,
				1.3f
			);

			//TODO Refactor EffectManager to have accurate apply, reapply & remove events.
			// Then can override a special base movement effect with negative speed;
			// can have special effects on first apply/reapply (current),
			// while active (normal ice particles)
			// & especially when finally removed
			// (directional "burst out" particles + glass break sound etc,
			// maybe a tiny bit of upwards velocity)
		}
	}
}