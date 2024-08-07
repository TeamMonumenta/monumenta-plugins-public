package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
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

public class Regicide implements Enchantment {

	private static final double DAMAGE_BONUS_PER_LEVEL = 0.1;
	private static final double BOSS_BONUS_PER_LEVEL = 0.05;
	public static final String CHARM_DAMAGE = "Regicide Damage";

	@Override
	public String getName() {
		return "Regicide";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGICIDE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 28;
	}

	public static double calculateDamageMultiplier(double level, Player player, LivingEntity target) {
		if (EntityUtils.isElite(target)) {
			return (1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, DAMAGE_BONUS_PER_LEVEL * level));
		} else if (EntityUtils.isBoss(target)) {
			return (1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, BOSS_BONUS_PER_LEVEL * level));
		} else {
			return 1;
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		DamageType type = event.getType();
		if (type != DamageType.AILMENT
			    && type != DamageType.POISON
			    && type != DamageType.FALL
			    && type != DamageType.OTHER
			    && type != DamageType.TRUE
		) {
			double mult = calculateDamageMultiplier(level, player, target);
			event.updateGearDamageWithMultiplier(mult);
			if (mult > 1) {
				World world = target.getWorld();
				Location loc = target.getLocation();
				if (type == DamageType.MELEE) {
					world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.8f, 0.7f);
				}
			}
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ITEM_AXE_SCRAPE, 1.5f, 0.8f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ITEM_TRIDENT_RETURN, 0.7f, 0.8f);
		}
	}
}
