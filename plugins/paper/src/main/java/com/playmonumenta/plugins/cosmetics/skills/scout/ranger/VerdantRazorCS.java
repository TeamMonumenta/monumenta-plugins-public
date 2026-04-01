package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class VerdantRazorCS extends RendingRazorCS {
	public static final String NAME = "Verdant Razor";

	private final HashMap<Integer, ItemDisplay> mRazorDisplayMap = new HashMap<>();

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Vine slinging's got a different meaning once",
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

	private static final Particle.DustTransition GREEN = new Particle.DustTransition(Color.fromRGB(140, 210, 45), Color.fromRGB(70, 105, 27), 1.2f);

	@Override
	public void razorCast(Player player) {
		int currentTick = Bukkit.getCurrentTick();
		Location loc = player.getLocation();
		World world = loc.getWorld();

		if (mRazorDisplayMap.get(currentTick) != null) {
			mRazorDisplayMap.get(currentTick).remove();
		}

		mRazorDisplayMap.put(currentTick,
			loc.getWorld().spawn(loc, ItemDisplay.class));
		EntityUtils.setRemoveEntityOnUnload(mRazorDisplayMap.get(currentTick));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> {
				if (mRazorDisplayMap.get(currentTick) != null) {
					mRazorDisplayMap.get(currentTick).remove();
				}
				mRazorDisplayMap.remove(currentTick);
			}, Constants.TICKS_PER_MINUTE);
		mRazorDisplayMap.get(currentTick).setItemStack(DisplayEntityUtils.generateRPItem(Material.STONE_HOE, "Forest's Reaper"));
		mRazorDisplayMap.get(currentTick).setTransformation(
			new Transformation(
				new Vector3f(),
				new AxisAngle4f(),
				new Vector3f(1.4f),
				new AxisAngle4f()
			));
		mRazorDisplayMap.get(currentTick).setTeleportDuration(2);
		mRazorDisplayMap.get(currentTick).setInterpolationDelay(0);

		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.8f);
	}

	@Override
	public void razorProjectileEffects(final Player player, final Location location, int startingTick) {
		ItemDisplay display = mRazorDisplayMap.get(startingTick);
		if (display != null) {
			// TODO: Would be nice if pitch adjusted itself
			Location loc = location.clone();
			loc.setYaw(40 * (Bukkit.getCurrentTick() - startingTick));
			loc.setPitch(90);
			display.teleport(loc);
		}

		new PartialParticle(Particle.DUST_COLOR_TRANSITION, location)
			.count(10)
			.delta(0.2)
			.extra(0.1)
			.data(GREEN)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.ELECTRIC_SPARK, location)
			.count(3)
			.delta(0.2)
			.extra(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.FALLING_DUST, location, 1)
			.delta(0.2)
			.extra(0)
			.data(Bukkit.createBlockData(Material.GREEN_TERRACOTTA))
			.spawnAsPlayerActive(player);
	}

	@Override
	public void razorTravelSound(final Player player, final Location location) {
		location.getWorld().playSound(location, "minecraft:entity.breeze.charge", SoundCategory.PLAYERS, 1f, 1f);
	}

	@Override
	public void razorHit(final Player player, final Location location) {
		final World world = player.getWorld();

		new PartialParticle(Particle.BLOCK_CRACK, location, 150)
			.delta(0.4)
			.extra(0)
			.data(Bukkit.createBlockData(Material.AZALEA_LEAVES))
			.spawnAsPlayerActive(player);

		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(location, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(location, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(location, "minecraft:entity.armadillo.hurt_reduced", SoundCategory.PLAYERS, 2f, 0.4f);
	}

	@Override
	public void razorPierce(Player player, Location location) {
		final World world = location.getWorld();

		new PartialParticle(Particle.DAMAGE_INDICATOR, location.clone().add(0, 0.5, 0))
			.count(5)
			.delta(0.2)
			.extra(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.BLOCK_CRACK, location, 30)
			.delta(0.3)
			.extra(0)
			.data(Bukkit.createBlockData(Material.AZALEA_LEAVES))
			.spawnAsPlayerActive(player);

		world.playSound(location, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.8f, 1.5f);
		world.playSound(location, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(location, Sound.ENTITY_BEE_STING, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 2f);
	}

	@Override
	public void razorReturned(final Location loc, int startingTick) {
		World world = loc.getWorld();

		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, "block.vault.insert_item", SoundCategory.PLAYERS, 1.8f, 1.1f);

		ItemDisplay display = mRazorDisplayMap.remove(startingTick);
		if (display != null) {
			display.remove();
		}
	}

	@Override
	public void onDeath() {
		// Just in case the player dies / unloads, hopefully proof against memory leaks
		for (ItemDisplay display : mRazorDisplayMap.values()) {
			if (display != null) {
				display.remove();
			}
		}
		mRazorDisplayMap.clear();
	}
}
