package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DepthsFrostNova extends DepthsAbility {

	public static final String ABILITY_NAME = "Frost Nova";
	public static final int[] DAMAGE = {6, 8, 10, 12, 14, 18};
	public static final int SIZE = 6;
	public static final double[] SLOW_MULTIPLIER = {0.25, 0.3, 0.35, 0.4, 0.45, 0.55};
	public static final int DURATION_TICKS = 4 * 20;
	public static final int COOLDOWN_TICKS = 18 * 20;
	public static final int ICE_TICKS = 6 * 20;

	public static final DepthsAbilityInfo<DepthsFrostNova> INFO =
		new DepthsAbilityInfo<>(DepthsFrostNova.class, ABILITY_NAME, DepthsFrostNova::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.FROST_NOVA_DEPTHS)
			.cooldown(COOLDOWN_TICKS)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsFrostNova::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.ICE))
			.descriptions(DepthsFrostNova::getDescription, MAX_RARITY);

	public DepthsFrostNova(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SIZE, mPlayer)) {
			EntityUtils.applySlow(mPlugin, DURATION_TICKS, SLOW_MULTIPLIER[mRarity - 1], mob);
			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true);
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
			DepthsUtils.iceExposedBlock(b, ICE_TICKS, mPlayer);
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
					new PartialParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, mLoc, 8, 0, 0, 0, 0.65).spawnAsPlayerActive(mPlayer);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}

				if (mRadius >= SIZE + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 0.45f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1, 1.25f);
		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.5f, 1f);
	}

	private static String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to unleash a frost nova, dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage to all enemies in a " + SIZE + " block cube around you and afflicting them with " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(SLOW_MULTIPLIER[rarity - 1]) + "%" + ChatColor.WHITE + " slowness for " + DURATION_TICKS / 20 + " seconds. All mobs and players within range are extinguished if they are on fire. Nearby blocks are replaced with ice for " + ICE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN_TICKS / 20 + "s.";
	}


}

