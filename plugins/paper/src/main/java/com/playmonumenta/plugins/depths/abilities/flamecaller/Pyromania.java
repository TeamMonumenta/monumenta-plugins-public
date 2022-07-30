package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Pyromania extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyromania";
	public static final double[] DAMAGE = {0.02, 0.025, 0.03, 0.035, 0.04, 0.05};
	public static final int RADIUS = 6;
	public static final int TWISTED_RADIUS = 8;

	public Pyromania(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.CAMPFIRE;
		mTree = DepthsTree.FLAMECALLER;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {

		int radius = mRarity < 6 ? RADIUS : TWISTED_RADIUS;
		int fireCount = 0;
		for (LivingEntity e : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
			if (e.getFireTicks() > 0) {
				fireCount++;
			}
		}
		if (fireCount > 0) {
			event.setDamage(event.getDamage() * (1 + (DAMAGE[mRarity - 1] * fireCount)));
		}
		return false;
	}

	@Override
	public String getDescription(int rarity) {
		if (rarity == 6) {
			return "For every mob on fire within " + DepthsUtils.getRarityColor(rarity) + TWISTED_RADIUS + ChatColor.WHITE + " blocks of you, gain " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(DAMAGE[rarity - 1]) + "%" + ChatColor.WHITE + " increased damage.";
		} else {
			return "For every mob on fire within " + RADIUS + " blocks of you, gain " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(DAMAGE[rarity - 1]) + "%" + ChatColor.WHITE + " increased damage.";
		}
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}

