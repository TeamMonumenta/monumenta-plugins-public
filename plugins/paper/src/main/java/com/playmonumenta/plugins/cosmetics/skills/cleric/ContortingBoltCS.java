package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ContortingBoltCS extends HandOfLightCS {

	public static final String NAME = "Contorting Bolt";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The projectile twists and turns towards your",
			"enemies, draining their life force and transmitting it",
			"to your allies. It's as if it has a mind of its own.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.PRISMARINE_SHARD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 180, 200), 1.1f);
	private static final Particle.DustOptions PINK = new Particle.DustOptions(Color.fromRGB(200, 100, 200), 1.1f);

	@Override
	public void lightHealEffect(Player player, Location loc, Player mTarget) {

	}

	@Override
	public void lightHealCastEffect(World world, Location userLoc, Plugin mPlugin, Player player, float radius, double angle, List<Player> players) {
		int delay;
		switch (players.size()) {
			case 1 -> delay = 5;
			case 2 -> delay = 4;
			case 3 -> delay = 3;
			case 4 -> delay = 2;
			default -> delay = 1;
		}
		new BukkitRunnable() {
			int NUMBER = -1;
			@Override
			public void run() {
				LivingEntity target = players.get(NUMBER + 1);
				if (NUMBER == -1) {
					// Chain from caster to first player
					new PPLine(Particle.REDSTONE, player.getLocation().add(0, 1, 0), players.get(0).getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(PINK).spawnAsPlayerActive(player);
					new PartialParticle(Particle.HEART, players.get(0).getEyeLocation().add(0, -0.5, 0)).count(12).delta(0.4, 0.8, 0.4).spawnAsPlayerActive(player);
					player.getWorld().playSound(players.get(0).getLocation(), Sound.BLOCK_SHROOMLIGHT_STEP, SoundCategory.PLAYERS, 1.0f + 0.5f / players.size(), 1.6f);
					player.getWorld().playSound(players.get(0).getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.4f + 0.5f / players.size(), 1.0f);
				} else if (NUMBER < players.size() - 1) {
					// Chain from player to player
					new PPLine(Particle.REDSTONE, players.get(NUMBER).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(PINK).spawnAsPlayerActive(player);
					new PartialParticle(Particle.HEART, target.getEyeLocation().add(0, -0.5, 0)).count(12).delta(0.4, 0.8, 0.4).spawnAsPlayerActive(player);
					player.getWorld().playSound(target.getLocation(), Sound.BLOCK_SHROOMLIGHT_STEP, SoundCategory.PLAYERS, 1.0f + 0.5f / players.size(), 1.5f);
					player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.4f + 0.5f / players.size(), 0.9f);
				}
				if (NUMBER == players.size() - 2) {
					// Chain from last player to caster
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						new PPLine(Particle.REDSTONE, player.getLocation().add(0, 1, 0), players.get(NUMBER).getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(PINK).spawnAsPlayerActive(player);
						new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0)).count(8).delta(0.6, 1.0, 0.6).spawnAsPlayerActive(player);
						player.getWorld().playSound(player.getLocation().add(0, 1, 0), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 2.0f);
						player.getWorld().playSound(player.getLocation().add(0, 1, 0), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.5f, 1.0f);
						this.cancel();
					}, delay);
				}
				NUMBER++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, delay);
	}

	@Override
	public void lightDamageEffect(Player player, Location loc, LivingEntity target, List<LivingEntity> undeadMobs) {

	}

	@Override
	public void lightDamageCastEffect(World world, Location userLoc, Plugin mPlugin, Player player, float radius, double angle, List<LivingEntity> mobs) {
		int delay;
		switch (mobs.size()) {
			case 1 -> delay = 5;
			case 2 -> delay = 4;
			case 3 -> delay = 3;
			case 4 -> delay = 2;
			default -> delay = 1;
		}
		new BukkitRunnable() {
			int mInt = -1;
			@Override
			public void run() {
				LivingEntity target = mobs.get(mInt + 1);
				if (mInt == -1) {
					// Chain from caster to first mob
					new PPLine(Particle.REDSTONE, player.getLocation().add(0, 1, 0), mobs.get(0).getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(CYAN).spawnAsPlayerActive(player);
					new PartialParticle(Particle.ELECTRIC_SPARK, mobs.get(0).getEyeLocation().add(0, -0.5, 0)).count(12).delta(0.4, 0.8, 0.4).spawnAsPlayerActive(player);
					player.getWorld().playSound(mobs.get(0).getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.0f + 0.5f / mobs.size(), 1.6f);
					player.getWorld().playSound(mobs.get(0).getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.4f + 0.5f / mobs.size(), 1.0f);
					player.getWorld().playSound(mobs.get(0).getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.PLAYERS, 1.2f + 0.5f / mobs.size(), 1.1f);
					// Hieroglyph for "Pierce"
					Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
					Vector front = player.getEyeLocation().getDirection().setY(0).normalize().multiply(0.75);
					Vector left90 = VectorUtils.rotateTargetDirection(front, -90, 0);
					Vector right90 = VectorUtils.rotateTargetDirection(front, 90, 0);
					Location loc1 = loc.clone().add(left90);
					Location loc2 = loc.clone().add(right90);
					for (int i = 0; i < 2; i++) {
						double delta = 0.2 * i;
						final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(50 - 10 * i, 100 - 20 * i, 200 - 40 * i), 1.2f - i * 0.2f);
						new PPLine(Particle.REDSTONE, loc.clone().add(front.clone().multiply(0.5)), loc.clone().subtract(front.clone().multiply(2))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front.clone().multiply(0.5)), loc1).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front.clone().multiply(0.5)), loc2).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front.clone().multiply(2)), loc1.clone().add(front.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front.clone().multiply(2)), loc2.clone().add(front.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc1, loc1.clone().add(front.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc2, loc2.clone().add(front.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front).add(left90), loc.clone().add(front).add(left90.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE, loc.clone().add(front).add(right90), loc.clone().add(front).add(right90.clone().multiply(1.5))).data(BLUE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
					}
					new PPCircle(Particle.ENCHANTMENT_TABLE, loc, 2).countPerMeter(10).extraRange(0.1, 0.15).innerRadiusFactor(1)
						.directionalMode(true).delta(1, 0.2, -4).rotateDelta(true).spawnAsPlayerActive(player);
				} else if (mInt < mobs.size() - 1) {
					// Chain from mob to mob
					new PPLine(Particle.REDSTONE, mobs.get(mInt).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(CYAN).spawnAsPlayerActive(player);
					new PartialParticle(Particle.ELECTRIC_SPARK, target.getEyeLocation().add(0, -0.5, 0)).count(12).delta(0.4, 0.8, 0.4).spawnAsPlayerActive(player);
					player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.0f + 0.5f / mobs.size(), 1.5f);
					player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.4f + 0.5f / mobs.size(), 0.9f);
					player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.PLAYERS, 1.2f + 0.5f / mobs.size(), 1.0f);
				}
				if (mInt == mobs.size() - 2) {
					// Chain from last mob to caster
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						new PPLine(Particle.REDSTONE, player.getLocation().add(0, 1, 0), mobs.get(mInt).getEyeLocation().add(0, -0.5, 0), 0.08).countPerMeter(12).groupingDistance(0).data(CYAN).spawnAsPlayerActive(player);
						new PartialParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0)).count(8).delta(0.6, 1.0, 0.6).spawnAsPlayerActive(player);
						player.getWorld().playSound(player.getLocation().add(0, 1, 0), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 1.6f);
						player.getWorld().playSound(player.getLocation().add(0, 1, 0), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.5f, 0.8f);
						this.cancel();
						}, delay);
				}
				mInt++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, delay);
	}
}
