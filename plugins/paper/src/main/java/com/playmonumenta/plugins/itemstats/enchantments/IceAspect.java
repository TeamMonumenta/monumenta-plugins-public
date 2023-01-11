package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.LocationUtils;
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

public class IceAspect implements Enchantment {
	public static final int ICE_ASPECT_DURATION = 20 * 4;
	public static final double SLOW_PER_LEVEL = 0.1;
	public static final float BONUS_DAMAGE = 1.0f;
	private static final Particle.DustOptions COLOR_LIGHT_BLUE = new Particle.DustOptions(Color.fromRGB(85, 170, 255), 0.75f);
	public static final String CHARM_SLOW = "Ice Aspect Slow Amplifier";
	public static final String CHARM_DURATION = "Ice Aspect Slow Duration";

	@Override
	public String getName() {
		return "Ice Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
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
				world.playSound(
					enemyLocation,
					Sound.BLOCK_SOUL_SAND_PLACE,
					SoundCategory.PLAYERS,
					1f,
					0.5f
				);
				world.playSound(
					enemyLocation,
					Sound.BLOCK_GLASS_BREAK,
					SoundCategory.PLAYERS,
					0.75f,
					1.1f
				);
				world.playSound(
					enemyLocation,
					Sound.BLOCK_GLASS_BREAK,
					SoundCategory.PLAYERS,
					0.5f,
					1.3f
				);
			}

			apply(plugin, player, level, duration, enemy, type == DamageType.MELEE);

			if (enemy instanceof Blaze) {
				event.setDamage(event.getDamage() + 1.0);
			}
		}
	}

	public static void apply(Plugin plugin, Player player, double level, int duration, LivingEntity enemy, boolean particles) {
		EntityUtils.applySlow(plugin, CharmManager.getDuration(player, CHARM_DURATION, duration), (level * SLOW_PER_LEVEL) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW), enemy);
		if (particles) {
			new PartialParticle(Particle.SNOWBALL, enemy.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerBuff(player);
		}
	}
}
