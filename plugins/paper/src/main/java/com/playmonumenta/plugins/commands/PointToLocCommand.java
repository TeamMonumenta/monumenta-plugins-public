package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.ParticleArgument;
import dev.jorel.commandapi.wrappers.ParticleData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PointToLocCommand {
	public static void register(Plugin plugin) {
		// Point to Location command
		// draws a line of particles from the player's location, towards the target location
		new CommandAPICommand("pointtolocation")
			.withPermission(CommandPermission.fromString("monumenta.command.pointtolocation"))
			.withArguments(
				new LocationArgument("start"),
				new LocationArgument("end"),
				new IntegerArgument("count"),
				new FloatArgument("distanceBetween"),
				new FloatArgument("acceleration"),
				new IntegerArgument("ticksBetween"),
				new ParticleArgument("midParticle"),
				new ParticleArgument("helixParticle"),
				new FloatArgument("radius"),
				new BooleanArgument("skrUsage")
			)
			.executes((sender, args) -> {
				Player player = (Player) CommandUtils.getCallee(sender);
				Location startpoint = ((Location) args.getUnchecked("start")).clone();
				Location endpoint = (Location) args.getUnchecked("end");
				int count = (int) args.getUnchecked("count");
				Vector direction = endpoint.toVector().subtract(startpoint.toVector());
				Vector endNormalized = direction.normalize().multiply((float) args.getUnchecked("distanceBetween"));
				int timer = args.getUnchecked("ticksBetween");
				float acceleration = (float) args.getUnchecked("acceleration");
				ParticleData<?> midParticleData = (ParticleData<?>) args.getUnchecked("midParticle");
				ParticleData<?> helixParticleData = (ParticleData<?>) args.getUnchecked("helixParticle");
				float radius = (float) args.getUnchecked("radius");
				boolean skr = (boolean) args.getUnchecked("skrUsage");

				Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
				// Particle.DustTransition skrColours = new Particle.DustTransition(Color.fromRGB(255, 13, 247), Color.fromRGB(75, 35, 158), 1f);
				final int[] mAngleOne = {0};
				final int[] mAngleTwo = {180};

				for (int i = 0; i < count; i++) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						if (startpoint.distanceSquared(endpoint) <= endNormalized.lengthSquared()) {
							return;
						}
						startpoint.add(endNormalized);
						endNormalized.multiply(acceleration);
						new PartialParticle(midParticleData.particle(), startpoint, 2, 0, 0, 0).spawnAsPlayerActive(player);
						if (!skr) {
							new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleOne[0], mAngleOne[0]).spawnAsEnemy();
							new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleTwo[0], mAngleTwo[0]).spawnAsEnemy();
						} else {
							double length = startpoint.distanceSquared(endpoint);
							Color helixColor;
							if (length < 100 * 100) {
								helixColor = Color.GREEN;
							} else if (length < 250 * 250) {
								helixColor = Color.YELLOW;
							} else if (length < 500 * 500) {
								helixColor = Color.ORANGE;
							} else {
								helixColor = Color.RED;
							}
							Particle.DustTransition skrColours = new Particle.DustTransition(helixColor, helixColor, 1f);
							new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleOne[0], mAngleOne[0]).spawnAsEnemy();
							new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleTwo[0], mAngleTwo[0]).spawnAsEnemy();
						}
						mAngleOne[0] = (mAngleOne[0] + 10) % 360;
						mAngleTwo[0] = (mAngleTwo[0] + 10) % 360;
					}, i * (long) timer);
				}
			})
			.register();
	}
}

