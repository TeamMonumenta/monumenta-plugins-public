package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

import net.md_5.bungee.api.ChatColor;

public class Aeromancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Aeromancy";
	public static final double[] DAMAGE = {1.16, 1.2, 1.24, 1.28, 1.32};

	public Aeromancy(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.FEATHER;
		mTree = DepthsTree.WINDWALKER;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (!mPlayer.isOnGround()) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
		}

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (!mPlayer.isOnGround()) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "While you are airborne, all damage you deal is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

