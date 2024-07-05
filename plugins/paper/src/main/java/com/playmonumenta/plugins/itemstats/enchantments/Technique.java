package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Technique implements Enchantment {
	private static final double DAMAGE_PER_LEVEL = 0.1;
	private static final double PROJ_REDUCTION = 0.75;
	private static final double DISTANCE_SQUARED = 9;

	public static boolean withinDistance(Player player, @Nullable LivingEntity source) {
		return source != null && player.getWorld() == source.getWorld() && player.getLocation().distanceSquared(source.getLocation()) <= DISTANCE_SQUARED;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if ((event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MAGIC ||
			     event.getType() == DamageType.PROJECTILE || event.getType() == DamageType.PROJECTILE_SKILL) &&
			    withinDistance(player, enemy)) {
			double bonus = DAMAGE_PER_LEVEL * level;
			if (event.getType() == DamageType.PROJECTILE) {
				bonus *= PROJ_REDUCTION;
			}
			event.updateGearDamageWithMultiplier(1 + bonus);

			player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1f);
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.9f, 0.8f);

			double widthDelta = PartialParticle.getWidthDelta(enemy);
			double heightDelta = PartialParticle.getHeightDelta(enemy);
			new PartialParticle(Particle.SWEEP_ATTACK, enemy.getEyeLocation(), 3, widthDelta * 1.5, heightDelta, widthDelta * 1.5).spawnAsEnemy();
			new PartialParticle(Particle.REDSTONE, enemy.getEyeLocation(), 6, widthDelta * 1.5, heightDelta, widthDelta * 1.5, new Particle.DustOptions(Color.fromRGB(252, 80, 80), 1.1f)).spawnAsEnemy();
		}
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TECHNIQUE;
	}

	@Override
	public String getName() {
		return "Technique";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 999; //same as first strike
	}
}
