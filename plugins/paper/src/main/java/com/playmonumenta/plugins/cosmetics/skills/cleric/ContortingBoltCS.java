package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
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

	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 220, 220), 1.2f);
	private static final Particle.DustOptions PINK = new Particle.DustOptions(Color.fromRGB(255, 200, 200), 1.2f);

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
		players.add(player);
		Vector dir = player.getEyeLocation().getDirection();
		createOrb(dir, LocationUtils.getHalfHeightLocation(player), player, players.get(0), PINK, Particle.CRIT, radius);
		world.playSound(userLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.2f, 0.9f);
		world.playSound(userLoc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 2f);
		world.playSound(userLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.2f, 2f);
		new BukkitRunnable() {
			int mInt = 0;
			@Override
			public void run() {
				if (mInt < players.size()) {
					createOrb(dir, LocationUtils.getHalfHeightLocation(players.get(mInt)), player, players.get(mInt + 1), PINK, Particle.CRIT, radius);
				} else {
					this.cancel();
				}
				mInt++;
			}
		}.runTaskTimer(Plugin.getInstance(), delay, delay);
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
		mobs.add(player);
		Vector dir = player.getEyeLocation().getDirection();
		createOrb(dir, LocationUtils.getHalfHeightLocation(player), player, mobs.get(0), CYAN, Particle.CRIT_MAGIC, radius);
		world.playSound(userLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.2f, 0.9f);
		world.playSound(userLoc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 2f);
		world.playSound(userLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.2f, 2f);
		new BukkitRunnable() {
			int mInt = 0;
			@Override
			public void run() {
				if (mInt < mobs.size() - 1) {
					createOrb(dir, LocationUtils.getHalfHeightLocation(mobs.get(mInt)), player, mobs.get(mInt + 1), CYAN, Particle.CRIT_MAGIC, radius);
				} else {
					this.cancel();
				}
				mInt++;
			}
		}.runTaskTimer(Plugin.getInstance(), delay, delay);
	}

	private void createOrb(Vector dir, Location loc, Player player, LivingEntity target, Particle.DustOptions color, Particle chainParticle, double radius) {
		World world = loc.getWorld();
		int mT = 0;
		double mArcCurve = 0;
		for (int i = 0; i < 10 * radius; i++) {
			mT++;
			Location to = LocationUtils.getHalfHeightLocation(target);

			mArcCurve += 0.005;
			dir.add(LocationUtils.getDirectionTo(to, loc).multiply(mArcCurve));

			if (dir.length() > 0.2) {
				dir.normalize().multiply(0.2);
			}

			loc.add(dir);
			new PartialParticle(Particle.REDSTONE, loc, 2, 0.1, 0.1, 0.1).data(color).spawnAsPlayerActive(player);
			new PartialParticle(chainParticle, loc.clone().add(0, 0.1, 0), 2, 0.1, 0.1, 0.1).spawnAsPlayerActive(player);

			if (mT > 5 && loc.distance(to) < 0.35) {
				if (target != player) {
					if (target instanceof Player) {
						new PartialParticle(Particle.HEART, target.getEyeLocation().add(0, -0.5, 0)).count(8).delta(0.4, 0.8, 0.4).spawnAsPlayerActive(player);
						world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.5f, 1.3f);
						world.playSound(target.getLocation(), Sound.BLOCK_SHROOMLIGHT_STEP, SoundCategory.PLAYERS, 1.6f, 1.2f);
						world.playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.6f, 0.9f);
					} else {
						new PartialParticle(Particle.BUBBLE_POP, to).count(12).delta(0.3, 0.6, 0.3).spawnAsPlayerActive(player);
						world.playSound(target.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.2f, 1.6f);
						world.playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.6f, 0.9f);
						world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.PLAYERS, 1.4f, 1.0f);
					}
				} else {
					new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0)).count(8).delta(0.6, 1.0, 0.6).spawnAsPlayerActive(player);
					world.playSound(player.getLocation().add(0, 1, 0), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 1.7f);
					world.playSound(player.getLocation().add(0, 1, 0), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.9f, 0.8f);
				}
				break;
			}
		}
	}
}
