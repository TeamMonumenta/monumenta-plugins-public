package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EarthquakeAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "earthquake";

	private static final Particle.DustOptions PERIODIC_DUST_OPTIONS = new Particle.DustOptions(Color.fromRGB(94, 52, 27), 1.5f);

	public EarthquakeAction() {
		super(IDENTIFIER);
		addParameter("radius", 5.0);
		addParameter("delay", 50);
		addParameter("damage", 30.0);
		addParameter("knockup_speed", 1.35);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		if (!AdvancementUtils.checkAdvancement(player, "monumenta:handbook/spawners_/earthquake_spawner")) {
			AdvancementUtils.grantAdvancement(player, "monumenta:handbook/spawners_/earthquake_spawner");
		}
		double radius = (double) getParameter(parameters, "radius");
		int delay = (int) getParameter(parameters, "delay");
		double damage = (double) getParameter(parameters, "damage");
		double knockupSpeed = (double) getParameter(parameters, "knockup_speed");

		Location spawnerLoc = BlockUtils.getCenteredBlockBaseLocation(spawner);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Hitbox hitbox = new Hitbox.SphereHitbox(spawnerLoc, radius);
			hitbox.getHitPlayers(true).forEach(p -> {
				DamageUtils.damage(null, p, DamageEvent.DamageType.BLAST, damage, null, true, false);
				p.setVelocity(p.getVelocity().add(new Vector(0, knockupSpeed, 0)));
			});
		}, delay);
		aesthetics(spawnerLoc, radius, delay);
	}

	private void aesthetics(Location spawnerLoc, double radius, int delay) {
		// Once every second until explosion
		new BukkitRunnable() {
			final int mMaxTimes = delay / 20;
			int mTimes = 0;

			@Override
			public void run() {
				if (mTimes >= mMaxTimes) {
					cancel();
					return;
				}
				spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 0.5f);
				spawnerLoc.getWorld().playSound(spawnerLoc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1.5f, 0.5f);
				new PPCircle(Particle.REDSTONE, spawnerLoc, radius).countPerMeter(3).data(PERIODIC_DUST_OPTIONS)
					.ringMode(false).spawnAsEnemy();
				mTimes++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20);

		// Explosion Aesthetics
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 0.5f);
			spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.35f);
			spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.5f);
			new PartialParticle(Particle.CLOUD, spawnerLoc, 100).extra(0.5).spawnAsEnemy();
			new PartialParticle(Particle.LAVA, spawnerLoc, 35).delta(2, 1, 2).spawnAsEnemy();
			new PartialParticle(Particle.BLOCK_CRACK, spawnerLoc, 100).delta(2, 1, 2)
				.data(Material.DIRT.createBlockData()).spawnAsEnemy();
			new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, spawnerLoc, 35).delta(2, 1, 2)
				.extra(0.1).spawnAsEnemy();
		}, delay);
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PartialParticle(Particle.REDSTONE, blockLoc, 15).delta(0.25).data(PERIODIC_DUST_OPTIONS)
			.spawnAsEnemy();
	}
}
