package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class DarkPunishmentCS extends DivineJusticeCS implements DepthsCS {
	//Darker divine justice. Depth set: shadow
	//Dark CLERIC!

	public static final String NAME = "Dark Punishment";

	private static final float HEAL_PITCH_SELF = 1.5f;
	private static final float HEAL_PITCH_OTHER = 1.75f;

	private static final Color TIP_COLOR = Color.fromRGB(40, 19, 102);
	private static final Color TIP_COLOR_TRANSITION = Color.fromRGB(63, 63, 63);

	private static final Color BASE_COLOR = Color.fromRGB(83, 60, 153);
	private static final Color BASE_COLOR_TRANSITION = Color.fromRGB(93, 89, 107);

	private static final double[] ANGLE = {305, 235, 275};
	private static final float[] TRIDENT_PITCH = {0.75f, 1f, 0.5f};
	private static final float[] SWEEP_PITCH = {0.75f, 0.9f, 0.55f};

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The light has betrayed you.",
			"So abandon your righteous ethics,",
			"and let the darkness consume you.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_SWORD;
	}

	@Override
	public @Nullable String getName() {
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
	public Material justiceAsh() {
		return Material.BLACK_DYE;
	}

	@Override
	public void justiceAshColor(Item item) {
		ScoreboardUtils.addEntityToTeam(item, "GlowingDarkPurple", NamedTextColor.DARK_PURPLE);
	}

	@Override
	public String justiceAshName() {
		return "Umbral Essence";
	}

	@Override
	public void justiceAshPickUp(Player player, Location loc) {
		player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 0.8f, 0.5f);
		player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.5f, 2f);
		player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
		player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1f, 0.5f);

		Location particleLocation = loc.add(0, 0.2, 0);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), player.getLocation().clone().add(0, 1, 0), player, particleLocation, null);
	}

	@Override
	public void justiceOnDamage(Player player, LivingEntity enemy, World world, Location enemyLoc, double widerWidthDelta, int combo) {
		world.playSound(enemyLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.65f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1, SWEEP_PITCH[combo]);
		world.playSound(enemyLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, TRIDENT_PITCH[combo]);
		world.playSound(enemyLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1, 0.75f);
		if (combo >= 2) {
			world.playSound(enemyLoc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
			world.playSound(enemyLoc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1, 0.8f);
		} else {
			world.playSound(enemyLoc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1, 0.5f);
		}

		PartialParticle partialParticle = new PartialParticle(
			Particle.DRAGON_BREATH,
			LocationUtils.getHalfHeightLocation(enemy), 15,
			0, 0, 0, 0.1
		).spawnAsPlayerActive(player);
		partialParticle.mParticle = Particle.SPELL_WITCH;
		partialParticle
			.spawnAsPlayerActive(player);

		Location loc = player.getLocation().add(0, 1, 0);
		ParticleUtils.drawHalfArc(loc, 2.1, ANGLE[combo], -20, 140, 7, 0.2,
			(Location l, int ring) -> {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
					new Particle.DustTransition(
						ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, ring / 7D),
						ParticleUtils.getTransition(BASE_COLOR_TRANSITION, TIP_COLOR_TRANSITION, ring / 7D),
						0.6f + (ring * 0.1f)
					))
					.spawnAsPlayerActive(player);
			});
	}

	@Override
	public void justiceKill(Player player, Location loc) {
		loc.add(0, 0.125, 0);
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.5f, 1.65f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.175)
			.spawnAsPlayerActive(player);

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
							)).spawnAsPlayerActive(player);
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
				SoundCategory.PLAYERS,
				0.5f,
				pitch
			);
		}
	}

	private void createOrb(Vector dir, Location loc, Player player, Location targetLoc, @Nullable Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = targetLoc;
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(player);

				new PartialParticle(Particle.DRAGON_BREATH,
					mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
						FastUtils.randomDoubleInRange(-0.05, 0.05),
						FastUtils.randomDoubleInRange(-0.05, 0.05)), 1,
					0, 0, 0, 0.01
				).spawnAsPlayerActive(player);

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.065;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					Color c = FastUtils.RANDOM.nextBoolean() ? BASE_COLOR : TIP_COLOR;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1.4f))
						.spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1f, 2f);
						new PartialParticle(Particle.SPELL_WITCH, mL, 10, 0f, 0f, 0f, 0.2F)
							.spawnAsPlayerActive(player);
						ParticleUtils.drawParticleCircleExplosion(player, player.getLocation().clone().add(0, 1, 0), 0, 1, 0, 0, 10, 0.25f,
							true, 0, 0, Particle.SQUID_INK);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
