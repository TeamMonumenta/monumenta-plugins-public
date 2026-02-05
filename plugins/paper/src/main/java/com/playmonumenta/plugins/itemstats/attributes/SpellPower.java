package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SpellPower implements Attribute {

	@Override
	public String getName() {
		return "Spell Power";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.SPELL_DAMAGE;
	}

	@Override
	public double getPriorityAmount() {
		return 19;
	}

	@Override
	public double getDefaultValue() {
		return 1;
	}

	public static float getSpellDamage(Plugin plugin, @Nullable Player player, int damage) {
		return getSpellDamage(plugin, player, (float) damage);
	}

	public static float getSpellDamage(Plugin plugin, @Nullable Player player, float damage) {
		if (player == null) {
			return damage;
		}
		return getSpellDamage(plugin.mItemStatManager.getPlayerItemStats(player), damage);
	}

	public static float getSpellDamage(@Nullable ItemStatManager.PlayerItemStats playerItemStats, float damage) {
		if (playerItemStats == null) {
			return damage;
		}
		return (float) (damage * playerItemStats.getItemStats().get(AttributeType.SPELL_DAMAGE));
	}
}
