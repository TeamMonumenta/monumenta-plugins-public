package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EverseeingEyeCS extends EagleEyeCS implements GalleryCS {

	public static final String NAME = "Everseeing Eye";

	private static final Particle.DustOptions INNER = new Particle.DustOptions(Color.fromRGB(8, 0, 1), 1.75f);
	private static final Particle.DustOptions MIDDLE = new Particle.DustOptions(Color.fromRGB(25, 25, 76), 1.5f);
	private static final Particle.DustOptions OUTER = new Particle.DustOptions(Color.fromRGB(255, 242, 248), 1.25f);
	private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(71, 0, 2), 1.2f);

	private static double EYE_ANIM_RADIUS = 2.4;
	private static int EYE_ANIM_UNITS = 16;
	private static int EYE_ANIM_FRAMES = 4;

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"The dream never ends. When its quivering eye",
			"gazes upon the land, the beast will dictate",
			"the realms of this dream. It will be endless."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.EAGLE_EYE;
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
	public boolean isUnlocked(Player mPlayer) {
		return ScoreboardUtils.getScoreboardValue(mPlayer, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			|| mPlayer.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void eyeStart(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.6f, 0.6f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.PLAYERS, 1.25f, 2f);
		world.playSound(mPlayer.getLocation(), Sound.AMBIENT_WARPED_FOREST_MOOD, SoundCategory.PLAYERS, 0.75f, 2f);

		new BukkitRunnable() {
			int mFrame = 0;
			final Vector mFront = VectorUtils.rotateTargetDirection(
				VectorUtils.rotationToVector(mPlayer.getLocation().getYaw(), mPlayer.getLocation().getPitch()), 0, 24);
			final Vector mOffset = VectorUtils.rotateTargetDirection(mFront.clone(), 0, -90).multiply(3);
			final Location mCenter = mPlayer.getEyeLocation().clone().add(mFront.clone().add(mOffset));

			@Override
			public void run() {
				if (mFrame++ >= EYE_ANIM_FRAMES) {
					this.cancel();
				}
				// Bloody outline
				ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
					t -> 0,
						t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.6, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOOD).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
					t -> 0,
						t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.6, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS,
						(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOOD).spawnAsPlayerActive(mPlayer)
				);
				if (mFrame > 0) {
					// White part, outer
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.45, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.8,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 3, 0, 0, 0, 0, OUTER).spawnAsPlayerActive(mPlayer)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.45, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.8,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 3, 0, 0, 0, 0, OUTER).spawnAsPlayerActive(mPlayer)
					);
				}
				if (mFrame > 1) {
					// Blue part, middle
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.3, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.55,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(mPlayer)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.3, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.55,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(mPlayer)
					);

					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.2, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.35,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(mPlayer)
					);
					ParticleUtils.drawCurve(mCenter, 0, EYE_ANIM_UNITS, mFront,
						t -> 0,
							t -> -FastUtils.sin(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * mFrame / EYE_ANIM_FRAMES * 0.2, t -> FastUtils.cos(t * Math.PI / EYE_ANIM_UNITS) * EYE_ANIM_RADIUS * 0.35,
							(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, MIDDLE).spawnAsPlayerActive(mPlayer)
					);
					// Black part, inner
					new PartialParticle(Particle.REDSTONE, mCenter, 15, 0.2, 0.2, 0.2, 0.1, INNER).spawnAsPlayerActive(mPlayer);
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 2);

	}

	@Override
	public void eyeOnTarget(World world, Player mPlayer, LivingEntity mob) {
		world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.15f, 0.6f);
		world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.2f, 1.5f);
		new PartialParticle(Particle.REDSTONE, mob.getLocation().add(0, 1, 0), 25, 0.7, 0.7, 0.7, 0.05, BLOOD).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIMSON_SPORE, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.005).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void eyeFirstStrike(World world, Player mPlayer, LivingEntity mob) {
		Vector offset = new Vector(0, 0.25, 2.5);
		Location loc = mob.getLocation();
		Location loc1 = loc.clone().add(VectorUtils.rotateYAxis(offset, mPlayer.getLocation().getYaw() + 45));
		Location loc2 = loc.clone().add(VectorUtils.rotateYAxis(offset, mPlayer.getLocation().getYaw() + 135));
		Location loc3 = loc.clone().add(VectorUtils.rotateYAxis(offset, mPlayer.getLocation().getYaw() + 225));
		Location loc4 = loc.clone().add(VectorUtils.rotateYAxis(offset, mPlayer.getLocation().getYaw() + 315));
		new PPLine(Particle.REDSTONE, loc1, loc3).shiftStart(0.25).countPerMeter(10).delta(0.125).extra(0.05).data(BLOOD).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc2, loc4).shiftStart(0.25).countPerMeter(10).delta(0.125).extra(0.05).data(BLOOD).spawnAsPlayerActive(mPlayer);
		world.playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.25f, 0.5f);
	}

	@Override
	public Team createTeams() {
		return ScoreboardUtils.getExistingTeamOrCreate("everseeingEyeColor", NamedTextColor.DARK_RED);
	}
}
