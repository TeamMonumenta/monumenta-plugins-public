package com.playmonumenta.plugins.depths.abilities.steelsage;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;

import net.md_5.bungee.api.ChatColor;

public class ProjectileMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Projectile Mastery";
	public static final double[] SPELL_MOD = {1.1, 1.125, 1.15, 1.175, 1.2, 1.25};

	public ProjectileMastery(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.BOW;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * getDamageMultiplier(mPlayer));

		return true;
	}

	public static double getDamageMultiplier(Player player) {
		ProjectileMastery pm = AbilityManager.getManager().getPlayerAbility(player, ProjectileMastery.class);
		return pm == null ? 1 : SPELL_MOD[pm.mRarity - 1];
	}

	@Override
	public String getDescription(int rarity) {
		return "Your projectile damage is multiplied by " + DepthsUtils.getRarityColor(rarity) + SPELL_MOD[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

