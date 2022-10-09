package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VampiricDrainCS extends SoulRendCS implements GalleryCS {
	// Gallery theme1: blood

	public static final String NAME = "Vampiric Drain";

	private static final Particle.DustOptions BLOODY_COLOR1 = new Particle.DustOptions(Color.fromRGB(141, 23, 16), 0.8f);
	private static final Particle.DustOptions BLOODY_COLOR2 = new Particle.DustOptions(Color.fromRGB(212, 30, 21), 1.0f);
	private static final BlockData BLOOD_BLOCK = Bukkit.createBlockData(Material.REDSTONE_BLOCK);
	private static final int RING_FRAMES = 7;

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"\"Vampires never use the tricks of human.\"",
			"Compared to souls, blood will always offer",
			"a far more effective path."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SOUL_REND;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GHAST_TEAR;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player mPlayer) {
		return mPlayer != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public void rendHitSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.5f, 0.55f);
		world.playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.35f, 0.65f);

		new BukkitRunnable() {
			int mTimes = 3;
			@Override
			public void run() {
				if (mTimes-- <= 0) {
					this.cancel();
				}
				world.playSound(loc, Sound.ENTITY_BAT_AMBIENT, SoundCategory.PLAYERS, 0.1f + mTimes * 0.05f, 0.5f + FastUtils.RANDOM.nextFloat() * 0.25f);
				world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.PLAYERS, 0.25f + mTimes * 0.05f, 0.5f + FastUtils.RANDOM.nextFloat() * 0.25f);
				world.playSound(loc, Sound.ENTITY_BAT_LOOP, SoundCategory.PLAYERS, 0.35f + mTimes * 0.1f, 0.5f + FastUtils.RANDOM.nextFloat() * 0.25f);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 4);
	}

	@Override
	public void rendHitParticle1(Player mPlayer, Location loc) {
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 20, 0.75, 0.5, 0.75, 0.0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 1, 0), 20, 0.75, 0.5, 0.75, 0.0, BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 4, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);

		new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.25, 0), 1.25).count(16).ringMode(true).data(BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void rendHitParticle2(Player mPlayer, Location loc, double radius) {
		rendHitParticle1(mPlayer, loc);

		PPCircle ring1 = new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.25, 0), 1.25).ringMode(true).data(BLOODY_COLOR1);
		PPCircle ring2 = new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.55, 0), 1.25).ringMode(true).data(BLOODY_COLOR1);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks++ >= RING_FRAMES) {
					this.cancel();
				}
				double mRadius = FastUtils.sin(0.5 * 3.1416 * mTicks / RING_FRAMES) * radius;
				ring1.radius(mRadius).count((int) Math.ceil(mRadius * 4)).spawnAsPlayerActive(mPlayer);
				ring2.radius(mRadius * 0.8).count((int) Math.ceil(mRadius * 3.2)).spawnAsPlayerActive(mPlayer);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		for (int i = 0; i < 13; i++) {
			double r = Math.sqrt(FastUtils.RANDOM.nextDouble()) * radius * 0.8;
			double theta = FastUtils.RANDOM.nextDouble() * 3.1416 * 2;
			double h = FastUtils.RANDOM.nextDouble() + 0.25;

			double dTheta = (FastUtils.RANDOM.nextDouble() + 0.5) * 3.1416 / 45;

			new BukkitRunnable() {
				@Override
				public void run() {
					ParticleUtils.drawCurve(loc, 0, 1,
						new Vector(0, 0, r),
						new Vector(r, 0, 0),
						new Vector(0, 1, 0),
						t -> FastUtils.cos(theta + t * dTheta),
						t -> FastUtils.sin(theta + t * dTheta),
						t -> h + 0.5 * t,
						(l, t) -> new PartialParticle(Particle.DAMAGE_INDICATOR, l, 4, 0.75, 0.5, 0.75, 0.6).spawnAsPlayerActive(mPlayer)
					);
				}
			}.runTaskLater(Plugin.getInstance(), (int) Math.round(r / radius * RING_FRAMES));
		}
	}

	@Override
	public void rendHealEffect(Player mPlayer, Player healed, LivingEntity enemy) {
		Location loc1 = enemy.getLocation().add(0, enemy.getHeight() / 1.6, 0);
		Location loc2 = healed.getLocation().add(0, 0.6, 0);
		Vector mFront = loc2.clone().subtract(loc1).toVector();
		double distance = mFront.length();
		new PartialParticle(Particle.REDSTONE, loc2, 15, 0.5, 0.7, 0.5, 0.1, BLOODY_COLOR2);

		ParticleUtils.drawCurve(loc1, 0, (int) Math.ceil(distance * 3), mFront.clone().normalize(),
			t -> t * 0.34,
			t -> FastUtils.cos(t * 3.1416 / 5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			t -> FastUtils.sin(t * 3.1416 / 5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			(loc, t) -> {
				new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			}
		);
		ParticleUtils.drawCurve(loc1, 0, (int) Math.ceil(distance * 3), mFront.clone().normalize(),
			t -> t * 0.34,
			t -> FastUtils.cos(t * 3.1416 / 5 - 3.1416 / 1.5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			t -> FastUtils.sin(t * 3.1416 / 5 - 3.1416 / 1.5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			(loc, t) -> {
				new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			}
		);
		ParticleUtils.drawCurve(loc1, 0, (int) Math.ceil(distance * 3), mFront.clone().normalize(),
			t -> t * 0.34,
			t -> FastUtils.cos(t * 3.1416 / 5 + 3.1416 / 1.5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			t -> FastUtils.sin(t * 3.1416 / 5 + 3.1416 / 1.5) * (1.25 - FastUtils.sin(t * 3.1416)) * 0.3,
			(loc, t) -> {
				new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			}
		);

	}

	@Override
	public void rendAbsorptionEffect(Player mPlayer, Player healed, LivingEntity enemy) {
		new PartialParticle(Particle.HEART, healed.getLocation().add(0, 1, 0), 5, 0.4, 0.6, 0.4, 0.0).spawnAsPlayerActive(mPlayer);
	}
}
