package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class SpellDamage extends BaseAbilityEnchantment {

	public SpellDamage() {
		super("Spell Power", EnumSet.of(ItemSlot.MAINHAND));
	}
}


