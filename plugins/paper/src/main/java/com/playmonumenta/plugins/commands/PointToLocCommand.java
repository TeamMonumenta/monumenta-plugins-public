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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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

				new BukkitRunnable() {
					int mCount = 0;
					int mAngleOne = 0;
					int mAngleTwo = 180;
					@Override
					public void run() {
						if (mCount >= count) {
							this.cancel();
							return;
						}
						if (startpoint.distanceSquared(endpoint) <= endNormalized.lengthSquared()) {
							return;
						}
						startpoint.add(endNormalized);
						endNormalized.multiply(acceleration);
						PartialParticle midParticle = new PartialParticle(midParticleData.particle(), startpoint, 2, 0, 0, 0, midParticleData.data());
						PPCircle helix1;
						PPCircle helix2;
						if (!skr) {
							helix1 = new PPCircle(helixParticleData.particle(), startpoint, radius).data(helixParticleData.data()).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleOne, mAngleOne);
							helix2 = new PPCircle(helixParticleData.particle(), startpoint, radius).data(helixParticleData.data()).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleTwo, mAngleTwo);
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
							helix1 = new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleOne, mAngleOne);
							helix2 = new PPCircle(Particle.DUST_COLOR_TRANSITION, startpoint, radius).data(skrColours).countPerMeter(1).directionalMode(false).rotateDelta(true).axes(up, right).ringMode(true).arcDegree(mAngleTwo, mAngleTwo);
						}
						if (allowedViewers != null) {
							midParticle.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
							helix1.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
							helix2.spawnForPlayers(ParticleCategory.FULL, allowedViewers);
						} else {
							CommandSender csender = CommandUtils.getCallee(sender);
							if (!(csender instanceof Player player)) {
								sender.sendMessage(Component.text("/pointtolocation cannot be used in a command block without allowed_viewers", NamedTextColor.RED));
								this.cancel(); // this isn't going to change, so only send the error once
								return;
							}
							midParticle.spawnAsPlayerActive(player);
							helix1.spawnAsPlayerActive(player);
							helix2.spawnAsPlayerActive(player);
						}
						mAngleOne = (mAngleOne + 10) % 360;
						mAngleTwo = (mAngleTwo + 10) % 360;

						mCount++;
					}
				}.runTaskTimer(plugin, 0, timer);
			})
			.register();
	}
}
