package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
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
	public static final double[] PLAYER_DAMAGE = {1.12, 1.15, 1.18, 1.21, 1.24};
	public static final double[] MOB_DAMAGE = {1.056, 1.07, 1.084, 1.098, 1.112};

	public Aeromancy(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.FEATHER;
		mTree = DepthsTree.WINDWALKER;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * damageMultiplier(event.getEntity()));

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * damageMultiplier(damagee));

		return true;
	}

	private double damageMultiplier(Entity damagee) {
		double multiplier = 1;
		if (!mPlayer.isOnGround()) {
			multiplier *= PLAYER_DAMAGE[mRarity - 1];
		}
		if (!damagee.isOnGround()) {
			multiplier *= MOB_DAMAGE[mRarity - 1];
		}
		return multiplier;
	}

	@Override
	public String getDescription(int rarity) {
		return "All damage you deal while airborne is multiplied by " + DepthsUtils.getRarityColor(rarity) + PLAYER_DAMAGE[rarity - 1] + ChatColor.WHITE + ". Addionally, all damage you deal against airborne enemies is multiplied by " + DepthsUtils.getRarityColor(rarity) + MOB_DAMAGE[rarity - 1] + ChatColor.WHITE + ".";
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

