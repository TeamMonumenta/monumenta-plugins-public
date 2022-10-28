package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
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

public class PrestigiousRondeCS extends DeadlyRondeCS implements PrestigeCS {

	public static final String NAME = "Prestigious Ronde";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private static final Particle.DustOptions GOLD_TINY = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 0.65f);
	private static final Particle.DustOptions LIGHT_TINY = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 0.65f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"RONDE_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DEADLY_RONDE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_13;
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
	public void rondeHitEffect(World world, Player mPlayer, double radius, double rondeBaseRadius, boolean lv2) {
		Vector mFront = mPlayer.getEyeLocation().getDirection();
		Location particleLoc = mPlayer.getEyeLocation().add(mFront.multiply(0.6 * radius));
		double multiplier = radius / rondeBaseRadius;
		double delta = 1.25 * multiplier;
		new PartialParticle(Particle.SWEEP_ATTACK, particleLoc, (int) (5 * multiplier), delta, 0.5, delta).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, particleLoc, (int) (30 * multiplier), delta, 0.5, delta, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, particleLoc, (int) (15 * multiplier), delta, 0.5, delta, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL, particleLoc, (int) (20 * multiplier), delta, 0.5, delta, 0.15).spawnAsPlayerActive(mPlayer);

		slash(world, mPlayer, particleLoc, radius, GOLD_COLOR, GOLD_TINY);
		new BukkitRunnable() {
			@Override
			public void run() {
				slash(world, mPlayer, particleLoc, radius, LIGHT_COLOR, LIGHT_TINY);
			}
		}.runTaskLater(Plugin.getInstance(), 3);
		if (lv2) {
			new BukkitRunnable() {
				@Override
				public void run() {
					slash(world, mPlayer, particleLoc, radius, GOLD_COLOR, LIGHT_TINY);
				}
			}.runTaskLater(Plugin.getInstance(), 5);
		}
	}

	private void slash(World world, Player mPlayer, Location pLoc, double radius, Particle.DustOptions color1, Particle.DustOptions color2) {
		double r = 0.05 * radius * Math.min(FastUtils.RANDOM.nextDouble() * FastUtils.RANDOM.nextDouble(), 0.75);
		double theta = FastUtils.RANDOM.nextDouble() * 2 * 3.1416;
		double dF = 0.048 * radius + FastUtils.RANDOM.nextDouble() * 0.032 * radius;
		double dR = r * FastUtils.cos(theta);
		double dU = r * FastUtils.sin(theta);
		double dX = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		double dY = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		double dZ = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		Location mCenter = pLoc.clone().add(dX, dY, dZ);

		new BukkitRunnable() {
			int mTick = 0;
			final int mUnits = 4;

			@Override
			public void run() {
				ParticleUtils.drawCurve(mCenter, 0, mUnits - 1, mPlayer.getLocation().getDirection(),
					t -> dF * (t + mUnits * (mTick - 0.3 * radius)),
					t -> dR * (t + mUnits * (mTick - 0.3 * radius)),
					t -> dU * (t + mUnits * (mTick - 0.3 * radius)),
					(l, t) -> {
						new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0.05, 0.05, 0, color1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.REDSTONE, l, 4, 0.25, 0.25, 0.25, 0, color2).spawnAsPlayerActive(mPlayer);
					});

				if (++mTick > radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		world.playSound(pLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.65f, 1.6f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.1f, 0.7f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.95f, 0.6f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.85f, 0.8f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.75f, 0.75f);
	}

	@Override
	public void rondeGainStackEffect(Player mPlayer) {
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.35f, 0.7f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.1f, 1.6f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 4f, 0.8f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.9f, 1.7f);
	}

	@Override
	public void rondeTickEffect(Player mPlayer, int charges, int mTicks) {
		for (int i = 0; i < charges; i++) {
			double angle = 2 * 3.1416 * i / charges + 0.28 * 3.1416 * mTicks / (charges + 1);
			double height = 0.86 + 0.17 * FastUtils.sin(0.067 * 3.1416 * mTicks + charges * 3.1416);
			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(FastUtils.cos(angle), height, -FastUtils.sin(angle)),
				(i + mTicks) % 3, 0.05, 0.45, 0.05, i % 2 == 0 ? GOLD_COLOR : LIGHT_COLOR).spawnAsPlayerBuff(mPlayer);
		}
	}
}
