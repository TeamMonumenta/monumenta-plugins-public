package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
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
import org.bukkit.entity.Trident;

import java.util.EnumSet;

public class Frost implements Enchantment {

	private static final int DURATION = 4 * 20;
	private static final double SLOW_EFFECT = 0.2;
	private static final Particle.DustOptions COLOUR_LIGHT_BLUE = new Particle.DustOptions(Color.fromRGB(85, 170, 255), 0.75f);

	@Override
	public String getName() {
		return "Frost";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FROST;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 13;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && !(event.getDamager() instanceof Trident)) {
			if (enemy instanceof Blaze) {
				event.setDamage(event.getDamage() + 1);
			}

			EntityUtils.applySlow(plugin, DURATION, SLOW_EFFECT, enemy);

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

			World world = enemy.getWorld();
			Location enemyLocation = enemy.getLocation();
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
		}
	}
}
