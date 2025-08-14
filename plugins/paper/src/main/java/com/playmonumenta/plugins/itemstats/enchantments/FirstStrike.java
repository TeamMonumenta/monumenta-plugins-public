package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.FirstStrikeCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FirstStrike implements Enchantment {

	private static final double DAMAGE_PER_LEVEL = 0.1;
	private static final int DURATION = 3 * 20;
	private static final String SOURCE = "FirstStrikeDisable";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(244, 141, 123), 0.75f);

	private static final EnumSet<DamageEvent.DamageType> SAME_TICK_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE_SKILL,
		DamageType.PROJECTILE_ENCH
	);

	private static final EnumSet<DamageEvent.DamageType> ACTIVATION_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL
	);

	private boolean mAttacked = false;

	@Override
	public String getName() {
		return "First Strike";
	}

	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRST_STRIKE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	// runs this first before damage is calculated, sets melee attack check to true if normally attacked
	// resets the flag after 1 tick (at the same time as cd application)
	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (ACTIVATION_DAMAGE_TYPES.contains(type)) {
			mAttacked = true;
			Bukkit.getScheduler().runTaskLater(plugin, () -> mAttacked = false, 1);
		}

		//onDamageDelayed does not include potion damage
		// For some godforsaken reason, onDamageDelayed does not play nice with PROJECTILE type damage. It does not accurately update the damage despite passing through the triggerFirstStrike method.
		// As a workaround, PROJECTILE damage will mark the mob to take increased damage that tick but will instead have its damage boost handled here.
		if ((event.getAbility() == ClassAbility.ALCHEMIST_POTION || event.getType() == DamageType.PROJECTILE) &&
			plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) == null) {
			double bonus = DAMAGE_PER_LEVEL * level;
			triggerFirstStrike(plugin, player, bonus, event, enemy);
		}
	}

	@Override
	public void onDamageDelayed(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();

		if (SAME_TICK_DAMAGE_TYPES.contains(type)
			&& plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) == null
			&& mAttacked) {
			double bonus = DAMAGE_PER_LEVEL * level;

			// has attacked in the same tick, trigger first strike
			triggerFirstStrike(plugin, player, bonus, event, enemy);
		}
	}

	public void triggerFirstStrike(Plugin plugin, Player player, double bonus, DamageEvent event, LivingEntity enemy) {
		event.updateGearDamageWithMultiplier(1 + bonus);

		double widthDelta = PartialParticle.getWidthDelta(enemy);
		double doubleWidthDelta = widthDelta * 2;
		double heightDelta = PartialParticle.getHeightDelta(enemy);

		new PartialParticle(Particle.CRIT, LocationUtils.getHeightLocation(enemy, 0.8), 8, doubleWidthDelta,
			heightDelta / 2, doubleWidthDelta).spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHeightLocation(enemy, 0.8), 6, doubleWidthDelta,
			heightDelta / 2, doubleWidthDelta, 1, COLOR).spawnAsEnemy();
		enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.8f, event.getAbility() == ClassAbility.ALCHEMIST_POTION ? 0.3f : 0.45f);

		//delay the cd effect so all damage events of the same tick can get scaled
		Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.mEffectManager.addEffect(enemy,
			SOURCE + player.getName(), new FirstStrikeCooldown(DURATION)), 1);
	}
}
