package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BloodbathCS extends IlluminateCS {

	public static final String NAME = "Bloodbath";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Cursestone soaks the floors in blood.",
			"Soon you shall bathe in theirs too.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.REDSTONE_ORE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(160, 20, 20), 1.1f);
	private int mProjectileTransition = 0;

	@Override
	public void castEffects(Player player) {
		mProjectileTransition = 0;
		Location loc = player.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 0.85f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	@Override
	public void projectileEffects(Player player, Location loc) {
		Particle.DustOptions color = new Particle.DustOptions(Color.fromRGB(Math.min(255, 10 * mProjectileTransition), Math.max(0, 240 - 10 * mProjectileTransition), Math.max(0, 240 - 10 * mProjectileTransition)), 1.2f);
		Vector front = loc.getDirection().normalize().multiply(0.75);
		Vector right = VectorUtils.rotateTargetDirection(front, -90, -90);
		Vector left = VectorUtils.rotateTargetDirection(front, -90, 90);
		Vector swirl1 = VectorUtils.rotateTargetDirection(right, (24 * mProjectileTransition) % 360, 0);
		Vector swirl2 = VectorUtils.rotateTargetDirection(left, (24 * mProjectileTransition) % 360, 0);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(swirl1), 6, 0.1, 0.1, 0.1, 0.1, color).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(swirl2), 6, 0.1, 0.1, 0.1, 0.1, color).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIMSON_SPORE, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(player);
		mProjectileTransition++;
	}

	@Override
	public void trailEffects(Player player, Location location, double radius, Vector increment, int ticks, int maxTicks, @Nullable Location enhanceZone, double enhanceRadius) {
		Location loc = location.clone();

		if (enhanceZone != null && location.distance(enhanceZone) < enhanceRadius + 0.5) {
			return;
		}

		Vector vec = new Vector(radius, 0, 0);
		vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 90);
		vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

		if (ticks % 2 == 0) {
			double length = increment.length();
			PPLine line1a = new PPLine(Particle.REDSTONE, loc.clone().add(vec), increment, length).data(RED);
			PPLine line2a = new PPLine(Particle.REDSTONE, loc.clone().subtract(vec), increment, length).data(RED);
			line1a.delta(0.05).offset(Math.random()).count(1).minimumCount(0).spawnAsPlayerActive(player);
			line2a.delta(0.05).offset(Math.random()).count(1).minimumCount(0).spawnAsPlayerActive(player);
		}

		if ((ticks != 0 && ticks % 40 == 0) || ticks == maxTicks - 1) {
			Location spark1 = loc.clone().add(vec);
			new PartialParticle(Particle.SMALL_FLAME, spark1, 1, 0.35, 0).spawnAsPlayerActive(player);

			Location spark2 = loc.clone().subtract(vec);
			new PartialParticle(Particle.SMALL_FLAME, spark2, 1, 0.35, 0).spawnAsPlayerActive(player);

			PPCircle sparkfloor = new PPCircle(Particle.FALLING_DUST, loc, radius * 0.9).ringMode(false);
			sparkfloor.count((int) (radius * radius * 3)).delta(0.2).extra(0.1).data(Material.NETHER_WART_BLOCK.createBlockData()).spawnAsPlayerActive(player);
		}

		if (ticks == 0 && location.getBlock().getType().equals(Material.AIR)) {
			TemporaryBlockChangeManager.INSTANCE.changeBlock(location.getBlock(), Material.LIGHT, maxTicks);
		} else if (ticks == maxTicks - 1) {
			TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(location.getBlock(), Material.LIGHT);
		}
	}

	@Override
	public void projectileExplosionEffects(Player player, Location location) {
		World world = player.getWorld();
		Location loc = location.clone();

		if (loc.getBlock().isSolid()) {
			loc.add(0, 0.5, 0);
		}

		new PartialParticle(Particle.FALLING_DUST, loc, 80, 2.25, 2.25, 2.25, 0.5, Material.NETHER_WART_BLOCK.createBlockData()).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, loc, 18, 1.25, 1.5, 1.25, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 4, 2, 2, 2, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 50, 0.5f, 0.75f, 0.5f, 1.0).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.5f, 1.4f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.3f, 0.6f);
		world.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.4f, 2.0f);
		world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 1.6f, 0.9f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.9f, 0.6f);
		world.playSound(loc, Sound.ENTITY_TURTLE_EGG_BREAK, SoundCategory.PLAYERS, 0.8f, 0.7f);
	}

	@Override
	public void explosionHitEffects(Player player, LivingEntity damagee) {
		World world = player.getWorld();
		Location loc = damagee.getLocation().add(0, damagee.getEyeHeight() / 2, 0);
		world.playSound(loc, Sound.ENTITY_WARDEN_HURT, SoundCategory.PLAYERS, 0.2f, 1.0f);
	}

	@Override
	public void sanctifiedZoneEffects(Player player, Location location, double radius, int ticks, double maxDuration) {
		Location loc = location.clone();
		int mTries = 0;
		while (loc.getBlock().isSolid() && mTries < 10) {
			loc.add(0, 0.1, 0);
			mTries++;
		}
		if (mTries > 0) {
			loc.add(0, 0.2, 0);
		}

		loc.setDirection(new Vector(1, 0, 0));

		if (ticks == 0) {
			ItemDisplay display = loc.getWorld().spawn(loc.clone().add(0, 0.25, 0), ItemDisplay.class);
			display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
			display.setItemStack(DisplayEntityUtils.generateRPItem(Material.CYAN_DYE, "Insanity's Edge"));
			EntityUtils.setRemoveEntityOnUnload(display);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), display::remove, (int) maxDuration);
			// Hieroglyph for "Fight"
			Vector front = player.getEyeLocation().getDirection().setY(0).normalize().multiply(0.66 * radius);
			Vector left = VectorUtils.rotateTargetDirection(front, -90, 0);
			Vector right = VectorUtils.rotateTargetDirection(front, 90, 0);
			double[] d = VectorUtils.vectorToRotation(front);
			Location loc1 = loc.clone().add(front);
			Location loc2 = loc.clone().add(left);
			Location loc3 = loc.clone().add(right);
			for (int i = 0; i < 2; i++) {
				double delta = 0.2 * i;
				final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(160 - 40 * i, 0, 40 - 10 * i), 1.2f - i * 0.2f);
				new PPLine(Particle.REDSTONE, loc, loc2).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc2.clone().add(front), loc2.clone().subtract(front)).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc, loc1.clone().add(right.clone().multiply(0.5))).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc3, loc1.clone().add(right.clone().multiply(0.5))).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc3, loc3.clone().subtract(front)).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc.clone().subtract(front).add(left.clone().multiply(0.5)), loc3.clone().subtract(front)).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, loc.clone().subtract(front), 0.3 * radius).arcDegree(d[0], d[0] + 180).data(RED).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			}
			new PPCircle(Particle.ENCHANTMENT_TABLE, loc, radius).countPerMeter(12).extraRange(0.1, 0.2).innerRadiusFactor(1)
				.directionalMode(true).delta(-2, 1, -8).rotateDelta(true).spawnAsPlayerActive(player);
		}

		if (ticks >= 10) {
			new PartialParticle(Particle.CRIMSON_SPORE, loc, 1, 0.5 * radius, 0, 0.5 * radius, 0).spawnAsPlayerActive(player);
			if (ticks % 2 == 0 && ticks % 20 < 10) {
				double pulseRadius = radius / 10 * (ticks % 10 + 1);
				final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(240 - 16 * (ticks % 10), 20, 20), 1.0f);
				new PPCircle(Particle.REDSTONE, loc, pulseRadius).countPerMeter(10).delta(0.07).data(RED).spawnAsPlayerActive(player);
			}
		}
	}

	@Override
	public void enhanceTickDamageEffect(Player player, LivingEntity mob) {
		new PartialParticle(Particle.FLAME, mob.getLocation()).count(3).delta(0.35).extra(0.1).spawnAsPlayerActive(player);
	}
}
