package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.ParticleArgument;
import dev.jorel.commandapi.wrappers.ParticleData;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PointToLocCommand {
	@SuppressWarnings("unchecked")
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
			.withOptionalArguments(new EntitySelectorArgument.ManyPlayers("allowed viewers"))
			.executes((sender, args) -> {
				Player player = (Player) CommandUtils.getCallee(sender);
				Location startpoint = ((Location) args.getUnchecked("start")).clone();
				Location endpoint = args.getUnchecked("end");
				int count = args.getUnchecked("count");
				Vector direction = endpoint.toVector().subtract(startpoint.toVector());
				Vector endNormalized = direction.normalize().multiply((float) args.getUnchecked("distanceBetween"));
				int timer = args.getUnchecked("ticksBetween");
				float acceleration = args.getUnchecked("acceleration");
				ParticleData<?> midParticleData = args.getUnchecked("midParticle");
				ParticleData<?> helixParticleData = args.getUnchecked("helixParticle");
				float radius = args.getUnchecked("radius");
				boolean skr = args.getUnchecked("skrUsage");
				Collection<Player> allowedViewers = (Collection<Player>) args.get("allowed viewers");

				Vector right;
				Vector up;
				if (direction.clone().normalize().equals(new Vector(0, 1, 0)) || direction.clone().normalize().equals(new Vector(0, -1, 0))) {
					right = new Vector(1, 0, 0);
					up = new Vector(0, 0, 1);
				} else {
					right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
					up = direction.clone().crossProduct(right).normalize();
				}
				final int[] mAngleOne = {0};
				final int[] mAngleTwo = {180};

				for (int i = 0; i < count; i++) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						if (startpoint.distanceSquared(endpoint) <= endNormalized.lengthSquared()) {
							return;
						}
						startpoint.add(endNormalized);
						endNormalized.multiply(acceleration);
						PartialParticle midParticle = new PartialParticle(midParticleData.particle(), startpoint, 2, 0, 0, 0);
						PPCircle helix1;
						PPCircle helix2;
						if (!skr) {
							helix1 = new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleOne[0], mAngleOne[0]);
							helix2 = new PPCircle(helixParticleData.particle(), startpoint, radius).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleTwo[0], mAngleTwo[0]);
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
							helix1 = new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleOne[0], mAngleOne[0]);
							helix2 = new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleTwo[0], mAngleTwo[0]);
						}
						if (allowedViewers != null) {
							midParticle.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
							helix1.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
							helix2.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
						} else {
							midParticle.spawnAsPlayerActive(player);
							helix1.spawnAsEnemy();
							helix2.spawnAsEnemy();
						}
						mAngleOne[0] = (mAngleOne[0] + 10) % 360;
						mAngleTwo[0] = (mAngleTwo[0] + 10) % 360;
					}, i * (long) timer);
				}
			})
			.register();
	}
}
