package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.Ethereal;
import com.playmonumenta.plugins.itemstats.enchantments.Evasion;
import com.playmonumenta.plugins.itemstats.enchantments.Inure;
import com.playmonumenta.plugins.itemstats.enchantments.Poise;
import com.playmonumenta.plugins.itemstats.enchantments.Reflexes;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.Steadfast;
import com.playmonumenta.plugins.itemstats.enchantments.Tempo;
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
		// When there is nonzero Armor, this method runs; otherwise, Agility runs.
		if (value > 0 && event.getType().isDefendable()) {
			double valueMod = 0;
			double agilValueMod = 0;
			valueMod += Shielding.applyShielding(event, plugin, player);
			valueMod += Inure.applyInure(event, plugin, player);
			valueMod += Steadfast.applySteadfast(event, plugin, player);
			valueMod += Poise.applyPoise(event, plugin, player);

			if ((plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR) > plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY)) &&
					(plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0)) {
				valueMod += Tempo.applyTempo(event, plugin, player);
				valueMod += Reflexes.applyReflexes(event, plugin, player);
				valueMod += Evasion.applyEvasion(event, plugin, player);
				valueMod += Ethereal.applyEthereal(event, plugin, player);
			} else if ((plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR) < plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY)) &&
				(plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0)) {
				agilValueMod += Shielding.applyShielding(event, plugin, player);
				agilValueMod += Inure.applyInure(event, plugin, player);
				agilValueMod += Steadfast.applySteadfast(event, plugin, player);
				agilValueMod += Poise.applyPoise(event, plugin, player);
				agilValueMod += Tempo.applyTempo(event, plugin, player);
				agilValueMod += Reflexes.applyReflexes(event, plugin, player);
				agilValueMod += Evasion.applyEvasion(event, plugin, player);
				agilValueMod += Ethereal.applyEthereal(event, plugin, player);
			} else if (plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY) > 0) {
				agilValueMod += Tempo.applyTempo(event, plugin, player);
				agilValueMod += Reflexes.applyReflexes(event, plugin, player);
				agilValueMod += Evasion.applyEvasion(event, plugin, player);
				agilValueMod += Ethereal.applyEthereal(event, plugin, player);
			}

			double armorCap = ServerProperties.getClassSpecializationsEnabled() ? 30 : 20;
			double valueBonus = Math.min(value, armorCap) * valueMod;
			double agilValueBonus = Math.min(plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY), armorCap) * agilValueMod;
			value += valueBonus;

			event.setDamage(event.getDamage() * DamageUtils.getDamageMultiplier(value, Math.max(0, plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY) + agilValueBonus), 0, event.getType().isEnvironmental()));
		}
	}

}
