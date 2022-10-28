package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrestigiousBondsCS extends VoodooBondsCS implements PrestigeCS {

	public static final String NAME = "Prestigious Bonds";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(224, 208, 80), 1.25f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"BONDS_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VOODOO_BONDS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOW_BERRIES;
	}

	@Override
	public String getName() {
		return NAME;
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
	public int getPrice() {
		return 1;
	}

	@Override
	public void bondsStartEffect(World world, Player mPlayer, double maxRadius) {
		final Location mCenter = mPlayer.getLocation().clone().add(0, 0.125, 0);
		world.playSound(mCenter, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.35f, 0.5f);
		world.playSound(mCenter, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 1.25f, 0.5f);
		world.playSound(mCenter, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 0.8f, 0.8f);
		world.playSound(mCenter, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1f, 0.6f);

		new BukkitRunnable() {
			PPCircle mRing = new PPCircle(Particle.REDSTONE, mCenter, 0).ringMode(true).data(LIGHT_COLOR);
			int mFrame = 0;
			double mRadius = 0.2;
			final Vector mFront = mPlayer.getLocation().getDirection().clone().setY(0).normalize().multiply(maxRadius);
			final int FRAMES = 20;
			final double D_RADIUS = (1 - mRadius) / FRAMES;
			final double DENSITY = 2.1;
			final double WIDTH = 0.7071;
			final int MAX_UNITS = (int) Math.ceil(DENSITY * maxRadius);

			@Override
			public void run() {
				if (++mFrame > FRAMES) {
					this.cancel();
					return;
				}

				final double r = maxRadius * (mRadius + D_RADIUS * mFrame);
				mRing.radius(r).count((int) Math.ceil(r * 16)).spawnAsPlayerActive(mPlayer);

				final int units = (int) Math.ceil(DENSITY * maxRadius * mFrame / FRAMES);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> WIDTH,
					t -> WIDTH * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> -WIDTH,
					t -> WIDTH * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> WIDTH * t / MAX_UNITS,
					t -> WIDTH,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> WIDTH * t / MAX_UNITS,
					t -> -WIDTH,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> 0.5 + 0.5 * t / MAX_UNITS,
					t -> 0.5 - 0.5 * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> -0.5 - 0.5 * t / MAX_UNITS,
					t -> 0.5 - 0.5 * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> -0.5 - 0.5 * t / MAX_UNITS,
					t -> -0.5 + 0.5 * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, -units, units, mFront,
					t -> 0.5 + 0.5 * t / MAX_UNITS,
					t -> -0.5 + 0.5 * t / MAX_UNITS,
					t -> 0,
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
				);

			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void bondsApplyEffect(Player mPlayer, Player p) {
		p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 0.65f);
		p.playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 2f, 0.6f);
		new PartialParticle(Particle.SPELL_INSTANT, p.getLocation(), 40, 0.3, 0, 0.3, 0.02).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 0.8, 0), p.getLocation().clone().add(0, 0.8, 0))
			.data(GOLD_COLOR).countPerMeter(2.5).delta(0.25).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void bondsSpreadParticle(Player mPlayer, Location mLoc, Location eLoc) {
		new PartialParticle(Particle.REDSTONE, mLoc, 30, 0.4, 0.7, 0.4, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mLoc, 40, 0.5, 0.5, 0.5, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		Vector mFront = mLoc.toVector().subtract(eLoc.toVector());
		ParticleUtils.drawCurve(eLoc.clone().add(0, 0.75, 0), 1, 36, mFront,
			t -> 0.5 + 0.5 * FastUtils.sinDeg(t * 10),
			t -> 0.125 * FastUtils.cosDeg(t * 10),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
	}
}
