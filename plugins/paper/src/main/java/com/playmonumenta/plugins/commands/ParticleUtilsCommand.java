package com.playmonumenta.plugins.commands;

import com.mojang.brigadier.LiteralMessage;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ParticleUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ParticleUtilsCommand {

	public static String COMMAND = "particleutils";
	public static Particle.DustOptions DEFAULT_DUST_OPTIONS = new Particle.DustOptions(Color.RED, 1);
	public static BlockData DEFAULT_BLOCK_DATA = Material.REDSTONE_BLOCK.createBlockData();
	private static final Argument<String> particleArgument = new StringArgument("particle").includeSuggestions(
		ArgumentSuggestions.strings(Arrays.stream(Particle.values()).map(Enum::name).collect(Collectors.toList()))
	);
	private static final Argument<Double> distanceFalloffArgument = new DoubleArgument("distance falloff").includeSuggestions(
		ArgumentSuggestions.stringsWithTooltips(StringTooltip.ofMessage("0", new LiteralMessage("Spawn the particles fully, ignoring PEB.")))
	);
	private static final Argument<String> extraDataArgument = new TextArgument("extra data").includeSuggestions(
		ArgumentSuggestions.strings("null", "\"255,0,0,2.5\"", "REDSTONE_BLOCK")
	);


	public static void register() {
		// Line
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.particleutils")
			.withSubcommands(
				// Line
				new CommandAPICommand("line")
					.withPermission("monumenta.particleutils.line")
					.withArguments(
						new LocationArgument("start"),
						new LocationArgument("end"),
						new DoubleArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.executes((sender, args) -> {
						Location start = (Location) args[0];
						Location end = (Location) args[1];
						double countPerMeter = (double) args[2];
						Particle particle = Particle.valueOf((String) args[3]);
						double distanceFalloff = (double) args[4];
						String extraData = (String) args[5];

						CommandSender callee = getFinalCallee(sender, distanceFalloff);
						doLine(callee, start, end, countPerMeter, particle, distanceFalloff, parseExtraData(extraData, particle));
					}),
				// Rectangle
				new CommandAPICommand("rectangle")
					.withPermission("monumenta.particleutils.rectangle")
					.withArguments(
						new LocationArgument("start"),
						new DoubleArgument("dx"),
						new DoubleArgument("dz"),
						new DoubleArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.executes((sender, args) -> {
						Location start = (Location) args[0];
						double dx = (double) args[1];
						double dz = (double) args[2];
						double countPerMeter = (double) args[3];
						Particle particle = Particle.valueOf((String) args[4]);
						double distanceFalloff = (double) args[5];
						String extraData = (String) args[6];

						CommandSender callee = getFinalCallee(sender, distanceFalloff);
						doRectangle(callee, start, dx, dz, countPerMeter, particle, distanceFalloff, parseExtraData(extraData, particle));
					}),
				// Circle
				new CommandAPICommand("circle")
					.withPermission("monumenta.particleutils.circle")
					.withArguments(
						new BooleanArgument("ring mode"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.executes((sender, args) -> {
						boolean ringMode = (boolean) args[0];
						Location center = (Location) args[1];
						double radius = (double) args[2];
						double countPerMeter = (double) args[3];
						Particle particle = Particle.valueOf((String) args[4]);
						double distanceFalloff = (double) args[5];
						String extraData = (String) args[6];

						CommandSender callee = getFinalCallee(sender, distanceFalloff);
						doCircle(callee, ringMode, center, radius, countPerMeter, particle, distanceFalloff, parseExtraData(extraData, particle));
					}),
				// Circle Telegraph
				new CommandAPICommand("circletelegraph")
					.withPermission("monumenta.particleutils.circletelegraph")
					.withArguments(
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new IntegerArgument("count"),
						particleArgument,
						distanceFalloffArgument,
						new DoubleArgument("particle speed"),
						new IntegerArgument("pulses"),
						new IntegerArgument("telegraph duration"),
						new IntegerArgument("pulse start offset"),
						extraDataArgument
					)
					.executes((sender, args) -> {
						Location center = (Location) args[0];
						double radius = (double) args[1];
						int count = (int) args[2];
						Particle particle = Particle.valueOf((String) args[3]);
						double distanceFalloff = (double) args[4];
						double particleSpeed = (double) args[5];
						int pulses = (int) args[6];
						int telegraphDuration = (int) args[7];
						int pulseStartOffset = (int) args[8];
						String extraData = (String) args[9];

						CommandSender callee = getFinalCallee(sender, distanceFalloff);
						doCircleTelegraph(callee, center, radius, count, particle, distanceFalloff, particleSpeed, pulses, telegraphDuration, pulseStartOffset, parseExtraData(extraData, particle));
					}),
				// Number
				new CommandAPICommand("number")
					.withPermission("monumenta.particleutils.number")
						.withArguments(
							new IntegerArgument("number"),
							new LocationArgument("location"),
							new DoubleArgument("scale"),
							new DoubleArgument("spacing"),
							new EntitySelectorArgument.OnePlayer("facing player"),
							particleArgument,
							//distanceFalloffArgument,
							extraDataArgument
						)
						.executes((sender, args) -> {
							int number = (int) args[0];
							Location location = (Location) args[1];
							double scale = (double) args[2];
							double spacing = (double) args[3];
							Player player = (Player) args[4];
							Particle particle = Particle.valueOf((String) args[5]);
							//double distanceFalloff = (double) args[6];
							// String extraData = (String) args[7];
							String extraData = (String) args[6];

							//CommandSender callee = getFinalCallee(sender, distanceFalloff);
							ParticleUtils.drawSevenSegmentNumber(number, location, player, scale, spacing, particle, parseExtraData(extraData, particle));
						})
			)
			.register();
	}

	// Can return null if distanceFalloff is 0 (or lower), which will be interpreted as
	// wanting to spawn the full amount of particles, bypassing player/entity PEB settings.
	// Useful for puzzles!
	private static @Nullable CommandSender getFinalCallee(CommandSender sender, double distanceFalloff) {
		if (distanceFalloff <= 0) {
			return null;
		}

		return CommandUtils.getCallee(sender);
	}

	private static @Nullable Object parseExtraData(String extraData, Particle particle) {
		if (isRedstone(particle)) {
			return parseDustOptions(extraData);
		}

		if (isBlock(particle)) {
			return parseBlockData(extraData);
		}

		return null;
	}

	private static boolean isRedstone(Particle particle) {
		return particle.equals(Particle.REDSTONE);
	}

	private static boolean isBlock(Particle particle) {
		return particle.equals(Particle.BLOCK_CRACK) || particle.equals(Particle.BLOCK_DUST) || particle.equals(Particle.BLOCK_MARKER);
	}

	private static Particle.DustOptions parseDustOptions(String extraData) {
		try {
			String[] parts = extraData.split(",");
			return new Particle.DustOptions(
					Color.fromRGB(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
					Float.parseFloat(parts[3])
			);
		} catch (Exception e) {
			MMLog.warning("Error parsing redstone extra data: " + extraData);
			return DEFAULT_DUST_OPTIONS;
		}
	}

	private static BlockData parseBlockData(String extraData) {
		try {
			return Material.valueOf(extraData).createBlockData();
		} catch (Exception e) {
			MMLog.warning("Error parsing block extra data: " + extraData);
			return DEFAULT_BLOCK_DATA;
		}
	}

	private static void spawnAsActive(@Nullable CommandSender callee, AbstractPartialParticle<? extends AbstractPartialParticle<?>> particle) {
		if (callee instanceof Player player) {
			particle.spawnAsPlayerActive(player);
		} else if (callee instanceof Entity entity) {
			particle.spawnAsEntityActive(entity);
		} else {
			particle.spawnFull();
		}
	}

	private static void doLine(@Nullable CommandSender callee, Location start, Location end, double countPerMeter, Particle particle, double distanceFalloff, @Nullable Object data) {
		PPLine line = new PPLine(particle, start, end).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff);

		if (data != null) {
			line.data(data);
		}

		spawnAsActive(callee, line);
	}

	private static void doRectangle(@Nullable CommandSender callee, Location start, double dx, double dz, double countPerMeter, Particle particle, double distanceFalloff, @Nullable Object data) {
		PPLine[] sides = {
			new PPLine(particle, start, start.clone().add(dx, 0, 0)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(dx, 0, 0), start.clone().add(dx, 0, dz)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(dx, 0, dz), start.clone().add(0, 0, dz)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(0, 0, dz), start).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff)
		};

		if (data != null) {
			Arrays.stream(sides).forEach(side -> side.data(data));
		}

		Arrays.stream(sides).forEach(side -> spawnAsActive(callee, side));
	}

	private static void doCircle(@Nullable CommandSender callee, boolean ringMode, Location center, double radius, double countPerMeter, Particle particle, double distanceFalloff, @Nullable Object data) {
		PPCircle circle = new PPCircle(particle, center, radius).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff).ringMode(ringMode);

		if (data != null) {
			circle.data(data);
		}

		spawnAsActive(callee, circle);
	}

	private static void doCircleTelegraph(@Nullable CommandSender callee, Location center, double radius, int count, Particle particle, double distanceFalloff, double particleSpeed, int pulses, int telegraphDuration, int pulseStartOffset, @Nullable Object data) {
		// Input Validation
		int finalPulseStartOffset = Math.max(pulseStartOffset, 0);
		int finalTelegraphDuration = Math.max(telegraphDuration, 1);
		int totalPulseTime = finalTelegraphDuration - finalPulseStartOffset;
		int finalPulses = Math.max(pulses, 1);
		int finalPulseDelay = Math.max(totalPulseTime / finalPulses, 1);

		new BukkitRunnable() {

			int mPulses = 0;

			@Override
			public void run() {
				// Ring around it
				PPCircle circle = new PPCircle(particle, center, radius).count(count).distanceFalloff(distanceFalloff);
				// Pulse Ring
				PPParametric pulse = new PPParametric(particle, center, (t, builder) -> {
					Location particleLoc = center.clone().add(radius * FastUtils.cos(t * Math.PI * 2), 0, radius * FastUtils.sin(t * Math.PI * 2));
					Vector toCenter = center.toVector().subtract(particleLoc.toVector()).normalize();
					builder.location(particleLoc);
					builder.offset(toCenter.getX(), 0, toCenter.getZ());
				}).count(count).directionalMode(true).extra(particleSpeed).distanceFalloff(distanceFalloff);

				if (data != null) {
					circle.data(data);
					pulse.data(data);
				}

				spawnAsActive(callee, circle);
				spawnAsActive(callee, pulse);

				mPulses++;
				if (mPulses >= finalPulses) {
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), finalPulseStartOffset, finalPulseDelay);
	}
}
