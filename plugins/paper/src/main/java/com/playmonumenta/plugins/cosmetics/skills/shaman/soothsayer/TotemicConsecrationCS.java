package com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TotemicConsecrationCS implements CosmeticSkill {
	private static final Particle.DustOptions COLOR_GOLD = new Particle.DustOptions(Color.fromRGB(255, 206, 92), 0.75f);
	private static final int CIRCLE_COUNT = 7;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TOTEMIC_CONSECRATION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.YELLOW_CANDLE;
	}

	public void blessedTotemTick(Player player, Location standLocation, double radius) {
		PPCircle outerRing = new PPCircle(Particle.FIREWORKS_SPARK, standLocation.add(0, 0.15, 0), radius).ringMode(true).countPerMeter(0.2).delta(0);
		PPCircle outerRing2 = new PPCircle(Particle.REDSTONE, standLocation.add(0, 0.1, 0), radius).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
		outerRing.spawnAsPlayerActive(player);
		outerRing2.spawnAsPlayerActive(player);

		PPCircle innerRing = new PPCircle(Particle.FIREWORKS_SPARK, standLocation.add(0, 0.45, 0), 0.5).ringMode(true).countPerMeter(0.2).delta(0);
		PPCircle innerRing2 = new PPCircle(Particle.REDSTONE, standLocation.add(0, 0.15, 0), 0.5).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
		PPCircle innerRing4 = new PPCircle(Particle.REDSTONE, standLocation.add(0, 0.3, 0), 0.5).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
		innerRing.spawnAsPlayerActive(player);
		innerRing2.spawnAsPlayerActive(player);
		innerRing4.spawnAsPlayerActive(player);
	}

	public void consecrationAction(Player player, Location standLocation, double radius) {
		new BukkitRunnable() {
			double mTicks = 0;

			@Override
			public void run() {
				if (mTicks > CIRCLE_COUNT) {
					this.cancel();
				}

				PPCircle ring = new PPCircle(Particle.REDSTONE, standLocation.add(0, -0.1, 0), radius * mTicks / CIRCLE_COUNT).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
				PPCircle ring2 = new PPCircle(Particle.FIREWORKS_SPARK, standLocation.add(0, 0.4, 0), radius * mTicks / CIRCLE_COUNT).ringMode(true).countPerMeter(0.6).delta(0);
				ring.spawnAsPlayerActive(player);
				ring2.spawnAsPlayerActive(player);

				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		Location loc = player.getLocation();
		player.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 2f, 0.5f);
		player.playSound(loc, Sound.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 0.8f, 0.8f);
		player.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 1.4f, 2f);
		player.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.4f, 0.5f);
		player.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.0f, 1.7f);
		player.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2f, 1.4f);
	}

	public void absorptionTriggered(Player activatedPlayer, Location standLocation, double radius) {
		new BukkitRunnable() {
			double mTicks = 0;

			@Override
			public void run() {
				if (mTicks > CIRCLE_COUNT) {
					this.cancel();
				}

				PPCircle ring = new PPCircle(Particle.REDSTONE, standLocation.add(0, -0.1, 0), radius * (1 - mTicks / CIRCLE_COUNT)).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
				PPCircle ring2 = new PPCircle(Particle.CRIT, standLocation.add(0, -0.1, 0), radius * (1 - mTicks / CIRCLE_COUNT)).ringMode(true).countPerMeter(0.6).delta(0).data(COLOR_GOLD);
				ring.spawnAsPlayerActive(activatedPlayer);
				ring2.spawnAsPlayerActive(activatedPlayer);

				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		World world = activatedPlayer.getWorld();
		world.playSound(standLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(standLocation, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.6f);
	}
}
