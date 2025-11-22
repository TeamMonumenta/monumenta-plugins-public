package com.playmonumenta.plugins.commands;

import com.mojang.brigadier.LiteralMessage;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import java.util.Collection;
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
	public static Particle.DustTransition DEFAULT_DUST_COLOR_TRANSITION = new Particle.DustTransition(Color.BLACK, Color.BLACK, 1f);
	private static final Argument<String> particleArgument = new StringArgument("particle").includeSuggestions(
		ArgumentSuggestions.strings(Arrays.stream(Particle.values()).map(Enum::name).collect(Collectors.toList()))
	);
	private static final Argument<Double> distanceFalloffArgument = new DoubleArgument("distance falloff").includeSuggestions(
		ArgumentSuggestions.stringsWithTooltips(StringTooltip.ofMessage("0", new LiteralMessage("Spawn the particles fully, ignoring PEB.")))
	);
	private static final Argument<String> extraDataArgument = new TextArgument("extra data").includeSuggestions(
		ArgumentSuggestions.strings("null", "\"255,0,0,2.5\"", "REDSTONE_BLOCK")
	);

	@SuppressWarnings("unchecked")
	public static void register() {
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
					.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						doLine(
							getFinalCallee(sender, distanceFalloff),
							0,
							args.getUnchecked("start"),
							args.getUnchecked("end"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(args.getUnchecked("extra data"), particle),
							(Collection<Player>) args.get("allowed viewers")
						);
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
					.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doRectangle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("start"),
							args.getUnchecked("dx"),
							args.getUnchecked("dz"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							(Collection<Player>) args.get("allowed viewers")
						);
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
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							null,
							args.getUnchecked("normal"),
							0,
							360,
							0
						);
					}),
				// Circle - optional player selector argument
				new CommandAPICommand("circle")
					.withPermission("monumenta.particleutils.circle")
					.withArguments(
						new BooleanArgument("ring mode"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument,
						new EntitySelectorArgument.ManyPlayers("allowed viewers")
					)
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							(Collection<Player>) args.get("allowed viewers"),
							args.getUnchecked("normal"),
							0,
							360,
							0
						);
					}),
				// Circle Telegraph
				new CommandAPICommand("circletelegraph")
					.withPermission("monumenta.particleutils.circletelegraph")
					.withArguments(
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new IntegerArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						new DoubleArgument("particle speed"),
						new IntegerArgument("pulses"),
						new IntegerArgument("telegraph duration"),
						new IntegerArgument("pulse start offset"),
						extraDataArgument
					)
					.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircleTelegraph(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							args.getUnchecked("particle speed"),
							args.getUnchecked("pulses"),
							args.getUnchecked("telegraph duration"),
							args.getUnchecked("pulse start offset"),
							parseExtraData(extraData, particle),
							(Collection<Player>) args.get("allowed viewers")
						);
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
						extraDataArgument
					)
					.executes((sender, args) -> {
						int number = args.getUnchecked("number");
						Location location = args.getUnchecked("location");
						double scale = args.getUnchecked("scale");
						double spacing = args.getUnchecked("spacing");
						Player player = args.getUnchecked("facing player");
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						String extraData = args.getUnchecked("extra data");

						ParticleUtils.drawSevenSegmentNumber(number, location, player, scale, spacing, particle, parseExtraData(extraData, particle));
					}),
				new CommandAPICommand("arc")
					.withPermission("monumenta.particleutils.arc")
					.withArguments(
						new BooleanArgument("ring mode"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						new IntegerArgument("start angle"),
						new IntegerArgument("end angle"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							null,
							args.getUnchecked("normal"),
							args.getUnchecked("start angle"),
							args.getUnchecked("end angle"),
							0
						);
					}),
				// allowed viewers
				new CommandAPICommand("arc")
					.withPermission("monumenta.particleutils.arc")
					.withArguments(
						new BooleanArgument("ring mode"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						new IntegerArgument("start angle"),
						new IntegerArgument("end angle"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument,
						new EntitySelectorArgument.ManyPlayers("allowed viewers")
					)
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							(Collection<Player>) args.get("allowed viewers"),
							args.getUnchecked("normal"),
							args.getUnchecked("start angle"),
							args.getUnchecked("end angle"),
							0
						);
					}),
				new CommandAPICommand("animcircle")
					.withPermission("monumenta.particleutils.animcircle")
					.withArguments(
						new BooleanArgument("ring mode"),
						new IntegerArgument("ticks"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						new IntegerArgument("start angle"),
						new IntegerArgument("end angle"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							null,
							args.getUnchecked("normal"),
							args.getUnchecked("start angle"),
							args.getUnchecked("end angle"),
							args.getUnchecked("ticks")
						);
					}),
				// allowed viewers
				new CommandAPICommand("animcircle")
					.withPermission("monumenta.particleutils.animcircle")
					.withArguments(
						new BooleanArgument("ring mode"),
						new IntegerArgument("ticks"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new DoubleArgument("count per meter"),
						new IntegerArgument("start angle"),
						new IntegerArgument("end angle"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument,
						new EntitySelectorArgument.ManyPlayers("allowed viewers")
					)
					.withOptionalArguments(new LocationArgument("normal"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						String extraData = args.getUnchecked("extra data");

						doCircle(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ring mode"),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(extraData, particle),
							(Collection<Player>) args.get("allowed viewers"),
							args.getUnchecked("normal"),
							args.getUnchecked("start angle"),
							args.getUnchecked("end angle"),
							args.getUnchecked("ticks")
						);
					}),
				new CommandAPICommand("animline")
					.withPermission("monumenta.particleutils.animline")
					.withArguments(
						new IntegerArgument("ticks"),
						new LocationArgument("start"),
						new LocationArgument("end"),
						new DoubleArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						doLine(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("ticks"),
							args.getUnchecked("start"),
							args.getUnchecked("end"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(args.getUnchecked("extra data"), particle),
							(Collection<Player>) args.get("allowed viewers")
						);
					}),
				new CommandAPICommand("animspiral")
					.withPermission("monumenta.particleutils.animspiral")
					.withArguments(
						new IntegerArgument("ticks"),
						new LocationArgument("center"),
						new DoubleArgument("radius"),
						new IntegerArgument("curves"),
						new DoubleArgument("curve angle"),
						new IntegerArgument("count per meter"),
						particleArgument,
						distanceFalloffArgument,
						extraDataArgument
					)
					.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
					.executes((sender, args) -> {
						Particle particle = Particle.valueOf(args.getUnchecked("particle"));
						double distanceFalloff = args.getUnchecked("distance falloff");
						doSpiral(
							getFinalCallee(sender, distanceFalloff),
							args.getUnchecked("center"),
							args.getUnchecked("radius"),
							args.getUnchecked("curves"),
							args.getUnchecked("curve angle"),
							args.getUnchecked("count per meter"),
							particle,
							distanceFalloff,
							parseExtraData(args.getUnchecked("extra data"), particle),
							args.getUnchecked("ticks"),
							(Collection<Player>) args.get("allowed viewers")
						);
					})
			)
			.register();
	}

	/**
	 * Returns null if distanceFalloff is 0 (or lower), which will be interpreted as
	 * wanting to spawn the full amount of particles, bypassing player/entity PEB settings.
	 * Useful for puzzles!
	 */
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

		if (isDustColorTransition(particle)) {
			return parseDustTransition(extraData);
		}

		return null;
	}

	private static boolean isRedstone(Particle particle) {
		return particle.equals(Particle.REDSTONE);
	}

	private static boolean isBlock(Particle particle) {
		return particle.equals(Particle.BLOCK_CRACK) || particle.equals(Particle.BLOCK_MARKER);
	}

	private static boolean isDustColorTransition(Particle particle) {
		return particle.equals(Particle.DUST_COLOR_TRANSITION);
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

	private static Particle.DustTransition parseDustTransition(String extraData) {
		try {
			String[] parts = extraData.split(",");
			return new Particle.DustTransition(
				Color.fromRGB(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
				Color.fromRGB(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])),
				Float.parseFloat(parts[6])
			);
		} catch (Exception e) {
			MMLog.warning("Error parsing dust transition: " + extraData);
			return DEFAULT_DUST_COLOR_TRANSITION;
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

	private static void spawnForViewers(Collection<Player> allowedViewers, AbstractPartialParticle<? extends AbstractPartialParticle<?>> particle) {
		particle.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
	}

	private static void spawnForViewers(Collection<Player> allowedViewers, AbstractPartialParticle<? extends AbstractPartialParticle<?>> particle, @Nullable Player callee) {
		particle.spawnForPlayers(ParticleCategory.FULL, allowedViewers, callee);
	}

	private static void doLine(@Nullable CommandSender callee, int ticks, Location start, Location end, double countPerMeter, Particle particle,
	                           double distanceFalloff, @Nullable Object data, @Nullable Collection<Player> allowedViewers) {
		PPLine line = new PPLine(particle, start, end).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff);

		if (ticks != 0) {
			line.delay(ticks);
		}

		if (data != null) {
			line.data(data);
		}

		if (allowedViewers != null) {
			if (callee instanceof Player player) {
				spawnForViewers(allowedViewers, line, player);
			} else {
				spawnForViewers(allowedViewers, line);
			}
		} else {
			spawnAsActive(callee, line);
		}
	}

	private static void doRectangle(@Nullable CommandSender callee, Location start, double dx, double dz, double countPerMeter,
	                                Particle particle, double distanceFalloff, @Nullable Object data, @Nullable Collection<Player> allowedViewers) {
		PPLine[] sides = {
			new PPLine(particle, start, start.clone().add(dx, 0, 0)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(dx, 0, 0), start.clone().add(dx, 0, dz)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(dx, 0, dz), start.clone().add(0, 0, dz)).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff),
			new PPLine(particle, start.clone().add(0, 0, dz), start).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff)
		};

		if (data != null) {
			Arrays.stream(sides).forEach(side -> side.data(data));
		}

		if (allowedViewers != null) {
			Arrays.stream(sides).forEach(side -> spawnForViewers(allowedViewers, side));
		} else {
			Arrays.stream(sides).forEach(side -> spawnAsActive(callee, side));
		}
	}

	private static void doCircle(@Nullable CommandSender callee, boolean ringMode, Location center, double radius,
	                             double countPerMeter, Particle particle, double distanceFalloff, @Nullable Object data,
	                             @Nullable Collection<Player> allowedViewers, @Nullable Location normalLoc, int startAngle, int endAngle, int ticks) {
		PPCircle circle = new PPCircle(particle, center, radius).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff).ringMode(ringMode).arcDegree(startAngle, endAngle).ticks(ticks);

		if (data != null) {
			circle.data(data);
		}

		if (normalLoc != null) {
			Vector up = VectorUtils.rotationToVector(normalLoc.getYaw(), normalLoc.getPitch() - 90);
			Vector right = up.clone().crossProduct(normalLoc.toVector().normalize());
			circle.axes(right, up);
		}

		if (allowedViewers != null) {
			spawnForViewers(allowedViewers, circle);
		} else {
			spawnAsActive(callee, circle);
		}
	}

	private static void doSpiral(@Nullable CommandSender callee, Location center, double radius, int curves, double curveAngle,
	                             int countPerMeter, Particle particle, double distanceFalloff, @Nullable Object data, int ticks, @Nullable Collection<Player> allowedViewers) {

		PPSpiral spiral = new PPSpiral(particle, center, radius).countPerBlockPerCurve(countPerMeter).curves(curves).curveAngle(curveAngle).distanceFalloff(distanceFalloff).ticks(ticks);

		if (data != null) {
			spiral.data(data);
		}

		if (allowedViewers != null) {
			spawnForViewers(allowedViewers, spiral);
		} else {
			spawnAsActive(callee, spiral);
		}
	}


	private static void doCircleTelegraph(@Nullable CommandSender callee, Location center, double radius, int countPerMeter,
	                                      Particle particle, double distanceFalloff, double particleSpeed, int pulses, int telegraphDuration,
	                                      int pulseStartOffset, @Nullable Object data, @Nullable Collection<Player> allowedViewers) {
		// Input Validation
		int finalPulseStartOffset = Math.max(pulseStartOffset, 0);
		int finalTelegraphDuration = Math.max(telegraphDuration, 1);
		int totalPulseTime = finalTelegraphDuration - finalPulseStartOffset;
		int finalPulses = Math.max(pulses, 1);
		int finalPulseDelay = Math.max(totalPulseTime / finalPulses, 1);

		// Static radius ring
		PPCircle circle = new PPCircle(particle, center, radius).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff);
		// Moving pulse ring
		PPCircle pulse = new PPCircle(particle, center, radius).countPerMeter(countPerMeter).distanceFalloff(distanceFalloff)
			.delta(-1, 0, 0).rotateDelta(true).directionalMode(true).extra(particleSpeed);

		if (data != null) {
			circle.data(data);
			pulse.data(data);
		}

		new BukkitRunnable() {

			int mPulses = 0;

			@Override
			public void run() {
				if (allowedViewers != null) {
					spawnForViewers(allowedViewers, circle);
					spawnForViewers(allowedViewers, pulse);
				} else {
					spawnAsActive(callee, circle);
					spawnAsActive(callee, pulse);
				}

				mPulses++;
				if (mPulses >= finalPulses) {
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), finalPulseStartOffset, finalPulseDelay);
	}
}
