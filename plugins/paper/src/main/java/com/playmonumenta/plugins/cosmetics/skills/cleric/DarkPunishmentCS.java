package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
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


public class DarkPunishmentCS extends DivineJusticeCS implements DepthsCS {
	//Darker divine justice. Depth set: shadow
	//Dark CLERIC!

	public static final String NAME = "Dark Punishment";

	private final float HEAL_PITCH_SELF = 1.5f;
	private final float HEAL_PITCH_OTHER = 1.75f;

	private static final Color TIP_COLOR = Color.fromRGB(40, 19, 102);
	private static final Color TIP_COLOR_TRANSITION = Color.fromRGB(63, 63, 63);

	private static final Color BASE_COLOR = Color.fromRGB(83, 60, 153);
	private static final Color BASE_COLOR_TRANSITION = Color.fromRGB(93, 89, 107);

	private static final double[] ANGLE = {305, 235, 275};
	private static final float[] TRIDENT_PITCH = {0.75f, 1f, 0.5f};
	private static final float[] SWEEP_PITCH = {0.75f, 0.9f, 0.55f};

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"The light has betrayed you.",
			"So abandon your righteous ethics,",
			"and let the darkness consume you.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DIVINE_JUSTICE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_SWORD;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_SHADOW;
	}

	@Override
	public float getHealPitchSelf() {
		return HEAL_PITCH_SELF;
	}

	@Override
	public float getHealPitchOther() {
		return HEAL_PITCH_OTHER;
	}

	@Override
	public void justiceOnDamage(Player mPlayer, LivingEntity enemy, double widerWidthDelta, int combo) {
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.65f);
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1, SWEEP_PITCH[combo]);
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, TRIDENT_PITCH[combo]);
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1, 0.75f);
		if (combo >= 2) {
			mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
			mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1, 0.8f);
		} else {
			mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1, 0.5f);
		}

		PartialParticle partialParticle = new PartialParticle(
			Particle.DRAGON_BREATH,
			LocationUtils.getHalfHeightLocation(enemy), 15,
			0, 0, 0, 0.1
		).spawnAsPlayerActive(mPlayer);
		partialParticle.mParticle = Particle.SPELL_WITCH;
		partialParticle
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		ParticleUtils.drawHalfArc(loc, 2.1, ANGLE[combo], -20, 140, 7, 0.2,
			(Location l, int ring) -> {
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
				new Particle.DustTransition(
					ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, ring / 7D),
					ParticleUtils.getTransition(BASE_COLOR_TRANSITION, TIP_COLOR_TRANSITION, ring / 7D),
					0.6f + (ring * 0.1f)
				))
				.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		});
	}

	@Override
	public void justiceKill(Player mPlayer, Location loc) {
		loc.add(0, 0.125, 0);
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.5f, 1.65f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.175)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {

			double mRadius = 0;
			final Location mL = loc.clone();
			final double RADIUS = 5;
			@Override
			public void run() {

				for (int i = 0; i < 2; i++) {
					mRadius += 0.33;
					for (int degree = 0; degree < 360; degree += 5) {
						double radian = FastMath.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
							FastUtils.sin(radian) * mRadius);
						Location loc = mL.clone().add(vec);
						new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0,
							new Particle.DustTransition(
								ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, mRadius / RADIUS),
								ParticleUtils.getTransition(BASE_COLOR_TRANSITION, TIP_COLOR_TRANSITION, mRadius / RADIUS),
								0.65f
							)).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					}
				}

				if (mRadius >= RADIUS) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void justiceHealSound(List<Player> players, float pitch) {
			for (Player healedPlayer : players) {
				healedPlayer.playSound(
					healedPlayer.getLocation(),
					Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
					0.5f,
					pitch
				);
			}
	}
}
