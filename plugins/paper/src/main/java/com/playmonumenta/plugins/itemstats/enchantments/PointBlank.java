package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PointBlank implements Enchantment {
	public static final int DISTANCE = 8;
	public static final int DAMAGE_PER_LEVEL = 2;

	@Override
	public String getName() {
		return "Point Blank";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.POINT_BLANK;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 8;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() == DamageType.PROJECTILE && !event.isCancelled()) {
			double effectiveness = 1.0;
			if (event.getDamager() instanceof AbstractArrow arrow) {
				// calculateBowDraw handles TK, crossbows and tridents automatically
				effectiveness = PlayerUtils.calculateBowDraw(arrow);
			}

			event.setFlatDamage(event.getFlatDamage() + effectiveness * apply(player, target, level));
		}
	}

	public static double apply(Player player, LivingEntity target, double level) {
		return apply(player, target.getEyeLocation(), level);
	}

	public static double apply(Player player, Location target, double level) {
		if (level > 0 && player.getEyeLocation().distance(target) < DISTANCE) {
			particles(target, player);
			return (level * DAMAGE_PER_LEVEL);
		}
		return 0;
	}

	public static void particles(Location loc, Player player) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 30, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.PLAYERS, 1.5f, 0.75f);
	}
}
