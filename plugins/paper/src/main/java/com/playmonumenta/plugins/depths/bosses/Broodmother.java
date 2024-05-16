package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.AntiRangeBoss;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.shadow.ChaosDagger;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.PassiveBite;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.PassiveLaserCores;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.PassiveLaserEyes;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.PassivePoisonousSkin;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.PassiveSpider;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellBloodyFang;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellDashBroodmother;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellEggThrow;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellLegSweep;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellSlam;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellTantrum;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellVenomSpray;
import com.playmonumenta.plugins.depths.bosses.spells.broodmother.SpellWebCarpet;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Broodmother extends SerializedLocationBossAbilityGroup {

	public static final String identityTag = "boss_broodmother";
	public static final int detectionRange = 100;
	public static final int VULNERABILITY_TIME = 20 * 90;
	public static final int DELAY_BETWEEN_LIMBS_RESPAWN = 20 * 10;
	public static final int GROUND_Y_LEVEL = 129;
	public static final double HEALTH = 5000;
	public static final double LIMB_HEALTH = 600;
	public static final double VULNERABILITY_A4_INCREASE = 0.15;
	public static final double VULNERABILITY_A8_INCREASE = 0.15;
	public static final double VULNERABILITY_A15_INCREASE = 0.2;

	public static final String MUSIC_TITLE_AMBIENT = "epic:music.broodmother_ambient";
	public static final int MUSIC_DURATION_AMBIENT = 2 * 60;
	public static final String MUSIC_TITLE = "epic:music.broodmother_phase1";
	public static final int MUSIC_DURATION = 2 * 60 + 53; // seconds
	public static final String MUSIC_TITLE_2 = "epic:music.broodmother_phase2";
	public static final int MUSIC_DURATION_2 = 3 * 60 + 21; // seconds

	private final BukkitRunnable mLimbsRunnable;
	private final Slime[] mLimbs = new Slime[4];
	private final Location[] mLimbLocs = new Location[4];
	private final @Nullable DepthsParty mParty;
	// Needed references to cancel structure pastings on death.
	private final SpellLegSweep mLegSweep;
	private final SpellTantrum mTantrum;
	private final @Nullable PassiveLaserCores mLaserCores;

	private int mVulnerableTicks = 0;
	private boolean mLimbsDied = false;
	private boolean mPausedVulnerableTimer = false;
	private boolean mIsRespawningLimbs = true;
	private boolean mCastingDisruptive = false;

	public Broodmother(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mParty = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc.clone().subtract(30, 0, 0));

		// Range immunity for main weakpoint is a8+. Remove it if lower.
		if (mParty != null && mParty.getAscension() < 8) {
			Plugin.getInstance().mBossManager.removeAbility(mBoss, AntiRangeBoss.identityTag);
		}

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.addScoreboardTag(ChaosDagger.NO_GLOWING_CLEAR_TAG);

		// Teleport the boss because depths bossfight command only accepts integer coordinates
		mBoss.teleport(mBoss.getLocation().add(0, 0, 0.5));

		// Health is scaled by party ascension
		EntityUtils.setMaxHealthAndHealth(mBoss, DepthsParty.getAscensionScaledHealth(HEALTH, mParty));

		// Respawn the boss structure
		StructuresAPI.loadAndPasteStructure("BikeSpiderBase", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
		startEffects();

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		SongManager.playBossSong(players, new SongManager.Song(MUSIC_TITLE, SoundCategory.RECORDS, MUSIC_DURATION, true, 1.0f, 1.0f, false), true, mBoss, true, 0, 5);

		PassiveSpider passiveSpider = new PassiveSpider(mBoss);
		PassivePoisonousSkin passivePoisonousSkin = new PassivePoisonousSkin(mBoss, mParty);
		PassiveBite passiveBite = new PassiveBite(this, mBoss, mParty);

		mLegSweep = new SpellLegSweep(mBoss, mParty, this);
		SpellVenomSpray spellVenomSpray = new SpellVenomSpray(mBoss, mParty);
		SpellWebCarpet spellWebCarpet = new SpellWebCarpet(mBoss, mParty);
		SpellEggThrow spellEggThrow = new SpellEggThrow(mBoss, mParty);
		SpellDashBroodmother spellDashBroodmother = new SpellDashBroodmother(mBoss, mParty);
		SpellSlam spellSlam = new SpellSlam(mBoss, mParty);
		SpellBloodyFang spellBloodyFang = new SpellBloodyFang(mBoss, mParty);
		mTantrum = new SpellTantrum(mBoss, mParty);

		List<Spell> passives;
		if (mParty != null && mParty.getAscension() >= 15) {
			mLaserCores = new PassiveLaserCores(mBoss);
			passives = List.of(
				new PassiveLaserEyes(mBoss, this, mParty),
				mLaserCores,
				passiveSpider,
				passiveBite,
				passivePoisonousSkin
			);
		} else if (mParty != null && mParty.getAscension() >= 8) {
			mLaserCores = null;
			passives = List.of(
				new PassiveLaserEyes(mBoss, this, mParty),
				passiveSpider,
				passiveBite,
				passivePoisonousSkin
			);
		} else {
			mLaserCores = null;
			passives = List.of(
				passiveSpider,
				passiveBite,
				passivePoisonousSkin
			);
		}

		SpellManager phase1Spells = new SpellManager(
			Arrays.asList(
				mLegSweep,
				spellVenomSpray,
				spellWebCarpet,
				spellEggThrow,
				spellBloodyFang
			)
		);

		SpellManager phase2Spells = new SpellManager(
			Arrays.asList(
				mLegSweep,
				spellVenomSpray,
				spellWebCarpet,
				spellEggThrow,
				spellBloodyFang,
				mTantrum
			)
		);

		SpellManager phase3Spells = new SpellManager(
			Arrays.asList(
				spellDashBroodmother,
				mLegSweep,
				spellVenomSpray,
				spellWebCarpet,
				spellEggThrow,
				spellBloodyFang,
				mTantrum
			)
		);

		SpellManager phase4Spells = new SpellManager(
			Arrays.asList(
				spellDashBroodmother,
				mLegSweep,
				spellVenomSpray,
				spellSlam,
				spellWebCarpet,
				spellEggThrow,
				spellBloodyFang,
				mTantrum
			)
		);

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(75, mBoss -> {
			changePhase(phase2Spells, passives, null);
			spawnLimbs();
			if (mLaserCores != null) {
				mLaserCores.spawnNewCores(1);
			}
			makeBossInvulnerable();
			mVulnerableTicks = 0;
			mLimbsDied = false;
		});
		events.put(50, mBoss -> {
			changePhase(phase3Spells, passives, null);
			spawnLimbs();
			if (mLaserCores != null) {
				mLaserCores.spawnNewCores(1);
			}
			makeBossInvulnerable();
			mVulnerableTicks = 0;
			mLimbsDied = false;
			List<Player> p = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
			SongManager.playBossSong(p, new SongManager.Song(MUSIC_TITLE_2, SoundCategory.RECORDS, MUSIC_DURATION_2, true, 1.0f, 1.0f, false), true, mBoss, true, 0, 5);
		});
		events.put(25, mBoss -> {
			changePhase(phase4Spells, passives, null);
			spawnLimbs();
			if (mLaserCores != null) {
				mLaserCores.spawnNewCores(2);
			}
			makeBossInvulnerable();
			mVulnerableTicks = 0;
			mLimbsDied = false;
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		constructBoss(phase1Spells, passives, detectionRange, bossBar, 140, 1, true);

		// Right front limb
		mLimbLocs[0] = mBoss.getLocation().add(-4.5, 0, 9);
		// Left front limb
		mLimbLocs[1] = mBoss.getLocation().add(-4.5, 0, -9);
		// Right back limb
		mLimbLocs[2] = mBoss.getLocation().add(5.5, 0.9, 9);
		// Left back limb
		mLimbLocs[3] = mBoss.getLocation().add(5.5, 0.9, -9);
		spawnLimbs();

		// Check to open core when limbs are all broken
		mLimbsRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!mIsRespawningLimbs && (mLimbsDied || haveAllLimbsDied())) {
					if (mVulnerableTicks == 0) {
						// Set this boolean so that it doesn't have to do the calculation again
						mLimbsDied = true;
						makeBossVulnerable();
						spawnElites();
					}
					if (mVulnerableTicks >= VULNERABILITY_TIME) {
						// Respawn the limbs
						makeBossInvulnerable();
						spawnLimbs();
					} else if (!mPausedVulnerableTimer) {
						mVulnerableTicks++;
					}
				}

				if (!mBoss.isValid()) {
					this.cancel();
				}
			}
		};
		mLimbsRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mLimbsRunnable.cancel();
		clearLimbs();
		if (mLaserCores != null) {
			mLaserCores.removeAllCores();
		}

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		BossUtils.endBossFightEffects(players);

		mLegSweep.stopLegTasks();
		mTantrum.stopTantrumTasks();

		// Prepare for explosion
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			StructuresAPI.loadAndPasteStructure("BikeSpiderSweepRight0", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			StructuresAPI.loadAndPasteStructure("BikeSpiderSweepLeft0", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
		}, 5);

		BukkitRunnable deathRunnable = new BukkitRunnable() {
			float mPitch = 0;
			@Override
			public void run() {
				if (mPitch > 2.0f) {
					// Death explosion
					List<BlockDisplay> blockDisplays = DisplayEntityUtils.turnBlockCuboidIntoBlockDisplays(mBoss.getLocation().clone().add(-8, -1, -12), mBoss.getLocation().clone().add(19, 9, 12), true);
					// Remove a percentage of the block displays for performance reasons
					ArrayList<BlockDisplay> toRemove = new ArrayList<>();
					blockDisplays.forEach(blockDisplay -> {
						if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
							toRemove.add(blockDisplay);
							blockDisplay.remove();
						}
					});
					blockDisplays.removeIf(toRemove::contains);
					// Do an explosion outwards from the rough center of the boss
					Location center = mBoss.getLocation().add(8, 4, 0.5);
					blockDisplays.forEach(blockDisplay -> {
						Vector3f translation = LocationUtils.getDirectionTo(blockDisplay.getLocation(), center).toVector3f().mul(5);
						blockDisplay.setInterpolationDelay(-1);
						blockDisplay.setInterpolationDuration(20);
						blockDisplay.setTransformation(new Transformation(translation, new Quaternionf(), new Vector3f(), new Quaternionf()));
					});
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 10, 0.5f), 10);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 10, 0.5f), 10);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 10, 0.5f), 10);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 10, 0.5f), 10);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 10, 0.5f), 10);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> blockDisplays.forEach(Entity::remove), 20);
					DepthsManager.getInstance().bossDefeated(mBoss.getLocation(), detectionRange);

					cancel();
					return;
				}

				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, mPitch);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, mPitch);
				mPitch += 0.1f;
			}
		};


		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10, 1);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> deathRunnable.runTaskTimer(mPlugin, 0, 3), 10);
	}

	private void startEffects() {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, 0.5f), 20);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, 0.5f), 50);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, 0.5f), 60);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 10, 0.5f), 80);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 1.5f), 100);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
				MessagingUtils.sendBoldTitle(player, Component.text("The Broodmother", NamedTextColor.DARK_RED), Component.text("Norvigut Deity", NamedTextColor.GOLD));
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
			}
		}, 100);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		// Hide the limbs at certain times for certain abilities, to make the spider invulnerable
		// during attack animations, without having to actually move the limbs with the animation
		Class<? extends Spell> spellClass = event.getSpell().getClass();
		if (spellClass.equals(SpellDashBroodmother.class) || spellClass.equals(SpellSlam.class)) {
			hideBossSpots(event.getSpell().castTicks());
		}
	}

	public void moveLimb(int index, Vector movement) {
		if (index < 0 || index > mLimbLocs.length) {
			return;
		}

		Slime limb = mLimbs[index];
		if (limb == null || !limb.isValid()) {
			return;
		}

		limb.teleport(limb.getLocation().add(movement));
	}

	private boolean haveAllLimbsDied() {
		return Arrays.stream(mLimbs).filter(Objects::nonNull).noneMatch(Entity::isValid);
	}

	private void clearLimbs() {
		Arrays.stream(mLimbs).filter(Objects::nonNull).forEach(Entity::remove);
	}

	private void spawnLimbs() {
		mIsRespawningLimbs = true;

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mLimbsDied = false;
			mVulnerableTicks = 0;
			mIsRespawningLimbs = false;

			for (int i = 0; i < 4; i++) {
				Entity e = LibraryOfSoulsIntegration.summon(mLimbLocs[i], "BroodmotherLimb");
				if (e instanceof Slime slime) {
					mLimbs[i] = slime;
					EntityUtils.setMaxHealthAndHealth(slime, DepthsParty.getAscensionScaledHealth(LIMB_HEALTH, mParty));
				} else {
					mLimbs[i] = null;
				}
				new PartialParticle(Particle.FIREWORKS_SPARK, mLimbLocs[i], 100).delta(0.5).extra(0.2).spawnAsEntityActive(mBoss);
			}
		}, DELAY_BETWEEN_LIMBS_RESPAWN);
	}

	private void makeBossSpotsInvulnerable(boolean includeBossItself) {
		Arrays.stream(mLimbs).filter(Objects::nonNull).forEach(l -> {
			l.setInvulnerable(true);
			l.setGlowing(false);
		});

		if (includeBossItself) {
			mBoss.setInvulnerable(true);
			mBoss.setGlowing(false);
		}
	}

	private void makeBossSpotsVulnerable(boolean includeBossItself) {
		Arrays.stream(mLimbs).filter(Objects::nonNull).forEach(l -> {
			l.setInvulnerable(false);
			l.setGlowing(true);
		});

		if (includeBossItself) {
			mBoss.setInvulnerable(false);
			mBoss.setGlowing(true);
		}
	}

	public boolean isCastingDisruptiveSpell() {
		return mCastingDisruptive;
	}

	private void hideBossSpots(int delay) {
		mCastingDisruptive = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			makeBossSpotsInvulnerable(mLimbsDied);
			mPausedVulnerableTimer = true;
		}, delay);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			// Do not make the boss vulnerable again if it was set to invulnerable by something else
			// (for example, vulnerability timer ending)
			makeBossSpotsVulnerable(mLimbsDied && !mIsRespawningLimbs);
			mPausedVulnerableTimer = false;
			mCastingDisruptive = false;
		}, delay + 40);
	}

	private void makeBossVulnerable() {
		mBoss.setInvulnerable(false);
		mBoss.setGlowing(true);
	}

	private void makeBossInvulnerable() {
		mBoss.setInvulnerable(true);
		mBoss.setGlowing(false);
	}

	private void spawnElites() {
		int playerCount = mBoss.getLocation().getNearbyPlayers(60).size();
		if (playerCount >= 3) {
			Location elite2Loc = mBoss.getLocation().clone().add(-3, 1, 5);
			LoSPool.fromString("~DD2_Broodmother_Elite").spawn(elite2Loc);
			new PartialParticle(Particle.TOTEM, elite2Loc, 150).extra(0.6).spawnAsEntityActive(mBoss);
		}
		Location elite1Loc = mBoss.getLocation().clone().add(-3, 1, -5);
		LoSPool.fromString("~DD2_Broodmother_Elite").spawn(elite1Loc);
		new PartialParticle(Particle.TOTEM, elite1Loc, 150).extra(0.6).spawnAsEntityActive(mBoss);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getBossSpellName() != null) {
			event.setDamage(DepthsParty.getAscensionScaledDamage(event.getDamage(), mParty));
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getDamager() instanceof Player) {
			double damageCap = 0.25 * EntityUtils.getMaxHealth(mBoss);
			if (event.getDamage() > damageCap) {
				event.setDamage(damageCap);
			}
		}
	}

	public static double getVulnerabilityAmount(@Nullable DepthsParty party) {
		double amount = 0;
		if (party != null) {
			if (party.getAscension() >= 4) {
				amount += VULNERABILITY_A4_INCREASE;
			}
			if (party.getAscension() >= 8) {
				amount += VULNERABILITY_A8_INCREASE;
			}
			if (party.getAscension() >= 15) {
				amount += VULNERABILITY_A15_INCREASE;
			}
		}
		return amount;
	}
}

