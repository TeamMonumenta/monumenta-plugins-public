package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpectralSerpentCS extends HallowedBeamCS {

	public static final String NAME = "Spectral Serpent";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The presence of wools has left a stain on the Ishniran fauna,",
			"turning some into mere ghostly husks. Death gave them power,",
			"and now they serve the Mind."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.LEAD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void beamHealEffect(Player player, LivingEntity target, Vector dir, double range, Location targetLocation) {
		World world = player.getWorld();
		Location serpentLoc = player.getLocation();
		Location playerLoc = player.getLocation();
		Location targetLoc = target.getLocation();
		world.playSound(playerLoc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.1f, 1.9f);
		world.playSound(playerLoc, Sound.ENTITY_CAT_HISS, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(targetLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.3f);
		world.playSound(targetLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.1f);
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), targetLoc, 2, 0, 6,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.VILLAGER_HAPPY, location, 1, 0, 0, 0, 0.05).directionalMode(true).spawnAsPlayerActive(player))
			)
		);
		ItemDisplay serpentHead = serpentLoc.getWorld().spawn(serpentLoc, ItemDisplay.class);
		EntityUtils.setRemoveEntityOnUnload(serpentHead);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), serpentHead::remove, 8);
		new BukkitRunnable() {
			int mSerpentHeadStage = 0;
			@Override
			public void run() {
				Vector direction = LocationUtils.getDirectionTo(targetLoc, playerLoc);
				serpentLoc.add(direction.clone().multiply(0.125 * playerLoc.distance(targetLoc)));
				serpentHead.setItemStack(DisplayEntityUtils.generateRPItem(Material.BLACK_CANDLE, "Spectral Serpent Head " + mSerpentHeadStage));
				serpentHead.teleport(serpentLoc);
				world.playSound(serpentLoc, Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);
				new PartialParticle(Particle.SWEEP_ATTACK, serpentLoc, 1, 0.0, 0.0, 0.0, 0.0f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.SCULK_SOUL, serpentLoc, 2, 0.2, 0.1, 0.2, 0.1f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.HEART, targetLoc, 1, 0.6, 1.2, 0.6, 0.0f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.BLOCK_CRACK, serpentLoc, 20, 0.15, 0.05, 0.15, 0.1f).data(Material.SCULK.createBlockData()).spawnAsPlayerActive(player);
				mSerpentHeadStage++;
				if (mSerpentHeadStage >= 8) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void beamHarm(Player player, LivingEntity target, Vector dir, double range, Location targetLocation) {
		World world = player.getWorld();
		Location serpentLoc = player.getLocation();
		Location playerLoc = player.getLocation();
		Location targetLoc = target.getLocation();
		world.playSound(playerLoc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.1f, 1.7f);
		world.playSound(playerLoc, Sound.ENTITY_CAT_HISS, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(targetLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.3f);
		world.playSound(targetLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.1f);
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), targetLoc, 2, 0, 6,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.DAMAGE_INDICATOR, location.clone().subtract(0, 0.8, 0), 1, 0, 0, 0, 0).directionalMode(true).spawnAsPlayerActive(player))
			)
		);
		ItemDisplay serpentHead = serpentLoc.getWorld().spawn(serpentLoc, ItemDisplay.class);
		EntityUtils.setRemoveEntityOnUnload(serpentHead);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), serpentHead::remove, 8);
		new BukkitRunnable() {
			int mSerpentHeadStage = 0;
			@Override
			public void run() {
				Vector direction = LocationUtils.getDirectionTo(targetLoc, playerLoc);
				serpentLoc.add(direction.clone().multiply(0.125 * playerLoc.distance(targetLoc)));
				serpentHead.setItemStack(DisplayEntityUtils.generateRPItem(Material.BLACK_CANDLE, "Spectral Serpent Head " + mSerpentHeadStage));
				serpentHead.teleport(serpentLoc);
				world.playSound(serpentLoc, Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);
				new PartialParticle(Particle.SWEEP_ATTACK, serpentLoc, 1, 0.0, 0.0, 0.0, 0.0f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.SCULK_SOUL, serpentLoc, 2, 0.2, 0.1, 0.2, 0.1f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.SCULK_SOUL, targetLoc, 1, 0.6, 1.2, 0.6, 0.1f).spawnAsPlayerActive(player);
				new PartialParticle(Particle.BLOCK_CRACK, serpentLoc, 25, 0.15, 0.05, 0.15, 0.1f).data(Material.SCULK.createBlockData()).spawnAsPlayerActive(player);
				if (mSerpentHeadStage >= 8) {
					this.cancel();
				}
				mSerpentHeadStage++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void beamHarmCrusade(Player player, LivingEntity target, Location targetLocation) {

	}

	@Override
	public void beamHarmOther(Player player, LivingEntity target, Location targetLocation) {

	}
}
