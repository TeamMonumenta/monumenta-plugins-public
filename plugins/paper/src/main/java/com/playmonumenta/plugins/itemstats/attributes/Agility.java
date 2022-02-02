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

public class Agility implements Attribute {

	@Override
	public String getName() {
		return "Agility";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.AGILITY;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// When there is zero Armor, this method runs; otherwise, Armor runs.
		if (value > 0 && event.getType().isDefendable() && plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR) <= 0) {
			double valueMod = 0;
			valueMod += Tempo.applyTempo(event, plugin, player);
			valueMod += Reflexes.applyReflexes(event, plugin, player);
			valueMod += Evasion.applyEvasion(event, plugin, player);
			valueMod += Ethereal.applyEthereal(event, plugin, player);

			if ((plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY) > plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR)) &&
				    (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0)) {
				valueMod += Shielding.applyShielding(event, plugin, player);
				valueMod += Inure.applyInure(event, plugin, player);
				valueMod += Steadfast.applySteadfast(event, plugin, player);
				valueMod += Poise.applyPoise(event, plugin, player);
			}

			double armorCap = ServerProperties.getClassSpecializationsEnabled() ? 30 : 20;
			double valueBonus = Math.min(value, armorCap) * valueMod;
			value += valueBonus;

			event.setDamage(event.getDamage() * DamageUtils.getDamageMultiplier(0, value, 0, event.getType().isEnvironmental()));
		}
	}

}
