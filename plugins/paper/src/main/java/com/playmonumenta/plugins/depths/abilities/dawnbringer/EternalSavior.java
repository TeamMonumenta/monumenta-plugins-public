package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static org.bukkit.Material.LIGHT;

public class EternalSavior extends DepthsAbility {
	public static final String ABILITY_NAME = "Eternal Savior";
	public static final int COOLDOWN = 120 * 20;
	private static final double TRIGGER_HEALTH = 0.2;
	public static final double[] HEAL_PER_SECOND = {0.12, 0.15, 0.18, 0.21, 0.24, 0.30};
	public static final int RADIUS = 5;
	public static final int STASIS_DURATION = 5 * 20;
	public static final double[] ABSORPTION = {4, 5, 6, 7, 8, 10};
	public static final int ABSORPTION_DURATION = 5 * 20;
	public static final int STUN_DURATION = 20;

	public static final String CHARM_COOLDOWN = "Eternal Savior Cooldown";

	private static final Color HEAL_BEAM_COLOR = Color.fromRGB(255, 180, 0);
	private static final Color HEAL_BEAM_COLOR_LIGHT = Color.fromRGB(255, 225, 75);

	public static final DepthsAbilityInfo<EternalSavior> INFO =
		new DepthsAbilityInfo<>(EternalSavior.class, ABILITY_NAME, EternalSavior::new, DepthsTree.DAWNBRINGER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.ETERNAL_SAVIOR)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.YELLOW_GLAZED_TERRACOTTA)
			.descriptions(EternalSavior::getDescription)
			.priorityAmount(10000);

	private Location mStasisLocation;
	private final int mStasisDuration;
	private final double mHeal;
	private final double mRadius;
	private final double mAbsorption;
	private final int mAbsorptionDuration;
	private final int mStunDuration;
	private boolean mStasisActive;

	public EternalSavior(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mStasisDuration = STASIS_DURATION;
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.ETERNAL_SAVIOR_RADIUS.mEffectName, RADIUS);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ETERNAL_SAVIOR_HEALING.mEffectName, HEAL_PER_SECOND[mRarity - 1]);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ETERNAL_SAVIOR_ABSORPTION.mEffectName, ABSORPTION[mRarity - 1]);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CharmEffects.ETERNAL_SAVIOR_ABSORPTION_DURATION.mEffectName, ABSORPTION_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.ETERNAL_SAVIOR_STUN_DURATION.mEffectName, STUN_DURATION);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		if (healthRemaining > maxHealth * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		mPlugin.mEffectManager.addEffect(mPlayer, "EternalSaviorStasis", new Stasis(mStasisDuration));

		Location loc = mPlayer.getLocation();
		new PartialParticle(Particle.TOTEM, LocationUtils.getHalfHeightLocation(mPlayer), 50, 0.2, 0.2, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.clone().add(0, 0.5, 0), 0, 0.01, -loc.getYaw(), -loc.getPitch(), 25, 0.65f, true, 0, 0, Particle.END_ROD);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.clone().add(0, 0.5, 0), 0, 0.01, -loc.getYaw(), -loc.getPitch(), 50, 0.37f, true, 0, 0, Particle.END_ROD);
		if (loc.getBlock().getType().equals(Material.AIR)) {
			TemporaryBlockChangeManager.INSTANCE.changeBlock(loc.getBlock(), LIGHT, mStasisDuration);
		}


		mStasisActive = true;
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mStasisLocation = mPlayer.getLocation().clone();
				Location loc = mPlayer.getLocation();
				World world = mPlayer.getWorld();

				if (mTicks % 2 == 0) {
					new PartialParticle(Particle.SPELL_INSTANT, LocationUtils.getHalfHeightLocation(mPlayer), 3, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mPlayer), 2, 0.3, 0.3, 0.3,
						new Particle.DustOptions(HEAL_BEAM_COLOR, 1f)).spawnAsPlayerActive(mPlayer);
				}

				if (mTicks <= 8) {
					new PPCircle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), mTicks * mRadius / 8)
						.data(new Particle.DustOptions(HEAL_BEAM_COLOR, 1f))
						.countPerMeter(2)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
				} else if (mTicks % 2 == 0) {
					new PPCircle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), mRadius)
						.data(new Particle.DustOptions(HEAL_BEAM_COLOR, 1.25f))
						.countPerMeter(0.75)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.5, 0), mRadius)
						.data(new Particle.DustOptions(HEAL_BEAM_COLOR_LIGHT, 1.25f))
						.countPerMeter(0.5)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);

					new PPCircle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 0.75, 0), mRadius)
						.count(5)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
				}

				switch (mTicks) {
					case 0 -> {
						world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.5f, 1.33f);
						world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.6f);
						world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1f, 1.7f);
						world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 0.95f);
						world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1.9f);
						world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1f, 2.0f);
						world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.2f);
					}
					case 20 -> {
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.1f);
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.1f);
						world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 1.55f);
					}
					case 40 -> {
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.3f);
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.3f);
						world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 1.7f);
					}
					case 60 -> {
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.5f);
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.5f);
						world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 1.85f);
					}
					case 80 -> {
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.7f);
						world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.7f);
						world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 2f);
					}
					default -> { }
				}

				if (mTicks % 4 == 0) {
					List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true);
					for (Player p : players) {
						// heal 5 times per second
						double healed = PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * mHeal / 5, mPlayer);

						if (healed > 0) {
							new PartialParticle(Particle.HEART, LocationUtils.getHalfHeightLocation(p), 2, 0.5, 1, 0.5, 0).spawnAsPlayerActive(mPlayer);
						}
					}

					// draw a particle arc to a random player
					if (!players.isEmpty()) {
						Collections.shuffle(players);

						Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.75).subtract(0, .8, 0);
						Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.75).subtract(0, .8, 0);
						Location sourceLoc = FastUtils.RANDOM.nextBoolean() ? rightHand : leftHand;

						createOrb(new Vector(FastUtils.randomDoubleInRange(-0.5, 0.5),
							FastUtils.randomDoubleInRange(0.3, 0.5),
							FastUtils.randomDoubleInRange(-0.5, 0.5)), sourceLoc, mPlayer, players.get(0));
					}
				}


				mTicks += 1;
				if (mTicks >= mStasisDuration) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				Location loc = mPlayer.getLocation();
				World world = mPlayer.getWorld();

				new PartialParticle(Particle.WAX_OFF, LocationUtils.getHalfHeightLocation(mPlayer), (int) (50 * mRadius / 5), mRadius * 0.6, mRadius * 0.3, mRadius * 0.6, 5f).spawnAsPlayerActive(mPlayer);

				world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 2.0f);
				world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 2.0f);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 2.0f);
				world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 2.0f);

				for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
					AbsorptionUtils.addAbsorption(p, mAbsorption, mAbsorption, mAbsorptionDuration);
					new PartialParticle(Particle.SPELL_INSTANT, LocationUtils.getHalfHeightLocation(p), 30, 0.4, 0.4, 0.4, 0.5).spawnAsPlayerActive(mPlayer);
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
					EntityUtils.applyStun(mPlugin, mStunDuration, mob);
					MovementUtils.knockAway(loc, mob, 0.4f);
				}

				mStasisActive = false;
				super.cancel();
			}

		}.runTaskTimer(mPlugin, 0, 1);

		sendActionBarMessage("Eternal Savior has been activated!"); // never shows up cause "You are in stasis!" message overrides it, lol
		event.setCancelled(true);
	}

	private void createOrb(Vector dir, Location loc, Player mPlayer, LivingEntity target) {
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = LocationUtils.getHalfHeightLocation(target);
				if (!to.getWorld().equals(mL.getWorld())) {
					cancel();
					return;
				}

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.095;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.25) {
						mD.normalize().multiply(0.25);
					}

					mL.add(mD);

					Color c = FastUtils.RANDOM.nextBoolean() ? HEAL_BEAM_COLOR : HEAL_BEAM_COLOR_LIGHT;
					double red = c.getRed() / 255D;
					double green = c.getGreen() / 255D;
					double blue = c.getBlue() / 255D;
					new PartialParticle(Particle.SPELL_MOB,
						mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05)),
						1, red, green, blue, 1)
						.directionalMode(true).spawnAsPlayerActive(mPlayer);

					c = FastUtils.RANDOM.nextBoolean() ? HEAL_BEAM_COLOR : HEAL_BEAM_COLOR_LIGHT;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1f))
						.spawnAsPlayerActive(mPlayer);

					if (mT > 5 && mL.distance(to) < 0.6) {
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

	public boolean hasIncreasedReviveRadius() {
		return mStasisActive;
	}

	public double getIncreasedReviveRadius() {
		return mRadius / 2;
	}

	public Location getSaviorLocation() {
		return mStasisLocation;
	}

	private static Description<EternalSavior> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<EternalSavior>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", enter stasis for ")
			.addDuration(a -> STASIS_DURATION, STASIS_DURATION)
			.add(" seconds. During this time, rapidly heal all other players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks for ")
			.addPercent(a -> a.mHeal * STASIS_DURATION / 20, HEAL_PER_SECOND[rarity - 1] * STASIS_DURATION / 20, false, true)
			.add(" of their max health over the full duration and begin reviving all graves in a ")
			.add(a -> a.mRadius / 2.0, RADIUS / 2.0)
			.add(" block radius. When stasis ends, give ")
			.add(a -> a.mAbsorption, ABSORPTION[rarity - 1], false, null, true)
			.add(" absorption to all players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks for ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_DURATION)
			.add(" seconds and stun nearby mobs for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds and knock them away.")
			.addCooldown(COOLDOWN);
	}


}
