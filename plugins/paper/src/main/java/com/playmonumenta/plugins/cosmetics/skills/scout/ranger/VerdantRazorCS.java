package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class VerdantRazorCS extends WhirlingBladeCS {

	public static final String NAME = "Verdant Razor";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Vine swinging's got a different meaning once",
			"you attach a sharp object to the end of it.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_INGOT;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private @Nullable Location mEndLoc = null;
	private final HashMap<Integer, ItemDisplay> razorDisplayMap = new HashMap<>();

	@Override
	public void onCast(Player player, Location loc, World world) {
		int currentTick = Bukkit.getCurrentTick();
		if (razorDisplayMap.get(currentTick) != null) {
			razorDisplayMap.get(currentTick).remove();
		}

		razorDisplayMap.put(currentTick,
			loc.getWorld().spawn(loc, ItemDisplay.class));
		EntityUtils.setRemoveEntityOnUnload(razorDisplayMap.get(currentTick));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> {
				if (razorDisplayMap.get(currentTick) != null) {
					razorDisplayMap.get(currentTick).remove();
				}
				razorDisplayMap.remove(currentTick);
			}, Constants.TICKS_PER_MINUTE);
		razorDisplayMap.get(currentTick).setItemStack(DisplayEntityUtils.generateRPItem(Material.CROSSBOW, "Steelsage Talisman"));
		razorDisplayMap.get(currentTick).setTransformation(
			new Transformation(
				new Vector3f(0),
				new AxisAngle4f(),
				new Vector3f(1.2f),
				new AxisAngle4f()
			));
		razorDisplayMap.get(currentTick).setTeleportDuration(1);

		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1.5f, 1.5f);
	}

	@Override
	public void hitMob(Player player, Location loc, World world) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.4f, 1.1f);
	}

	@Override
	public void tick(Player player, Location bladeLoc, World world, Location playerLoc, double throwRadius, double bladeRadius, int degrees, int startingTick, boolean isFirstCycle) {
		Location oldLoc = bladeLoc;

		Vector front = LocationUtils.getDirectionTo(bladeLoc, playerLoc.clone().add(0, 1, 0));
		Vector left = VectorUtils.rotateTargetDirection(front, 90, 0);
		Vector down = VectorUtils.rotateTargetDirection(front, 0, 90);

		double distance;
		if (isFirstCycle && degrees == 0) {
			distance = throwRadius;
		} else {
			distance = throwRadius + bladeRadius;
		}

		bladeLoc = playerLoc.clone().add(front.clone().multiply(distance)).add(0, 0.5, 0).setDirection(down);
		playerLoc.add(0, 0.75, 0).add(isFirstCycle && degrees == 0 ? new Vector(0, 0, 0) : front);

		final Particle.DustTransition GREEN = new Particle.DustTransition(Color.fromRGB(140, 210, 45 + (int) (45 * Math.sin(degrees))), Color.fromRGB(70, 105, 27), 1.2f);
		new PPLine(Particle.DUST_COLOR_TRANSITION, playerLoc, bladeLoc, 0.08).countPerMeter(10).data(GREEN).spawnAsPlayerActive(player);
		new PPLine(Particle.SPORE_BLOSSOM_AIR, playerLoc, bladeLoc).delta(0.15).countPerMeter(0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WHITE_SMOKE, bladeLoc).count(4).delta(left.getX(), 0, left.getZ()).extra(0.2).directionalMode(true).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WHITE_SMOKE, bladeLoc).count(3).delta(left.getX(), 0, left.getZ()).extra(0.16).directionalMode(true).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, bladeLoc).count(2).delta(left.getX(), 0, left.getZ()).extra(0.12).directionalMode(true).spawnAsPlayerActive(player);

		world.playSound(bladeLoc, Sound.BLOCK_AZALEA_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);

		if (razorDisplayMap.get(startingTick) != null) {
			// Razor has to "teleport" "twice as far" to account for travel time
			razorDisplayMap.get(startingTick).teleport(bladeLoc.clone().add(bladeLoc.clone().subtract(oldLoc)));
		}
		mEndLoc = bladeLoc;
	}

	@Override
	public void end(World world, Location loc, Player player, int startingTick) {
		world.playSound(loc, "block.vault.insert_item", SoundCategory.PLAYERS, 1.8f, 1.1f);
		Location playerLoc = player.getLocation().add(0, 1, 0);
		if (mEndLoc != null && razorDisplayMap.get(startingTick) != null) {
			Vector toBlade = LocationUtils.getVectorTo(mEndLoc, playerLoc);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks >= 4) {
						if (razorDisplayMap.get(startingTick) != null) {
							razorDisplayMap.get(startingTick).remove();
						}
						razorDisplayMap.remove(startingTick);
						this.cancel();
					}
					if (razorDisplayMap.get(startingTick) != null && mEndLoc != null) {
						Location playerLoc = player.getEyeLocation().add(0, -0.5, 0);
						Location bladeLoc = playerLoc.clone().add(toBlade.clone().multiply(1 - mTicks * 0.34)).setDirection(mEndLoc.getDirection());
						razorDisplayMap.get(startingTick).teleport(bladeLoc);
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	@Override
	public void onDeath() {
		// Just in case the player dies / unloads, hopefully proof against memory leaks
		for (ItemDisplay display : razorDisplayMap.values()) {
			if (display != null) {
				display.remove();
			}
		}
		razorDisplayMap.clear();
	}
}
