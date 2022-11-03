package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.Adaptability;
import com.playmonumenta.plugins.itemstats.enchantments.Guard;
import com.playmonumenta.plugins.itemstats.enchantments.Inure;
import com.playmonumenta.plugins.itemstats.enchantments.Poise;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.Steadfast;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Armor implements Attribute {

	@Override
	public String getName() {
		return "Armor";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.ARMOR;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// When there is positive Armor, this method runs; otherwise, Agility runs.
		if (value > 0 && event.getType().isDefendable()) {
			boolean adaptability = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0;
			double damageMultiplier = getDamageMultiplier(value, getSecondaryEnchantsMod(event, plugin, player),
				plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY), Agility.getSecondaryEnchantsMod(event, plugin, player),
				getSecondaryEnchantCap(), adaptability, 0, event.getType().isEnvironmental());
			event.setDamage(event.getDamage() * damageMultiplier);
		}
	}

	/**
	 * Calculates the amount to multiply incoming damage with for the given values of armor, agility, etc.
	 *
	 * @param armor         Total armor value
	 * @param armorMods     Total secondary armor enchantment modifier, see {@link #getSecondaryEnchantsMod(DamageEvent, Plugin, Player)}
	 * @param agility       Total agility value
	 * @param agilityMods   Total secondary agility enchantment modifier, see {@link Agility#getSecondaryEnchantsMod(DamageEvent, Plugin, Player)}
	 * @param adaptability  Whether the {@link Adaptability} enchantment is present
	 * @param epf           EPF from protection enchantments
	 * @param environmental Whether the damage is environmental
	 * @return Damage multiplier
	 */
	public static double getDamageMultiplier(double armor, double armorMods, double agility, double agilityMods, double secondaryEnchantsCap, boolean adaptability, double epf, boolean environmental) {
		double armorValueMod = 0;
		double agilityValueMod = 0;
		if (adaptability) {
			if (armor > agility) {
				armorValueMod = armorMods + agilityMods;
			} else {
				agilityValueMod = armorMods + agilityMods;
			}
		} else {
			armorValueMod = armorMods;
			agilityValueMod = agilityMods;
		}

		double armorBonus = Math.min(armor, secondaryEnchantsCap) * armorValueMod;
		double agilityBonus = Math.min(agility, secondaryEnchantsCap) * agilityValueMod;

		return DamageUtils.getDamageMultiplier(armor + armorBonus, agility + agilityBonus, epf, environmental);
	}

	/**
	 * Gets the maximum armor/agility points that may be increased by secondary enchants for the current region.
	 * Any additional points will still provide defense, but won't be affected by secondary enchants.
	 */
	public static double getSecondaryEnchantCap() {
		return getSecondaryEnchantCap(ServerProperties.getClassSpecializationsEnabled());
	}

	public static double getSecondaryEnchantCap(boolean region2) {
		return region2 ? 30 : 20;
	}

	/**
	 * Gets the total modifier (0-based) of armor from secondary enchants for the given event and player.
	 */
	public static double getSecondaryEnchantsMod(DamageEvent event, Plugin plugin, Player player) {
		return Shielding.applyShielding(event, plugin, player)
			       + Inure.applyInure(event, plugin, player)
			       + Steadfast.applySteadfast(event, plugin, player)
			       + Poise.applyPoise(event, plugin, player)
			       + Guard.applyGuard(event, plugin, player);
	}

}
