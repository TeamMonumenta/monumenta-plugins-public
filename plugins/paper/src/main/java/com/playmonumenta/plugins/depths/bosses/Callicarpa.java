package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.shadow.ChaosDagger;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.BrambleBall;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.EvolutionSeeds;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.FlowerPower;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.LeafNova;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.PassiveFloralInsignia;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.PassiveGardenTwo;
import com.playmonumenta.plugins.depths.bosses.spells.callicarpa.ThornIvy;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Callicarpa extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_callicarpa";
	/* These are used in other Callicarpa spells */
	public static final String FLOWER_TAG = "callicarpa_flower";
	public static final String FLOWER_EVOLVED_TAG = "callicarpa_flowerevolved";
	public static final String FLOWER_SPOT_TAG = "callicarpa_flowerspot";
	public static final String MOB_SPAWN_SPOT_TAG = "callicarpa_mobspot";
	public static final int detectionRange = 100;

	private static final double HEALTH = 7500;
	private static final int MENACE_SPAWN_DURATION = 10;
	private static final double MENACE_SPAWN_HEIGHT = 10;
	private static final int MENACE_MOUNT_START_DELAY = 6;
	private static final int MENACE_MOUNT_DELAY = 8;
	private static final Color MENACE_LINK_COLOR = Color.fromRGB(114, 39, 179);
	private static final Particle.DustOptions mMenaceLinkOptions = new Particle.DustOptions(MENACE_LINK_COLOR, 2);
	private static final String MUSIC_TITLE = "epic:music.hedera";
	private static final int MUSIC_DURATION = 202; // seconds

	private final int mFloorY;
	private final @Nullable DepthsParty mParty;

	private @Nullable Hoglin mMenace;
	private boolean mIsReflectingProjectiles = false;

	public Callicarpa(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mFloorY = spawnLoc.getBlockY() - 1;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.addScoreboardTag(ChaosDagger.NO_GLOWING_CLEAR_TAG);

		// Health is scaled by party ascension
		mParty = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		EntityUtils.setMaxHealthAndHealth(mBoss, DepthsParty.getAscensionScaledHealth(HEALTH, mParty));

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		SongManager.playBossSong(players, new SongManager.Song(MUSIC_TITLE, SoundCategory.RECORDS, MUSIC_DURATION, true, 2.0f, 1.0f, false), true, mBoss, true, 0, 5);

		spawnAnimation();

		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), 30.0);
		for (ArmorStand stand : nearbyStands) {

			//Set bedrock behind boss room
			if (stand.getName().contains(Hedera.DOOR_FILL_TAG)) {
				Location baseLoc = stand.getLocation().getBlock().getLocation();
				stand.remove();
				Location p1 = baseLoc.clone().add(0, -6, -6);
				Location p2 = baseLoc.clone().add(0, 6, 6);
				LocationUtils.fillBlocks(p1, p2, Material.BEDROCK);
				p1 = p1.clone().add(1, 0, 0);
				p2 = p2.clone().add(1, 0, 0);
				LocationUtils.fillBlocks(p1, p2, Material.BLACK_CONCRETE);
			}
		}

		// Garden handles both flower spawning, and adds spawning at Ascension 4+
		PassiveGardenTwo garden = new PassiveGardenTwo(mBoss, mParty);

		// Passive Spells
		List<Spell> passives;
		if (mParty != null && mParty.getAscension() >= 15) {
			passives = List.of(
				garden,
				new SpellBlockBreak(mBoss, 2, 3, 2, true, Material.AIR),
				new PassiveFloralInsignia(mBoss, mFloorY)
			);
		} else {
			passives = List.of(
				garden,
				new SpellBlockBreak(mBoss, 2, 3, 2, true, Material.AIR)
			);
		}

		FlowerPower flowerPower = new FlowerPower(mBoss, mFloorY, mParty, garden);
		ThornIvy thornIvy = new ThornIvy(mBoss, mFloorY, mParty, this);
		BrambleBall brambleBall = new BrambleBall(mBoss, mFloorY, mParty);
		EvolutionSeeds evolutionSeeds = new EvolutionSeeds(mBoss, mParty, garden);
		LeafNova leafNova = new LeafNova(mBoss, mParty);

		// Phase 1 - 100% - 90%
		SpellManager phase1Spells = new SpellManager(List.of(
			thornIvy, brambleBall, evolutionSeeds, leafNova
		));

		// Phase 2 - 90% - 50%
		SpellManager phase2Spells = new SpellManager(List.of(
			thornIvy, flowerPower, brambleBall, evolutionSeeds, leafNova
		));

		// Phase 3- 50% - 00%
		SpellManager phase3Spells = new SpellManager(List.of(
			thornIvy, flowerPower, brambleBall, evolutionSeeds
		));

		// Health Events
		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(90, mBoss -> {
			// Change to Phase 2.
			changePhase(phase2Spells, passives, null);
		});
		events.put(80, mBoss -> {
			// Summon the Hoglin Menace. It shall be a menace.
			Location menaceSpawnLoc = mBoss.getLocation().clone();
			menaceSpawnLoc.setY(mFloorY + 1);
			summonMenace(menaceSpawnLoc);
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
				player.sendMessage(
					Component.text("[Callicarpa] ", NamedTextColor.GOLD)
						.append(Component.text("Ah, foe indeed; yet I am caged. Defend the blight or face their rage...", NamedTextColor.DARK_GREEN))
				);
			}
		});
		events.put(50, mBoss -> {
			// Change to Phase 3
			changePhase(phase3Spells, passives, null);
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
				player.sendMessage(
					Component.text("[Callicarpa] ", NamedTextColor.GOLD)
						.append(Component.text("You know not what has fallen here... for if you knew, you'd run in fear. It whispers in my head... constantly... even my spells are falling victim to what this presence demands...", NamedTextColor.DARK_GREEN))
				);
			}
			// Launch both the Menace and Callicarpa towards each other.
			// Run this with a couple ticks of delay JUST IN CASE people SOMEHOW get Callicarpa down from 81% to 50%
			// in one single strike. It also lets me draw a cool line between Callicarpa and the Menace.
			if (mMenace == null) {
				return;
			}

			// Pre-Animation Aesthetics
			new PPLine(Particle.REDSTONE, mBoss.getLocation().clone().add(0, 1, 0), mMenace.getLocation().clone().add(0, 1, 0)).countPerMeter(2).extra(0)
				.data(mMenaceLinkOptions).spawnAsBoss();
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 2f);

			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> {
				if (mMenace == null) {
					return;
				}
				// Jump sounds
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 10f, 0.8f);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 10f, 0.8f);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 10f, 0.8f);

				Vector menaceToCalli = mBoss.getLocation().toVector().subtract(mMenace.getLocation().toVector()).normalize();
				Vector calliToMenace = mMenace.getLocation().toVector().subtract(mBoss.getLocation().toVector()).normalize();
				double midpointDistance = mBoss.getLocation().distance(mMenace.getLocation()) * 0.5;
				calliToMenace.multiply(Math.log(midpointDistance));
				calliToMenace.setY(0.7);
				menaceToCalli.multiply(Math.log(midpointDistance));
				menaceToCalli.setY(0.7);

				mBoss.setVelocity(calliToMenace);
				mMenace.setVelocity(menaceToCalli);

				Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> {
					// Callicarpa mounts the Hoglin Menace, after a delay.
					// Remove the RangedAttackGoal from Callicarpa, since that will make the Hoglin behave like a ranged mob
					// (it would stop running at its target if within 5 blocks of it)
					if (mMenace == null) {
						return;
					}

					NmsUtils.getVersionAdapter().disableRangedAttackGoal(mBoss);
					mMenace.addPassenger(mBoss);
				}, MENACE_MOUNT_DELAY);
			}, MENACE_MOUNT_START_DELAY);
		});

		// Boss Bar and Construct
		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, detectionRange, BarColor.PURPLE, BarStyle.SEGMENTED_10, events);
		constructBoss(phase1Spells, passives, detectionRange, bossBar, 100, 1, true);
	}

	private void summonMenace(Location loc) {
		// Summon a ring of particles at the spawn location to signify what's happening.
		new PPCircle(Particle.FLAME, loc, 5).count(480).extra(0).spawnAsBoss();

		// Start the Animation for the Menace spawning.
		new BukkitRunnable() {
			final Location mCurrLoc = loc.clone().add(0, MENACE_SPAWN_HEIGHT, 0);
			final Location mSpawnLoc = loc.clone();
			final double mHeightDecrease = MENACE_SPAWN_HEIGHT / (double) MENACE_SPAWN_DURATION;

			int mTicks = 0;

			@Override
			public void run() {
				// Summon two rings of particles at current location.
				new PPCircle(Particle.FLAME, mCurrLoc, 3).ringMode(true).count(45).extra(0.1).spawnAsBoss();
				new PPCircle(Particle.FLAME, mCurrLoc, 1).ringMode(true).count(27).extra(0.05).spawnAsBoss();

				// Play a sound at the current location.
				mBoss.getWorld().playSound(mCurrLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1f, 1.5f);

				// Decrease the height of the location.
				mCurrLoc.subtract(0, mHeightDecrease, 0);

				if (mTicks >= MENACE_SPAWN_DURATION) {
					// Create a particle and smoke explosion at the ground.
					new PartialParticle(Particle.FLAME, mCurrLoc, 200).extra(0.25).spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_HUGE, mCurrLoc, 1).extra(0).spawnAsBoss();
					new PPCircle(Particle.CLOUD, mCurrLoc.clone().add(0, 1, 0), 0.5).countPerMeter(5)
						.rotateDelta(true).delta(1, 0, 0).ringMode(true).extra(0.1).spawnAsBoss();
					// Summon the Menace.
					Entity menace = LibraryOfSoulsIntegration.summon(mSpawnLoc, "HoglinMenace");
					if (menace instanceof Hoglin hoglin) {
						mMenace = hoglin;
						mMenace.addScoreboardTag(ChaosDagger.NO_GLOWING_CLEAR_TAG);
						mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10f, 1f);
						mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 2f);

						// Knock players away from the impact point
						mCurrLoc.getNearbyPlayers(2)
							.forEach(hitPlayer -> MovementUtils.knockAwayRealistic(mCurrLoc, hitPlayer, 2, 0.5f, true));
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}

	private void spawnAnimation() {
		Location center = mBoss.getLocation();
		new PPFlower(Particle.REDSTONE, center, 5).petals(13).angleStep(0.05)
			.transitionColors(Color.fromRGB(108, 195, 245), Color.fromRGB(139, 26, 214), 1)
			.spawnAsBoss();
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, Component.text("Callicarpa", TextColor.fromHexString("#1b8c2c")), Component.text("Blight of the Beyond", TextColor.fromHexString("#e673c5")));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
			player.sendMessage(
				Component.text("[Callicarpa] ", NamedTextColor.GOLD)
					.append(Component.text("Oh stars above and dark below, the whispers question, friend or foe?", NamedTextColor.DARK_GREEN))
			);
		}
	}

	public void isReflectingProjectiles(boolean isReflectingProjectiles) {
		mIsReflectingProjectiles = isReflectingProjectiles;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		// Kill off the Menace
		if (mMenace != null) {
			mMenace.setHealth(0);
			mMenace.remove();
		}

		// Kill off all the Flowers
		Location loc = mBoss.getLocation();
		loc.getNearbyEntities(200, 30, 200).stream()
			.filter(e -> e.getScoreboardTags().contains(FLOWER_TAG)).forEach(Entity::remove);

		for (Player player : PlayerUtils.playersInRange(loc, detectionRange, true)) {
			player.sendMessage(
				Component.text("[Callicarpa] ", NamedTextColor.GOLD)
					.append(Component.text("Oh yes, at last, the voice recedes... with final breath... I am now freed...", NamedTextColor.DARK_GREEN))
			);
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 4));
		}

		new BukkitRunnable() {
			final int mMaxTimesRun = 20;
			final double mYIncrease = 2;
			final Location mCurrLoc = mBoss.getLocation();
			final Particle.DustOptions mBaseColor = new Particle.DustOptions(Color.fromRGB(232, 46, 124), 2);
			final Particle.DustOptions mFlowerColor = new Particle.DustOptions(Color.fromRGB(237, 145, 242), 1);

			int mRuns = 0;

			@Override
			public void run() {
				if (mRuns == 0) {
					new PPFlower(Particle.REDSTONE, mCurrLoc, 6).data(mBaseColor).petals(9).angleStep(0.05).spawnAsBoss();
				} else {
					new PPFlower(Particle.REDSTONE, mCurrLoc, 2).data(mFlowerColor).petals(5).spawnAsBoss();
				}

				mCurrLoc.add(0, mYIncrease, 0);

				mRuns++;
				if (mRuns >= mMaxTimesRun) {
					DepthsManager.getInstance().bossDefeated(loc, detectionRange);
					cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getBossSpellName() != null) {
			event.setDamage(DepthsParty.getAscensionScaledDamage(event.getDamage(), mParty));
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (
			mIsReflectingProjectiles &&
			event.getDamager() instanceof Player damager &&
			DamageEvent.DamageType.getAllProjectileTypes().contains(event.getType())
		) {
			FlowerPower.launchEnergyLaser(
				damager,
				mBoss.getEyeLocation(),
				mBoss, new Particle.DustOptions(FlowerPower.ENERGY_COLOR, 1.5f),
				mFloorY,
				FlowerPower.LASER_TRAVEL_SPEED,
				null,
				false
			);
			event.setCancelled(true);
		}

		if (event.getDamager() instanceof Player) {
			double damageCap = 0.25 * EntityUtils.getMaxHealth(mBoss);
			if (event.getDamage() > damageCap) {
				event.setDamage(damageCap);
			}
		}
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		if (
			mIsReflectingProjectiles &&
			event.getEntity().getShooter() instanceof Player shooter
		) {
			FlowerPower.launchEnergyLaser(
				shooter,
				mBoss.getEyeLocation(),
				mBoss, new Particle.DustOptions(FlowerPower.ENERGY_COLOR, 1.5f),
				mFloorY,
				FlowerPower.LASER_TRAVEL_SPEED,
				null,
				false
			);
			event.setCancelled(true);
		}
	}
}
