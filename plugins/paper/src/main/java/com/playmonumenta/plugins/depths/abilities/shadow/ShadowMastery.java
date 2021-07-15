package com.playmonumenta.plugins.depths.abilities.shadow;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

import net.md_5.bungee.api.ChatColor;

public class ShadowMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Deadly Strike";
	public static final double[] DAMAGE = {1.10, 1.125, 1.15, 1.175, 1.2};

	public ShadowMastery(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.BLACK_CONCRETE_POWDER;
		mTree = DepthsTree.SHADOWS;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Your melee damage is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

