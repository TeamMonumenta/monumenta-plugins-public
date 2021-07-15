package com.playmonumenta.plugins.depths.abilities.frostborn;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsFrostNova extends DepthsAbility {

	public static final String ABILITY_NAME = "Frost Nova";
	public static final int[] DAMAGE = {6, 8, 10, 12, 14};
	public static final int SIZE = 6;
	public static final double[] SLOW_MULTIPLIER = {0.25, 0.3, 0.35, 0.4, 0.45};
	public static final int DURATION_TICKS = 4 * 20;
	public static final int COOLDOWN_TICKS = 18 * 20;
	public static final int ICE_TICKS = 8 * 20;

	public DepthsFrostNova(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.ICE;
		mTree = DepthsTree.FROSTBORN;
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mLinkedSpell = ClassAbility.FROST_NOVA;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SIZE, mPlayer)) {
			EntityUtils.applySlow(mPlugin, DURATION_TICKS, SLOW_MULTIPLIER[mRarity - 1], mob);
			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
			EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, true, true, true, false);
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), SIZE, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		//Set ice in world
		ArrayList<Block> blocksToIce = new ArrayList<>();
		Block block = mPlayer.getWorld().getBlockAt(mPlayer.getLocation()).getRelative(BlockFace.DOWN);
		blocksToIce.add(block);
		blocksToIce.add(block.getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST).getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST).getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST).getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST).getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));

		for (Block b : blocksToIce) {
			//Check above block first and see if it is exposed to air
			if (b.getRelative(BlockFace.UP).isSolid() && !b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isSolid()) {
				DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), ICE_TICKS);
			} else if (b.isSolid()) {
				DepthsUtils.spawnIceTerrain(b.getLocation(), ICE_TICKS);

			} else if (b.getRelative(BlockFace.DOWN).isSolid()) {
				DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.DOWN).getLocation(), ICE_TICKS);

			}
		}

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mPlayer.getLocation();
			@Override
			public void run() {
				mRadius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0, 0.1);
					world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 8, 0, 0, 0, 0.65);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}

				if (mRadius >= SIZE + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.45f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
		world.spawnParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35);
		world.spawnParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1f);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to unleash a frost nova, dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage to all enemies in a " + SIZE + " block cube around you and afflicting them with " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(SLOW_MULTIPLIER[rarity - 1]) + "%" + ChatColor.WHITE + " slowness for " + DURATION_TICKS / 20 + " seconds. All mobs and players within range are extinguished if they are on fire. Nearby blocks are replaced with ice for " + ICE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN_TICKS / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}
}

