package com.playmonumenta.plugins.depths.abilities.flamecaller;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.CustomDamageEvent;

import net.md_5.bungee.api.ChatColor;

public class PrimordialMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Primordial Mastery";
	public static final double[] SPELL_MOD = {1.1, 1.125, 1.15, 1.175, 1.2};

	public static String tree;

	public PrimordialMastery(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.FIRE_CORAL_FAN;
		mTree = DepthsTree.FLAMECALLER;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		double originalDamage = event.getDamage();
		double modifiedDamage = originalDamage * SPELL_MOD[mRarity - 1];
		event.setDamage(modifiedDamage);
	}

	@Override
	public String getDescription(int rarity) {
		return "All ability damage is multiplied by " + DepthsUtils.getRarityColor(rarity) + SPELL_MOD[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}

