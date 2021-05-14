package com.playmonumenta.plugins.enchantments.abilities;

import java.util.EnumSet;

import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;



public class SpellPower extends BaseAbilityEnchantment {
	public SpellPower() {
		super("Spell Power", EnumSet.of(ItemSlot.MAINHAND));
	}
}