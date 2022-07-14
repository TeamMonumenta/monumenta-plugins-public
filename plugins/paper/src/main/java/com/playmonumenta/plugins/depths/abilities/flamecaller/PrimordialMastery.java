package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PrimordialMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Primordial Mastery";
	public static final double[] SPELL_MOD = {1.08, 1.095, 1.11, 1.125, 1.14, 1.18};

	public PrimordialMastery(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.FIRE_CORAL_FAN;
		mTree = DepthsTree.FLAMECALLER;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() != null) {
			event.setDamage(event.getDamage() * SPELL_MOD[mRarity - 1]);
		}
		return false; // only changes event damage
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

