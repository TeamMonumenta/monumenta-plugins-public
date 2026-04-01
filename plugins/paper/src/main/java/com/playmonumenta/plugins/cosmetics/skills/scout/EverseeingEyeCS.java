package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EverseeingEyeCS extends PartingShotCS implements GalleryCS {

	// TODO: Could be improved

	public static final String NAME = "Everseeing Eye";

	private static final Particle.DustOptions INNER = new Particle.DustOptions(Color.fromRGB(8, 0, 1), 1.75f);
	private static final Particle.DustOptions MIDDLE = new Particle.DustOptions(Color.fromRGB(25, 25, 76), 1.5f);
	private static final Particle.DustOptions OUTER = new Particle.DustOptions(Color.fromRGB(255, 242, 248), 1.25f);
	private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(71, 0, 2), 1.2f);

	private static final double EYE_ANIM_RADIUS = 2.4;
	private static final int EYE_ANIM_UNITS = 16;
	private static final int EYE_ANIM_FRAMES = 4;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The dream never ends. When its quivering eye",
			"gazes upon the land, the beast will dictate",
			"the realms of this dream. It will be endless."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CHORUS_FLOWER;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			|| player.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void dodge(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(loc, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.PLAYERS, 4f, 2f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.4f, 0.8f);

		new PartialParticle(Particle.DAMAGE_INDICATOR, player.getEyeLocation())
			.count(10)
			.delta(0.2, 0.2, 0.2)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getHalfHeightLocation(player))
			.count(40)
			.data(Material.REDSTONE_BLOCK.createBlockData())
			.delta(0.2, 0.5, 0.2)
			.spawnAsPlayerActive(player);

		// Eye Animation
		new BukkitRunnable() {
			int mFrame = 0;
			final Vector mFront = VectorUtils.rotateTargetDirection(
				VectorUtils.rotationToVector(loc.getYaw(), 0), 0, 0);

			final Location mCenter = player.getEyeLocation().clone().add(0, 3, 0);

			@Override
			public void run() {
				if (mFrame++ >= EYE_ANIM_FRAMES) {
					this.cancel();
				}
				// Bloody outline
				ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
					t -> 0,
					t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.6, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS,
					(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOOD).spawnAsPlayerActive(player)
				);
				ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
					t -> 0,
					t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.6, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS,
					(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOOD).spawnAsPlayerActive(player)
				);
				if (mFrame > 0) {
					// White part, outer
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.45, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.8,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 3, 0, 0, 0, 0, OUTER).spawnAsPlayerActive(player)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.45, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.8,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 3, 0, 0, 0, 0, OUTER).spawnAsPlayerActive(player)
					);
				}
				if (mFrame > 1) {
					// Blue part, middle
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.3, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.55,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(player)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.3, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.55,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(player)
					);

					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.2, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.35,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(player)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
						t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.2, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.35,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(player)
					);
					// Black part, inner
					new PartialParticle(Particle.REDSTONE, mCenter, 15, 0.2, 0.2, 0.2, 0.1, INNER).spawnAsPlayerActive(player);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	@Override
	public void tickEffect(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(player))
			.count(5)
			.delta(0.2, 0.5, 0.2)
			.data(BLOOD)
			.extra(0.01f)
			.spawnAsPlayerBuff(player);
	}

	@Override
	public void shoot(World world, Player player, Location loc, Projectile proj) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1f, 1.6f);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (!player.isValid() || player.isDead() || !proj.isValid() || ++mT > 100) {
					this.cancel();
					return;
				}

				new PartialParticle(Particle.BLOCK_CRACK, proj.getLocation())
					.minimumCount(1)
					.data(Material.REDSTONE_BLOCK.createBlockData())
					.count(2)
					.spawnAsPlayerActive(player);

				new PartialParticle(Particle.REDSTONE, proj.getLocation())
					.minimumCount(1)
					.data(BLOOD)
					.count(2)
					.spawnAsPlayerActive(player);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void land(World world, Player player, Location loc, double radius) {
		loc.add(0, 0.15, 0);

		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1f, 1.6f);
		world.playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 1f, 0.4f);

		new PPCircle(Particle.BLOCK_CRACK, loc, radius)
			.ringMode(false)
			.data(Material.REDSTONE_BLOCK.createBlockData())
			.count(125)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.REDSTONE, loc, radius)
			.ringMode(false)
			.data(BLOOD)
			.count(125)
			.spawnAsPlayerActive(player);

	}

	@Override
	public void expire(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1f, 1.6f);
	}

	@Override
	public String getDummyName() {
		return "OcularClone";
	}

	@Override
	public void revealStart(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.AMBIENT_WARPED_FOREST_ADDITIONS, SoundCategory.HOSTILE, 1.5f, 2f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_HORSE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.PLAYERS, 1.25f, 2f);
		world.playSound(loc, Sound.AMBIENT_WARPED_FOREST_MOOD, SoundCategory.PLAYERS, 3f, 2f, 3);

		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 5, 0.85,
			l -> {
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 2, 0.15, 0.15, 0.15)
					.extra(0.01)
					.spawnAsPlayerActive(player);

				new PartialParticle(Particle.REDSTONE, loc, 2, 0.15, 0.15, 0.15)
					.extra(0.01)
					.data(BLOOD)
					.spawnAsPlayerActive(player);
			});

		HashSet<Item> itemSet = new HashSet<>();

		for (int i = 0; i < 5; i++) {
			Location newLoc = loc.clone();
			Vector random = VectorUtils.randomHorizontalUnitVector();
			random.setY(FastUtils.randomDoubleInRange(0.4, 1));
			newLoc.setDirection(random);

			itemSet.add(AbilityUtils.spawnAbilityItem(world, newLoc, Material.NETHER_WART_BLOCK, "meat",
				false, 0.6f, false, true));
		}

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (itemSet.isEmpty()) {
					this.cancel();
					return;
				}

				if (mT > 100) {
					itemSet.forEach(Item::remove);
					itemSet.clear();
				}

				Iterator<Item> it = itemSet.iterator();

				while (it.hasNext()) {
					Item item = it.next();

					Location loc = item.getLocation();
					loc.add(0, 0.2, 0);
					loc.add(item.getVelocity());

					new PartialParticle(Particle.REDSTONE, loc)
						.delta(0.1)
						.count(5)
						.data(BLOOD)
						.spawnAsPlayerActive(player);

					if (item.isOnGround()) {
						world.playSound(loc, Sound.BLOCK_SNIFFER_EGG_CRACK, 0.8f, 1f);
						world.playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 1.0f, 0.8f);
						world.playSound(loc, Sound.ENTITY_SPIDER_STEP, SoundCategory.PLAYERS, 0.85f, 0.8f);

						item.remove();
						it.remove();
					}
				}

				mT++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);


	}

	@Override
	public void revealOnTarget(World world, Player player, LivingEntity mob) {
		world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.15f, 0.6f);
		world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.2f, 1.5f);
		new PartialParticle(Particle.REDSTONE, mob.getLocation().add(0, 1, 0), 25, 0.7, 0.7, 0.7, 0.05, BLOOD).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIMSON_SPORE, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.005).spawnAsPlayerActive(player);
	}
}
