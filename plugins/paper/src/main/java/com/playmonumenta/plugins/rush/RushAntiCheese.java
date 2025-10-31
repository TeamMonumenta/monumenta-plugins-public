package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class RushAntiCheese {
	private static final int TICK_RATE = 5;
	private static final int RADIUS = 1;
	private static final int MAX_INACTIVITY = (Constants.TICKS_PER_SECOND * 6) / TICK_RATE;
	private static final int DAMAGE_TICK = Constants.TICKS_PER_SECOND / TICK_RATE;
	private static final Material BREAKABLE_MATERIAL = Material.OBSIDIAN;
	private static final Material UNBREAKABLE_MATERIAL = Material.COMMAND_BLOCK;
	private static final Component CHEESE_MESSAGE = Component.text("The ground under you corrodes...", NamedTextColor.RED).decorate(TextDecoration.ITALIC);


	HashSet<Player> mGracePeriod = new HashSet<>();
	HashMap<Player, Location> mPlayerTracker = new HashMap<>();
	HashMap<Player, Integer> mPlayerInactivity = new HashMap<>();
	BukkitTask mAntiCheese;

	// Players should reference mPlayers from RushArena. Should stop if mPlayers is empty.

	public RushAntiCheese(Set<Player> players, double centerY) {
		players.forEach(p -> mPlayerTracker.put(p, p.getLocation()));
		players.forEach(p -> mPlayerInactivity.put(p, 0));
		mAntiCheese = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (players.isEmpty()) {
					disable();
					return;
				}

				for (Player p : players) {
					if (mPlayerTracker.get(p) == null || mPlayerInactivity.get(p) == null) {
						continue;
					}
					Location pLoc = p.getLocation();
					boolean isHighLocation = pLoc.getY() - centerY >= 10;
					if (mPlayerTracker.get(p).distanceSquared(pLoc) <= RADIUS * RADIUS * (isHighLocation ? 1.5 : 1)
						&& p.getGameMode().equals(GameMode.SURVIVAL)) {
						int count = mPlayerInactivity.get(p);
						if (count < MAX_INACTIVITY) {
							count += isHighLocation ? 2 : 1;
						} else {
							count = 0;
							doAntiCheese(p);
							mGracePeriod.add(p);
						}
						mPlayerInactivity.put(p, count);
					} else {
						mPlayerTracker.put(p, p.getLocation());
						mPlayerInactivity.put(p, 0);
					}
				}

				if (mTicks % DAMAGE_TICK == 0) {
					for (Player p : players) {
						if (!mGracePeriod.remove(p)) {
							runDamageFloor(p);
						}
					}
					mTicks = 0;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), Constants.TICKS_PER_SECOND * 2, TICK_RATE);
	}

	private static void runDamageFloor(Player p) {
		if (p == null || p.isDead() || !p.isValid() || !p.getGameMode().equals(GameMode.SURVIVAL)) {
			return;
		}

		Location loc = p.getLocation();
		World world = p.getWorld();
		Material block = loc.getBlock().getRelative(BlockFace.DOWN).getType();
		if (block == BREAKABLE_MATERIAL || block == UNBREAKABLE_MATERIAL) {
			// Damage player
			Vector vel = p.getVelocity();
			DamageUtils.damage(null, p, DamageEvent.DamageType.AILMENT, 0.1, null, true, false);
			BossUtils.bossDamagePercent(null, p, 0.2f);
			p.setVelocity(vel);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 0.3f, 1.5f);
		}


	}

	private static void doAntiCheese(Player player) {
		Location playerLoc = player.getLocation();
		World world = player.getWorld();

		// Replace block with obsidian, replace unbreakable with another unbreakable
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				Location loc = playerLoc.clone().add(x, -1, z);
				Block block = loc.getBlock();

				if (block.getType() == Material.BEDROCK) {
					block.setType(UNBREAKABLE_MATERIAL);
				}
				if (!(block.getType() == UNBREAKABLE_MATERIAL)) {
					block.setType(BREAKABLE_MATERIAL);
				}
			}
		}

		world.playSound(playerLoc, Sound.ENTITY_GENERIC_DRINK, 1f, 0.5f);
		world.playSound(playerLoc, Sound.ENTITY_WITCH_DRINK, 1f, 0.5f);
		world.playSound(playerLoc, Sound.ENTITY_SNIFFER_HURT, 1f, 0.5f);
		player.sendMessage(CHEESE_MESSAGE);
	}

	public void disable() {
		mPlayerInactivity.clear();
		mPlayerTracker.clear();
		mGracePeriod.clear();
		mAntiCheese.cancel();
	}

}
