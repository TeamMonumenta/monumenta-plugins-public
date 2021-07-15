package com.playmonumenta.plugins.depths.abilities.steelsage;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.effects.Bleed;

import net.md_5.bungee.api.ChatColor;

public class BarbedProjectiles extends DepthsAbility {

	public static final String ABILITY_NAME = "Barbed Projectiles";
	public static final int[] BLEED_LEVEL = {2, 2, 3, 3, 4};
	public static final int[] DURATION = {20 * 4, 20 * 6, 20 * 4, 20 * 6, 20 * 4};

	public BarbedProjectiles(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.WEEPING_VINES;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		mPlugin.mEffectManager.addEffect(le, ABILITY_NAME, new Bleed(DURATION[mRarity - 1], BLEED_LEVEL[mRarity - 1], mPlugin));

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Your projectiles inflict Bleed " + DepthsUtils.getRarityColor(rarity) + BLEED_LEVEL[rarity - 1] + ChatColor.WHITE + " for " + DepthsUtils.getRarityColor(rarity) + DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

