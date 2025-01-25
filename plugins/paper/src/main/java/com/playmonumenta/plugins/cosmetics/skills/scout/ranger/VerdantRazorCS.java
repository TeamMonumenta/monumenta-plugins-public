package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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
	private @Nullable ItemDisplay mRazorDisplay = null;

	@Override
	public void onCast(Player player, Location loc, World world) {
		if (mRazorDisplay != null) {
			mRazorDisplay.remove();
		}

		mRazorDisplay = loc.getWorld().spawn(loc, ItemDisplay.class);
		EntityUtils.setRemoveEntityOnUnload(mRazorDisplay);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), mRazorDisplay::remove, 30);
		mRazorDisplay.setItemStack(DisplayEntityUtils.generateRPItem(Material.CROSSBOW, "Steelsage Talisman"));

		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1.5f, 1.5f);

	}

	@Override
	public void hitMob(Player player, Location loc, World world) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.4f, 1.1f);
	}

	@Override
	public void tick(Player player, Location bladeLoc, World world, Location playerLoc, double throwRadius, double bladeRadius, int degrees) {
		Vector front = LocationUtils.getDirectionTo(bladeLoc, playerLoc.clone().add(0, 1, 0));
		Vector left = VectorUtils.rotateTargetDirection(front, 90, 0);
		Vector down = VectorUtils.rotateTargetDirection(front, 0, 90);
		double distance;
		switch (degrees) {
			case 0, 360 -> distance = throwRadius;
			default -> distance = throwRadius + bladeRadius;
		}
		bladeLoc = playerLoc.clone().add(front.clone().multiply(distance)).add(0, 0.375, 0).setDirection(down);
		playerLoc.add(0, 0.75, 0).add(degrees == 0 ? new Vector(0, 0, 0) : front);

		final Particle.DustTransition GREEN = new Particle.DustTransition(Color.fromRGB(140, 210, 45 + (int) (45 * Math.sin(degrees))), Color.fromRGB(70, 105, 27), 1.2f);
		new PPLine(Particle.DUST_COLOR_TRANSITION, playerLoc, bladeLoc, 0.08).countPerMeter(10).data(GREEN).spawnAsPlayerActive(player);
		new PPLine(Particle.SPORE_BLOSSOM_AIR, playerLoc, bladeLoc).delta(0.15).countPerMeter(0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WHITE_SMOKE, bladeLoc).count(4).delta(left.getX(), 0, left.getZ()).extra(0.2).directionalMode(true).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WHITE_SMOKE, bladeLoc).count(3).delta(left.getX(), 0, left.getZ()).extra(0.16).directionalMode(true).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, bladeLoc).count(2).delta(left.getX(), 0, left.getZ()).extra(0.12).directionalMode(true).spawnAsPlayerActive(player);

		world.playSound(bladeLoc, Sound.BLOCK_AZALEA_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);

		if (mRazorDisplay != null) {
			mRazorDisplay.teleport(bladeLoc);
		}
		mEndLoc = bladeLoc;
	}

	@Override
	public void end(World world, Location loc, Player player) {
		world.playSound(loc, "block.vault.insert_item", SoundCategory.PLAYERS, 1.8f, 1.1f);
		Location playerLoc = player.getLocation().add(0, 1, 0);
		if (mEndLoc != null && mRazorDisplay != null) {
			Vector toBlade = LocationUtils.getVectorTo(mEndLoc, playerLoc);
			final Particle.DustTransition GREEN = new Particle.DustTransition(Color.fromRGB(140, 210, 45), Color.fromRGB(70, 105, 27), 1.2f);
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mTicks >= 4) {
						if (mRazorDisplay != null) {
							mRazorDisplay.remove();
						}
						this.cancel();
					}
					if (mRazorDisplay != null && mEndLoc != null) {
						Location playerLoc = player.getLocation().add(0, 0.75, 0);
						Location bladeLoc = playerLoc.clone().add(toBlade.clone().multiply(1 - mTicks * 0.25)).setDirection(mEndLoc.getDirection());
						mRazorDisplay.teleport(bladeLoc);
						new PPLine(Particle.DUST_COLOR_TRANSITION, playerLoc, bladeLoc, 0.1).countPerMeter(3).data(GREEN).spawnAsPlayerActive(player);
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}
}
