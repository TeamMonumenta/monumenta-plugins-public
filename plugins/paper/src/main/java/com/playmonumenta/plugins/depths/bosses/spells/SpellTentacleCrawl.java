package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellTentacleCrawl extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;
	public int mCooldownTicks;
	public Nucleus mBossInstance;

	private Map<Location, Material> mOldBlocks = new HashMap<>();
	private Map<Location, BlockData> mOldData = new HashMap<>();

	private int DURATION = 5 * 20;
	private int DAMAGE = 50;
	private int ROOT_DURATION = (int) 1.5 * 20;

	public SpellTentacleCrawl(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
		mBossInstance = bossInstance;
	}

	@Override
	public boolean canRun() {
		return mBossInstance.mIsHidden;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, 80, true);
		players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);

		for (Player player : players) {
			cast(player);
		}
	}

	private void cast(Player target) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= DURATION) {
					damage();
					this.cancel();
				} else if (mTicks < DURATION - 10 && target != null) {
					placeFans(target.getLocation());
				}

				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void placeFans(Location loc) {
		World world = loc.getWorld();
		for (int offsetX = -2; offsetX <= 2 + 2; offsetX++) {
			int range = 2 - Math.abs(offsetX);
			for (int offsetZ = -range; offsetZ <= range; offsetZ++) {
				Location bLoc = loc.clone().add(offsetX, 0, offsetZ);
				Block oldBlock = bLoc.getBlock();
				Material type = oldBlock.getType();
				if (type != Material.FIRE_CORAL_FAN) {
					mOldBlocks.put(bLoc, oldBlock.getType());
					mOldData.put(bLoc, oldBlock.getBlockData());
					oldBlock.setType(Material.FIRE_CORAL_FAN);
					BlockData bd = oldBlock.getBlockData();
					if (bd instanceof Waterlogged) {
						((Waterlogged) bd).setWaterlogged(false);
					}
					oldBlock.setBlockData(bd);
					world.spawnParticle(Particle.SPELL_INSTANT, bLoc, 1, 0.45, 6, 0.45, 0, null, true);
				}
			}
		}
	}

	private void damage() {
		if (!mOldBlocks.isEmpty()) {
			World world = mStartLoc.getWorld();
			Iterator<Map.Entry<Location, Material>> blocks = mOldBlocks.entrySet().iterator();
			List<Player> hitPlayers = new ArrayList<Player>();
			while (blocks.hasNext()) {
				Map.Entry<Location, Material> e = blocks.next();
				for (Player player : PlayerUtils.playersInBox(e.getKey(), 1, 1)) {
					if (!hitPlayers.contains(player)) {
						BossUtils.bossDamage(mBoss, player, DAMAGE, e.getKey());
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ROOT_DURATION, 2));
						player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ROOT_DURATION, -4));
						hitPlayers.add(player);
					}
				}

				if (e.getKey().getBlock().getType() == Material.FIRE_CORAL_FAN) {
					e.getKey().getBlock().setType(e.getValue());
					if (mOldData.containsKey(e.getKey())) {
						e.getKey().getBlock().setBlockData(mOldData.get(e.getKey()));
						BlockData bd = e.getKey().getBlock().getBlockData();
						if (bd instanceof Waterlogged) {
							((Waterlogged) bd).setWaterlogged(false);
						}
						e.getKey().getBlock().setBlockData(bd);
					}
					world.spawnParticle(Particle.FLAME, e.getKey().add(0.5, 0.5, 0.5), 10, 1, 0.5, 1);
					world.playSound(e.getKey(), Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.0f);
				}
				blocks.remove();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
