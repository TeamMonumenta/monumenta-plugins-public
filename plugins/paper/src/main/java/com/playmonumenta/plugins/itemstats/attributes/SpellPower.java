package com.playmonumenta.plugins.itemstats.attributes;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;

public class SpellPower implements Attribute {

	@Override
	public @NotNull String getName() {
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

	public static float getSpellDamage(Plugin plugin, @Nullable Player player, int damage) {
		return getSpellDamage(plugin, player, (float) damage);
	}

	public static float getSpellDamage(Plugin plugin, @Nullable Player player, float damage) {
		if (player == null) {
			return damage;
		}

		return (float) (damage * plugin.mItemStatManager.getAttributeAmount(player, AttributeType.SPELL_DAMAGE));
	}
}
