package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellEarthshake extends SpellBaseAoE {

	public static final String FALLING_BLOCK_COMMAND1 = "{BlockState:{Name:\"";
	public static final String FALLING_BLOCK_COMMAND2 = "\"},Time:1,Motion:[";

	private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
            Material.AIR,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.BEDROCK,
			Material.BARRIER,
            Material.SPAWNER,
            Material.CHEST
        );
	private static Location targetLocation;
	private static boolean targeted = false;
	private static int particleCounter1 = 0;
	private static int particleCounter2 = 0;

	public SpellEarthshake(Plugin plugin, LivingEntity launcher, int radius, int time) {
		super(plugin, launcher, radius * 2, time, 0, false, Sound.BLOCK_ENDER_CHEST_OPEN,
			(Location loc) -> {
				List<Player> players = PlayerUtils.playersInRange(launcher.getLocation(), 12.0);
				if (!players.isEmpty() && !targeted) {

					// Single target chooses a random player within range
					Collections.shuffle(players);
					for (Player player : players) {
						if (LocationUtils.hasLineOfSight(launcher, player)) {
							targetLocation = player.getLocation().clone();
							targeted = true;
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.75f);
						} else {
							targetLocation = null;
						}
					}
				}
				if (targetLocation != null) {
					loc = targetLocation;
					World world = loc.getWorld();
					if (particleCounter1 % 2 == 0) {
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, ((double) radius * 2) / 2, ((double) radius * 2) / 2, ((double) radius * 2) / 2, 0.05);
					}

					world.spawnParticle(Particle.BLOCK_CRACK, loc, 2, radius / 2, 0.1, radius / 2, Bukkit.createBlockData(Material.STONE));

					if (particleCounter1 % 20 == 0 && particleCounter1 > 0) {
						world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 0.5f);
						world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
						for (int i = 0; i < 360; i += 18) {
							world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(Math.cos(Math.toRadians(i)) * radius, 0.2, Math.sin(Math.toRadians(i)) * radius), 1, 0.1, 0.1, 0.1, 0);
						}
						world.spawnParticle(Particle.BLOCK_CRACK, loc, 80, radius / 2, 0.1, radius / 2, Bukkit.createBlockData(Material.DIRT));
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 8, radius / 2, 0.1, radius / 2, 0);
					}
					particleCounter1++;
				}
			},
			(Location loc) -> {
				if (targetLocation != null) {
					loc = targetLocation;
					World world = loc.getWorld();

					if (particleCounter2 % 10 == 0) {
						world.spawnParticle(Particle.LAVA, loc, 1, 0.25, 0.25, 0.25, 0.1);
						launcher.getWorld().spawnParticle(Particle.DRIP_LAVA, launcher.getLocation().clone().add(0, launcher.getHeight() / 2, 0), 1, 0.25, 0.45, 0.25, 1);
					}
					particleCounter2++;
				}
			},
			(Location loc) -> {
				if (targetLocation != null) {
					loc = targetLocation;
					World world = loc.getWorld();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.35f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.5f, 0.5f);

					world.spawnParticle(Particle.CLOUD, loc, 150, 0, 0, 0, 0.5);
					world.spawnParticle(Particle.LAVA, loc, 35, radius / 2, 0.1, radius / 2, 0);
					world.spawnParticle(Particle.BLOCK_CRACK, loc, 200, radius / 2, 0.1, radius / 2, Bukkit.createBlockData(Material.DIRT));
					world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 35, radius / 2, 0.1, radius / 2, 0.1);

					for (int i = 0; i < 100; i++) {
						world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(-3 + FastUtils.RANDOM.nextDouble() * 6, 0.1, -3 + FastUtils.RANDOM.nextDouble() * 6), 0, 0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4);
					}
				}
			},
			(Location loc) -> {
			},
			(Location loc) -> {
				if (targetLocation != null) {
					loc = targetLocation;

					World world = loc.getWorld();
					ArrayList<Block> blocks = new ArrayList<Block>();

					//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
					for (int x = radius * -1; x <= radius; x++) {
						for (int y = radius * -1; y <= radius; y++) {
							Block selected = null;
							Block test = world.getBlockAt(loc.clone().add(x, -2, y));
							if (test.getType() != Material.AIR) {
								selected = world.getBlockAt(loc.clone().add(x, -2, y));
							}
							test = world.getBlockAt(loc.clone().add(x, -1, y));
							if (test.getType() != Material.AIR) {
								selected = world.getBlockAt(loc.clone().add(x, -1, y));
							} else {
								blocks.add(selected);
								continue;
							}
							test = world.getBlockAt(loc.clone().add(x, 0, y));
							if (test.getType() != Material.AIR) {
								selected = world.getBlockAt(loc.clone().add(x, 0, y));
							} else {
								blocks.add(selected);
								continue;
							}
							test = world.getBlockAt(loc.clone().add(x, 1, y));
							if (test.getType() != Material.AIR) {
								selected = world.getBlockAt(loc.clone().add(x, 1, y));
							} else {
								blocks.add(selected);
								continue;
							}
							test = world.getBlockAt(loc.clone().add(x, 2, y));
							if (test.getBlockData().getMaterial() != Material.AIR) {
								selected = world.getBlockAt(loc.clone().add(x, 2, y));
							} else {
								blocks.add(selected);
								continue;
							}
						}
					}

					//Make the blocks go flying
					for (Block b: blocks) {
						if (b == null) {
							continue;
						}

						Material material = b.getType();
						if (!mIgnoredMats.contains(material) && FastUtils.RANDOM.nextInt(4) > 1) {
							double x = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
							double z = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;

							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:falling_block " + b.getLocation().getX() + " " + b.getLocation().getY() + " " + b.getLocation().getZ() + " " + FALLING_BLOCK_COMMAND1 + b.getBlockData().getAsString() + FALLING_BLOCK_COMMAND2 + x + ",1.0," + z + "]}");
							world.getBlockAt(b.getLocation()).setType(Material.AIR);
						}
					}

					//Knock up player
					for (Player p : PlayerUtils.playersInRange(loc, radius * 2)) {
						world.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
						if (p.getLocation().distance(loc) <= radius) {
							p.setVelocity(p.getVelocity().add(new Vector(0.0, 1.5, 0.0)));
							BossUtils.bossDamage(launcher, p, 35.0);
						} else {
							p.setVelocity(p.getVelocity().add(new Vector(0.0, 1.0, 0.0)));
							BossUtils.bossDamage(launcher, p, 35.0);
						}
					}
				}

				targeted = false;
				particleCounter1 = 0;
				particleCounter2 = 0;
			}
		);
	}
}
