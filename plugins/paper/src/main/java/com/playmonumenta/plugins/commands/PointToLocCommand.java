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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PointToLocCommand {
	public static void register(Plugin plugin) {
		// Point to Location command
		// draws a line of particles from the player's location, towards the target location
		{
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
					Location startpoint = (Location) args.getUnchecked("start");
					Location endpoint = (Location) args.getUnchecked("end");
					int count = (int) args.getUnchecked("count");
					Vector direction = endpoint.toVector().subtract(startpoint.toVector());
					Vector endNormalized = direction.normalize().multiply((float) args.getUnchecked("distanceBetween"));
					int timer = args.getUnchecked("ticksBetween");
					ParticleData<?> midParticleData = (ParticleData<?>) args.getUnchecked("midParticle");
					ParticleData<?> helixParticleData = (ParticleData<?>) args.getUnchecked("helixParticle");
					float radius = (float) args.getUnchecked("radius");
					boolean skr = (boolean) args.getUnchecked("skrUsage");
					Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
					Particle.DustTransition skrColours = new Particle.DustTransition(Color.fromRGB(255, 13, 247), Color.fromRGB(75, 35, 158), 1f);
					for (int i = 0; i < count; i++) {
						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							startpoint.add(endNormalized);
							endNormalized.multiply((float) args.getUnchecked("acceleration"));
							new PartialParticle(midParticleData.particle(), startpoint, 2, 0, 0, 0)
								.spawnAsPlayerActive(player);

						}, i * (long) timer);
					}
					new BukkitRunnable() {
						int mAngleOne = 0;
						int mAngleTwo = 180;
						int mTicks = 0;
						@Override
						public void run() {
							if (mTicks < count * timer) {
								if (!skr) {
									new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleOne, mAngleOne).spawnAsEnemy();
									new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleTwo, mAngleTwo).spawnAsEnemy();
								} else {
									new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleOne, mAngleOne).spawnAsEnemy();
									new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleTwo, mAngleTwo).spawnAsEnemy();
								}

								mAngleOne += 10;
								mAngleTwo += 10;
								if (mAngleOne == 360) {
									mAngleOne = 0;
								}
								if (mAngleTwo == 360) {
									mAngleTwo = 0;
								}
							}
							if (mTicks >= count * timer) {
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 1);
				})
				.register();
		}
	}
}
