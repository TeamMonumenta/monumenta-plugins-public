package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.FirstStrikeCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
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
	private static final double PROJ_REDUCTION = 0.75;
	private static final int DURATION = 3 * 20;
	private static final String SOURCE = "FirstStrikeDisable";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(244, 141, 123), 0.75f);

	private static final EnumSet<DamageEvent.DamageType> MELEE_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH,
		DamageEvent.DamageType.MELEE_SKILL
	);

	private static final EnumSet<DamageEvent.DamageType> PROJ_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.PROJECTILE_SKILL
	);

	private boolean mMeleeAttacked = false;

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
	// resets the flag after 1 tick (at the same time as cd application
	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (type == DamageType.MELEE) {
			mMeleeAttacked = true;
			Bukkit.getScheduler().runTaskLater(plugin, () -> mMeleeAttacked = false, 1);
		}

		//onDamageDelayed does not include projectile damage
		if (PROJ_DAMAGE_TYPES.contains(type) &&
			plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) == null) {
			double bonus = DAMAGE_PER_LEVEL * level * PROJ_REDUCTION;
			triggerFirstStrike(plugin, player, bonus, event, enemy);
		}
	}

	@Override
	public void onDamageDelayed(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();

		if ((MELEE_DAMAGE_TYPES.contains(type)
			&& ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand()))
			|| PROJ_DAMAGE_TYPES.contains(type)) {
			if (plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) == null) {
				double bonus = DAMAGE_PER_LEVEL * level;
				if (PROJ_DAMAGE_TYPES.contains(type)) {
					bonus *= PROJ_REDUCTION;
				}

				// has not melee hit at the same tick, do not trigger first strike
				if (MELEE_DAMAGE_TYPES.contains(type) && !mMeleeAttacked) {
					return;
				}

				// has melee attacked in the same tick, trigger first strike
				triggerFirstStrike(plugin, player, bonus, event, enemy);
			}
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
		enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.8f, 0.45f);

		//delay the cd effect so all damage events of the same tick can get scaled
		Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.mEffectManager.addEffect(enemy,
			SOURCE + player.getName(), new FirstStrikeCooldown(DURATION)), 1);
	}
}
