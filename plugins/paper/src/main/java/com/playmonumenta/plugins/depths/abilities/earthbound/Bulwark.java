package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.utils.AbilityUtils;

import net.md_5.bungee.api.ChatColor;

public class Bulwark extends DepthsAbility {

	public static final String ABILITY_NAME = "Bulwark";
	public static final int[] COOLDOWN = {16, 14, 12, 10, 8}; //seconds

	public Bulwark(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.NETHERITE_HELMET;
		mTree = DepthsTree.EARTHBOUND;

		mInfo.mLinkedSpell = ClassAbility.BULWARK;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		mInfo.mCooldown = COOLDOWN[mRarity - 1] * 20;
		if (event.getCause() == DamageCause.ENTITY_ATTACK && !AbilityUtils.isBlocked(event)) {
			trigger(event);
			putOnCooldown();
		}

		return true;
	}

	private void trigger(EntityDamageByEntityEvent event) {
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		Location particleLoc = loc.add(0, mPlayer.getHeight() / 2, 0);
		world.spawnParticle(Particle.CLOUD, particleLoc, 10, 0.35, 0.35, 0.35, 0.125);
		world.spawnParticle(Particle.BLOCK_DUST, particleLoc, 20, 0.5, 0.5, 0.5, 0.125, Material.STRIPPED_DARK_OAK_WOOD.createBlockData());
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1, 1.2f);
		event.setDamage(0);
		event.setCancelled(true);
	}

	@Override
	public boolean runCheck() {
		return (!isOnCooldown());
	}

	@Override
	public String getDescription(int rarity) {
		return "You block the next melee attack that would have hit you, nullifying the damage. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] + ChatColor.WHITE + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}
}
