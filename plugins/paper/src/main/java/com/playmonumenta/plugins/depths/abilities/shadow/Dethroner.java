package com.playmonumenta.plugins.depths.abilities.shadow;

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
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class Dethroner extends DepthsAbility {

	public static final String ABILITY_NAME = "Dethroner";
	public static final double[] ELITE_DAMAGE = {1.14, 1.175, 1.21, 1.245, 1.28};
	public static final double[] BOSS_DAMAGE = {1.1, 1.125, 1.15, 1.175, 1.2};

	public Dethroner(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.DRAGON_HEAD;
		mTree = DepthsTree.SHADOWS;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (EntityUtils.isBoss(event.getEntity())) {
			event.setDamage(event.getDamage() * BOSS_DAMAGE[mRarity - 1]);
		} else if (EntityUtils.isElite(event.getEntity())) {
			event.setDamage(event.getDamage() * ELITE_DAMAGE[mRarity - 1]);
		}

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (EntityUtils.isBoss(event.getEntity())) {
			event.setDamage(event.getDamage() * BOSS_DAMAGE[mRarity - 1]);
		} else if (EntityUtils.isElite(event.getEntity())) {
			event.setDamage(event.getDamage() * ELITE_DAMAGE[mRarity - 1]);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "All damage you deal to elites is multiplied by " + DepthsUtils.getRarityColor(rarity) + ELITE_DAMAGE[rarity - 1] + ChatColor.WHITE + ". All damage you deal to bosses is multiplied by " + DepthsUtils.getRarityColor(rarity) + BOSS_DAMAGE[rarity - 1] + ChatColor.WHITE + ".";
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

