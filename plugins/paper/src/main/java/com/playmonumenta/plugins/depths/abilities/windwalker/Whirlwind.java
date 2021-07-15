package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import net.md_5.bungee.api.ChatColor;


public class Whirlwind extends DepthsAbility {

	public static final String ABILITY_NAME = "Whirlwind";
	private static final int RADIUS = 3;
	private static final double[] KNOCKBACK_SPEED = {0.8, 1.0, 1.2, 1.4, 1.6};

	public static String tree;

	public Whirlwind(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_PICKAXE;
		mTree = DepthsTree.WINDWALKER;
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		//If we break a spawner with a pickaxe
		if (InventoryUtils.isPickaxeItem(event.getPlayer().getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			World world = event.getPlayer().getWorld();
			Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.2f, 0.25f);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.2f, 0.35f);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.2f, 0.45f);
			world.spawnParticle(Particle.CLOUD, loc, 30, 1, 1, 1, 0.8);
			for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS)) {
				e.setVelocity(e.getVelocity().add(e.getLocation().toVector().subtract(loc.subtract(0, 0.5, 0).toVector()).normalize().multiply(KNOCKBACK_SPEED[mRarity - 1]).add(new Vector(0, 0.3, 0))));
				world.spawnParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35);
			}
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Breaking a spawner knocks back all mobs within " + RADIUS + " blocks with a speed of " + DepthsUtils.getRarityColor(rarity) + KNOCKBACK_SPEED[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SPAWNER;
	}
}

