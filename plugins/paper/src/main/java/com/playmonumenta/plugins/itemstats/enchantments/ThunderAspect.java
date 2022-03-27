package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class ThunderAspect implements Enchantment {
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	@Override
	public String getName() {
		return "Thunder Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.THUNDER_ASPECT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 14;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getDamager() instanceof Trident) {
			apply(plugin, player, level, enemy);
		}
	}

	public static void apply(Plugin plugin, Player player, double level, LivingEntity enemy) {
		double rand = FastUtils.RANDOM.nextDouble();
		World world = enemy.getWorld();

		if (rand < (level * 0.1 * player.getCooledAttackStrength(0))) {
			if (EntityUtils.isElite(enemy)) {
				EntityUtils.applyStun(plugin, 10, enemy);
			} else {
				EntityUtils.applyStun(plugin, 50, enemy);
			}

			if (!(EntityUtils.isBoss(enemy) || enemy.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
				Location loc = enemy.getLocation();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
				loc = loc.add(0, 1, 0);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, YELLOW_1_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, YELLOW_2_COLOR);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15);
			}
		}
	}
}


