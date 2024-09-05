package com.playmonumenta.plugins.spawners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SpawnerVisualisation implements Listener {

	private static final String PLACED_SPAWNER_PARTICLE_DURATION_SCOREBOARD = "ShowSpawnerRangeDuration";

	// particles used to show different spawners' ranges
	private static final Particle[] PARTICLES = {
			Particle.END_ROD,
			Particle.FLAME,
			Particle.SOUL_FIRE_FLAME,
			Particle.DRAGON_BREATH,
			};

	private final Map<Player, BukkitRunnable> mRunnables = new HashMap<>();

	public void register() {

		Bukkit.getPluginManager().registerEvents(this, Plugin.getInstance());

		new CommandAPICommand("spawnervisualisation")
				.withPermission("monumenta.command.spawnervisualisation")
				.withSubcommand(new CommandAPICommand("show")
						.withArguments(new MultiLiteralArgument("action", "activation_ranges", "spawn_ranges"))
						.executes((sender, args) -> {
							Player player = CommandUtils.getPlayerFromSender(sender);
							String action = Objects.requireNonNull(args.getUnchecked("action"));
							player.sendMessage(Component.text("Spawner range visualisation enabled for " + action.replace('_', ' ')
									+ ". Note that you can change the number of particles by adjusting the 'own emoji' category in the PEB."));
							startRunnable(player, null, action.equals("activation_ranges"), -1);
						})
				)
				.withSubcommand(new CommandAPICommand("placed_spawner_ranges")
						.withArguments(new IntegerArgument("duration_in_seconds"))
						.executes((sender, args) -> {
							Player player = CommandUtils.getPlayerFromSender(sender);
							int duration = Objects.requireNonNull(args.getUnchecked("duration_in_seconds"));
							ScoreboardUtils.setScoreboardValue(player, PLACED_SPAWNER_PARTICLE_DURATION_SCOREBOARD, duration);
							if (duration == 0) {
								player.sendMessage(Component.text("Will not show ranges for placed spawners."));
							} else {
								player.sendMessage(Component.text("Showing ranges for placed spawners " + (duration > 0 ? "for " + duration + " seconds" : "until unloaded or a new spawner is placed")
										+ ". Note that you can change the number of particles by adjusting the 'own emoji' category in the PEB."));
							}
						})
				)
				.withSubcommand(new CommandAPICommand("disable")
						.executes((sender, args) -> {
							Player player = CommandUtils.getPlayerFromSender(sender);
							BukkitRunnable runnable = mRunnables.remove(player);
							if (runnable != null) {
								runnable.cancel();
								player.sendMessage(Component.text("Active spawner range visualisation disabled"));
							} else {
								player.sendMessage(Component.text("There's no active spawner range visualisation to disable"));
							}
						})
				)
				.register();

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (event.getBlockPlaced().getType() == Material.SPAWNER
				&& event.getPlayer().getGameMode() == GameMode.CREATIVE
				&& !Plugin.IS_PLAY_SERVER) {
			Player player = event.getPlayer();
			int duration = ScoreboardUtils.getScoreboardValue(player, PLACED_SPAWNER_PARTICLE_DURATION_SCOREBOARD).orElse(60);
			if (duration != 0) {
				player.sendMessage(Component.text("Showing activation range (white) and spawn range (flames) for the placed spawner " + (duration > 0 ? "for " + duration + " seconds " : ""))
						.append(Component.text("[change]", NamedTextColor.GRAY).clickEvent(ClickEvent.suggestCommand("/spawnervisualisation placed_spawner_ranges "))));
				startRunnable(player, event.getBlockPlaced().getLocation(), true, duration);
			}
		}
	}

	private void startRunnable(Player player, @Nullable Location spawnerLocation, boolean showActivationRange, int duration) {
		BukkitRunnable oldRunnable = mRunnables.remove(player);
		if (oldRunnable != null) {
			oldRunnable.cancel();
		}
		int endTick = duration < 0 ? Integer.MAX_VALUE : Bukkit.getCurrentTick() + 20 * duration;
		BukkitRunnable runnable = new BukkitRunnable() {
			private final Map<Location, Particle> mUsedParticles = new HashMap<>();

			@Override
			public void run() {
				if (!player.isOnline() || endTick < Bukkit.getCurrentTick()) {
					cancel();
					return;
				}

				if (spawnerLocation != null) {
					if (!spawnerLocation.getChunk().isLoaded()
							|| spawnerLocation.getWorld().getBlockAt(spawnerLocation).getType() != Material.SPAWNER
							|| !(spawnerLocation.getWorld().getBlockState(spawnerLocation) instanceof CreatureSpawner spawner)) {
						cancel();
						return;
					}
					drawSphere(player, spawner.getLocation().add(0.5, 0.5, 0.5), spawner.getRequiredPlayerRange(), Particle.END_ROD);
					drawBox(player, spawner.getLocation().add(0.5, 0.5, 0.5), spawner.getSpawnRange(), 1.5, Particle.FLAME);
					drawBox(player, spawner.getLocation().add(0.5, 0.5, 0.5), 0.55, 0.55, Particle.FLAME);
				} else {
					int pcx = player.getLocation().getChunk().getX();
					int pcz = player.getLocation().getChunk().getZ();
					List<CreatureSpawner> spawners = new ArrayList<>();
					for (Chunk chunk : player.getWorld().getLoadedChunks()) {
						if (Math.abs(chunk.getX() - pcx) <= 3 && Math.abs(chunk.getZ() - pcz) <= 3) {
							for (BlockState bs : chunk.getTileEntities(b -> b.getLocation().distanceSquared(player.getLocation()) <= 48 * 48 && b.getType() == Material.SPAWNER, false)) {
								if (bs instanceof CreatureSpawner spawner) {
									spawners.add(spawner);
								}
							}
						}
					}
					List<CreatureSpawner> limitedSpawners = spawners.stream()
							.sorted(Comparator.comparing(spawner -> spawner.getLocation().distance(player.getLocation()) - spawner.getRequiredPlayerRange()))
							.limit(PARTICLES.length)
							.toList();
					mUsedParticles.keySet().removeIf(loc -> limitedSpawners.stream().noneMatch(s -> s.getLocation().equals(loc)));
					List<Particle> availableParticles = new ArrayList<>(Arrays.asList(PARTICLES));
					availableParticles.removeAll(mUsedParticles.values());
					for (CreatureSpawner spawner : limitedSpawners) {
						Particle particle = mUsedParticles.computeIfAbsent(spawner.getLocation(), loc -> availableParticles.remove(0));
						if (showActivationRange) {
							drawSphere(player, spawner.getLocation().add(0.5, 0.5, 0.5), spawner.getRequiredPlayerRange(), particle);
						} else {
							drawBox(player, spawner.getLocation().add(0.5, 0.5, 0.5), spawner.getSpawnRange(), 1.5, particle);
						}
						// show colours on spawner
						drawBox(player, spawner.getLocation().add(0.5, 0.5, 0.5), 0.55, 0.55, particle);
					}
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				mRunnables.remove(player);
				super.cancel();
			}
		};
		runnable.runTaskTimer(Plugin.getInstance(), 0, 10);
		mRunnables.put(player, runnable);
	}

	private static void drawBox(Player player, Location center, double radiusHorz, double radiusVert, Particle particle) {
		Vector radiusVector = new Vector(radiusHorz, radiusVert, radiusHorz);
		Vector[] axes = {new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, 0, 1)};
		for (int a = 0; a < 3; a++) {
			Vector dir = axes[a];
			Vector dirU = axes[(a + 1) % 3];
			Vector dirV = axes[(a + 2) % 3];
			for (int i = 0; i < 4; i++) {
				int u = 1 - 2 * (i % 2);
				int v = 1 - 2 * (i / 2);
				Vector offset = dir.clone().add(dirU.clone().multiply(u)).add(dirV.clone().multiply(v)).multiply(radiusVector);
				Location corner1 = center.clone().add(offset);
				Location corner2 = center.clone().add(offset.clone().subtract(dir.clone().multiply(radiusVector).multiply(2)));
				new PPLine(particle, corner1, corner2)
						.countPerMeter(3)
						.distanceFalloff(200)
						.spawnForPlayer(ParticleCategory.OWN_EMOJI, player);
			}
		}
	}

	private static void drawSphere(Player player, Location center, double radius, Particle particle) {
		Vector[] axes = {new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, 0, 1)};
		for (int a = 0; a < 3; a++) {
			new PPCircle(particle, center, radius)
					.axes(axes[a], axes[(a + 1) % 3])
					.countPerMeter(2)
					.distanceFalloff(200)
					.spawnForPlayer(ParticleCategory.OWN_EMOJI, player);
			new PPParametric(particle, center,
					(t, builder) -> {
						builder.location(center.clone().add(new Vector(FastUtils.RANDOM.nextGaussian(), FastUtils.RANDOM.nextGaussian(), FastUtils.RANDOM.nextGaussian()).normalize().multiply(radius)));
					})
					.count(Math.min(500, (int) (radius * radius)))
					.distanceFalloff(200)
					.spawnForPlayer(ParticleCategory.OWN_EMOJI, player);
		}
	}

}
