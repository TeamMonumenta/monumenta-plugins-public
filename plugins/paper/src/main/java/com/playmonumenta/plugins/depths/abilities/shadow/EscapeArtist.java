package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EscapeArtist extends DepthsAbility {
	public static final String ABILITY_NAME = "Escape Artist";
	private static final int COOLDOWN = 90 * 20;
	private static final double TRIGGER_HEALTH = 0.3;
	private static final String RESISTANCE_EFFECT_NAME = "EscapeArtistResistance";
	private static final double RESISTANCE = 0.5;
	private static final int[] STEALTH_DURATION = {60, 70, 80, 90, 100, 120};
	private static final int TP_WINDOW = 60;
	private static final double PROJECTILE_SPEED = 2.25;
	private static final double MAX_TP_DISTANCE = 14;
	private static final double STUN_RADIUS = 5;
	private static final int[] STUN_DURATION = {20, 30, 40, 50, 60, 80};

	public static final String CHARM_COOLDOWN = "Escape Artist Cooldown";


	public static final DepthsAbilityInfo<EscapeArtist> INFO =
		new DepthsAbilityInfo<>(EscapeArtist.class, ABILITY_NAME, EscapeArtist::new, DepthsTree.SHADOWDANCER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.ESCAPE_ARTIST)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EscapeArtist::cast, new AbilityTrigger(AbilityTrigger.Key.DROP)))
			.displayItem(Material.ENDER_PEARL)
			.descriptions(EscapeArtist::getDescription)
			.priorityAmount(10000);

	private final Map<Snowball, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();
	private final int mStealthDuration;
	private final double mMaxTPDistance;
	private final double mProjectileSpeed;
	private final double mStunRadius;
	private final int mStunDuration;
	private boolean mTPReady;

	public EscapeArtist(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mStealthDuration = CharmManager.getDuration(mPlayer, CharmEffects.ESCAPE_ARTIST_STEALTH_DURATION.mEffectName, STEALTH_DURATION[mRarity - 1]);
		mMaxTPDistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ESCAPE_ARTIST_MAX_TP_DISTANCE.mEffectName, MAX_TP_DISTANCE);
		mProjectileSpeed = PROJECTILE_SPEED;
		mStunRadius = CharmManager.getRadius(mPlayer, CharmEffects.ESCAPE_ARTIST_STUN_RADIUS.mEffectName, STUN_RADIUS);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.ESCAPE_ARTIST_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
		mTPReady = false;
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

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		putOnCooldown();

		mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(mStealthDuration, -RESISTANCE));
		AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration);

		mTPReady = true;
		// set to false later if the player did not teleport
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (mTPReady) {
				world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
			}
			mTPReady = false;
		}, TP_WINDOW);

		new PartialParticle(Particle.SMOKE_LARGE, loc, 80, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 120, 0, 0, 0, 0.33).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_WITCH, loc, 80, 1, 1, 1, 0.3).spawnAsPlayerActive(mPlayer);

		new PPLine(Particle.REDSTONE, mPlayer.getLocation().add(2.5, 0, 2.5), mPlayer.getLocation().add(-2.5, 0, -2.5))
			.data(new Particle.DustOptions(Color.BLACK, 2f)).countPerMeter(3.5).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().add(-2.5, 0, 2.5), mPlayer.getLocation().add(2.5, 0, -2.5))
			.data(new Particle.DustOptions(Color.BLACK, 2f)).countPerMeter(3.5).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, mPlayer.getLocation(), 1.75).ringMode(true).countPerMeter(3.5)
			.data(new Particle.DustOptions(Color.BLACK, 2f)).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_SKELETON_DEATH, SoundCategory.PLAYERS, 0.7f, 0.65f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_DONKEY_DEATH, SoundCategory.PLAYERS, 0.4f, 0.8f);

		sendActionBarMessage("Escape Artist has been activated!");
		event.setCancelled(true);
	}

	public boolean cast() {
		if (!mTPReady) {
			return false;
		}

		mTPReady = false;

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 20, 0, 0, 0, 0.6).spawnAsPlayerActive(mPlayer);

		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, mProjectileSpeed, "EscapeArtistProjectile", Particle.SMOKE_NORMAL);
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mPlayerItemStatsMap.put(proj, playerItemStats);

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final Location mPlayerLocation = mPlayer.getLocation();
			@Override
			public void run() {
				if (mPlayerItemStatsMap.get(proj) != playerItemStats) {
					mPlugin.mProjectileEffectTimers.removeEntity(proj);
					this.cancel();
				}

				if (proj.getLocation().distance(mPlayerLocation) > mMaxTPDistance) {
					proj.remove();
					mPlugin.mProjectileEffectTimers.removeEntity(proj);
					this.cancel();
				}

				// max time limit to avoid weird scenarios
				if (mT > 100) {
					mPlugin.mProjectileEffectTimers.removeEntity(proj);
					proj.remove();
					this.cancel();
				}

				if (proj.isDead()) {
					if (mPlayerItemStatsMap.remove(proj) != null) {
						DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
						if (dp != null && !dp.mDead) {
							executeTeleport(proj.getLocation().add(0, 1, 0).setDirection(mPlayer.getEyeLocation().getDirection()));
						}
					}
					mPlugin.mProjectileEffectTimers.removeEntity(proj);
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

		world.playSound(loc, Sound.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && proj.getTicksLived() <= 100) {
			ItemStatManager.PlayerItemStats stats = mPlayerItemStatsMap.remove(proj);
			if (!mPlayer.getWorld().equals(proj.getWorld())) {
				return;
			}
			if (stats != null) {
				mPlugin.mProjectileEffectTimers.removeEntity(proj);
				executeTeleport(proj.getLocation().add(0, 1, 0).setDirection(mPlayer.getEyeLocation().getDirection()));
			}
		}
	}

	public void executeTeleport(Location destination) {
		World world = mPlayer.getWorld();
		Location originalLoc = mPlayer.getLocation();

		// do stun at original location first, then teleport
		for (LivingEntity mob : EntityUtils.getNearbyMobs(originalLoc, mStunRadius, mPlayer)) {
			EntityUtils.applyStun(mPlugin, mStunDuration, mob);
		}

		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, originalLoc, 75, 1.5, 1.5, 1.5, 0.28).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, originalLoc, 125, 1.5, 1.5, 1.5, 0.28).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SQUID_INK, originalLoc.clone().add(0, 0.5, 0), mStunRadius)
			.ringMode(false)
			.countPerMeter(3)
			.spawnAsPlayerActive(mPlayer);

		world.playSound(originalLoc, Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(originalLoc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 2.0f);

		if (!ZoneUtils.hasZoneProperty(originalLoc, ZoneProperty.NO_MOBILITY_ABILITIES)
			&& !ZoneUtils.hasZoneProperty(destination, ZoneProperty.NO_MOBILITY_ABILITIES)) {

			if (LocationUtils.blinkCollisionCheck(mPlayer, destination.toVector())) {
				Vector vec = destination.clone().subtract(originalLoc).toVector().normalize().multiply(0.4);
				vec.setY(0.4);
				mPlayer.setVelocity(vec);

				mPlugin.mEffectManager.addEffect(mPlayer, "EscapeArtistFallImmunity", new PercentDamageReceived(60, -1, EnumSet.of(DamageType.FALL)));

				new PPLine(Particle.SQUID_INK, originalLoc, destination, 0.2)
					.countPerMeter(3)
					.spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.DRAGON_BREATH, destination, 20, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_WITCH, destination, 20, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.SMOKE_LARGE, destination, 1.5)
					.ringMode(false)
					.countPerMeter(3)
					.extra(0.1)
					.spawnAsPlayerActive(mPlayer);

				world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.95f);
				world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.25f);
				world.playSound(destination, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.5f, 2.0f);
			}
		}
	}

	private static Description<EscapeArtist> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<EscapeArtist>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", ignore the hit and gain 50% resistance and stealth for ")
			.addDuration(a -> a.mStealthDuration, STEALTH_DURATION[rarity - 1], false, true)
			.add(" seconds. Hitting the drop key within the next ")
			.addDuration(a -> TP_WINDOW, TP_WINDOW)
			.add(" seconds throws a projectile that travels up to ")
			.add(a -> a.mMaxTPDistance, MAX_TP_DISTANCE)
			.add(" blocks away. Upon hitting a mob, block, or reaching its max distance, you teleport to its location and leave behind a smoke bomb that stuns all mobs in a ")
			.add(a -> a.mStunRadius, STUN_RADIUS)
			.add(" block radius for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], false, true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}
