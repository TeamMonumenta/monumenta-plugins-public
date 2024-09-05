package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class Decay implements Enchantment {

	public static final int DURATION = 20 * 4;
	public static final double DAMAGE = 2;
	public static final String DOT_EFFECT_NAME = "DecayDamageOverTimeEffect";

	@Override
	public String getName() {
		return "Decay";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DECAY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 16;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			DamageType type = event.getType();
			int duration = (int) (DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			apply(plugin, enemy, duration, value, player, type);
		}
	}

	public static void apply(Plugin plugin, LivingEntity enemy, int duration, double decayLevel, Player player, DamageType type) {
		double desiredPeriod = 40 / decayLevel;
		if (desiredPeriod > DURATION) { // Can happen with enchantment reductions from region scaling
			return;
		}
		// The DoT effect only runs every 5 ticks, so select the period as a multiple of 5 ticks and adjust damage instead to match expected DPS
		int adjustedPeriod = (int) Math.ceil(desiredPeriod / 5) * 5;
		double damage = DAMAGE * adjustedPeriod / desiredPeriod;
		plugin.mEffectManager.addEffect(enemy, DOT_EFFECT_NAME,
			new CustomDamageOverTime(DURATION, damage, adjustedPeriod, player, plugin.mItemStatManager.getPlayerItemStatsCopy(player), null, DamageType.AILMENT));

		if (type == DamageType.MELEE) {
			World world = enemy.getWorld();
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.35f, 0.9f);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_WITHER_SHOOT, 0.05f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.4f, 0.7f);
		}
	}
}
