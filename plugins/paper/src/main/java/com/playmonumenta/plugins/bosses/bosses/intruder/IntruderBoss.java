package com.playmonumenta.plugins.bosses.bosses.intruder;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellAbhorrentHallucination;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellAmalgamatingDreamscape;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellAntiCheese;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellCerebralOnslaught;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellCerebralOutburst;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellCognitiveDistortion;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellEngulfingPsyche;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellFacelessOne;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellIntruderAdvancements;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellLiminalCorruption;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellLingeringScar;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellLucidRend;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellMalevolentConduit;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellNightmareSlash;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellNightmarishCarvings;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellParasomnicMist;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellPsychicMiasma;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellScreamroom;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellSinkingNightmares;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellTwistedReplicants;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellTwistedSwipe;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IntruderBoss extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_intruder";
	public static final TextColor TEXT_COLOR = TextColor.color(0x8b0000);
	public static final int DETECTION_RANGE = 512;
	public static final double MAX_HEALTH = 25000;
	public static final Set<String> DEBUFFS_TO_CLEANSE = Set.of(SpellAmalgamatingDreamscape.ANTIHEAL_SOURCE,
		SpellAmalgamatingDreamscape.WEAKNESS_SOURCE,
		SpellCognitiveDistortion.SLOWNESS_SOURCE,
		SpellCognitiveDistortion.WEAKNESS_SOURCE,
		SpellCognitiveDistortion.ANTI_HEAL_SOURCE,
		SpellScreamroom.DEBUFF_ID);
	private static final int GAZE_SUMMON_INTERVAL = 15;
	private static final int GAZE_COUNT = 3;

	public static final String DEAD_TAG = "TwistedIntruderDead";
	public static final String STALKER_ACTIVE_TAG = "AntumbralStalkingActive";

	private final ArmorStand mSeenCutscene;
	private final ArmorStand mAllDead;
	private final ArmorStand mRespawnArena;
	private final ArmorStand mEnableDetectionCircle;

	private final SpellScreamroom mScreamroom;
	private final SpellLiminalCorruption mLiminalCorruption;
	private final SpellMalevolentConduit mMalevolentConduit;
	private final SpellNightmareSlash mNightmareSlash;
	private final SpellNightmarishCarvings mNightmarishCarvings;
	private final SpellAbhorrentHallucination mAbhorrentHallucination;
	private final SpellCognitiveDistortion mShadowRealmClone;
	private final SpellTwistedReplicants mTwistedReplicants;
	private final SpellAmalgamatingDreamscape mAmalgamatingDreamscape;
	private final SpellIntruderAdvancements mIntruderAdvancements;
	private final SpellEngulfingPsyche mEngulfingPsyche;
	private final SpellLingeringScar mLiminalScar;
	private final SpellLucidRend mLucidRend;
	private final SpellCerebralOutburst mDesperateCerebralOutburst;
	private final SpellAntiCheese mSpellAntiCheese;
	private final SpellPsychicMiasma mPsychicMiasma;

	private final List<Spell> mSpellsDesperation;
	private final List<Spell> mBaseActiveSpells;
	private final List<Spell> mPassives;

	private final BossBarManager mBossBarManager;
	private final Set<Player> mWarned = new HashSet<>();

	@FunctionalInterface
	public interface Dialogue {
		/**
		 * Functional Interface to run Intruder dialogue
		 *
		 * @param delay            delay in ticks (between each dialogue)
		 * @param messages         messages as List of Strings
		 * @param seed             seed for the dialogue sound (Should use 36 as default)
		 * @param runAfterDialogue runnable to run after dialogue has finished
		 * @param runnableDelay    run after this many ticks passed after the last dialogue finished
		 */
		void dialogue(int delay, List<String> messages, long seed, Runnable runAfterDialogue, int runnableDelay);

		// For one-liner dialogue.
		default void dialogue(String message) {
			dialogue(0, List.of(message), 36, () -> {
			}, 0);
		}

		default void dialogue(int delay, List<String> messages) {
			dialogue(delay, messages, 36, () -> {
			}, 0);
		}

		default void dialogue(int delay, List<String> messages, Runnable runAfterDialogue, int runnableDelay) {
			dialogue(delay, messages, 36, runAfterDialogue, runnableDelay);
		}
	}

	public interface Narration {
		/**
		 * Functional Interface to run Intruder narration
		 *
		 * @param message message as string
		 */
		void narration(String message);
	}

	private @Nullable BukkitRunnable mSoundRunnable;
	private boolean mDefeated = false;
	private boolean mPartyDefeated = false;

	public IntruderBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		// spells
		List<Player> mPlayers = playersInRange(mSpawnLoc, true);
		mPlayers.forEach(player -> player.removeScoreboardTag(DEAD_TAG));
		mPassives = List.of(
			mSpellAntiCheese = new SpellAntiCheese(plugin, boss, spawnLoc),
			mIntruderAdvancements = new SpellIntruderAdvancements(boss),
			new SpellRunAction(() -> {
				if (playersInRange(mBoss.getLocation()).isEmpty()) {
					lossCutscene();
				}
			}),
			new SpellTwistedSwipe(boss, false),
			new SpellCerebralOutburst(plugin, boss, this::dialogue, false)
		);
		mPsychicMiasma = new SpellPsychicMiasma(plugin, boss, spawnLoc);
		mScreamroom = new SpellScreamroom(plugin, boss, spawnLoc.getBlockY(), mIntruderAdvancements::addDistortedPlayer, this::dialogue, this::narration);
		mLiminalCorruption = new SpellLiminalCorruption(plugin, boss, this::dialogue, false);
		mMalevolentConduit = new SpellMalevolentConduit(plugin, boss, this::dialogue, this::narration);
		mNightmareSlash = new SpellNightmareSlash(boss);
		mEngulfingPsyche = new SpellEngulfingPsyche(plugin, boss, spawnLoc);

		mBaseActiveSpells = List.of(
			new SpellCerebralOnslaught(plugin, boss, spawnLoc),
			mNightmarishCarvings = new SpellNightmarishCarvings(plugin, boss, mPlayers, spawnLoc, this::dialogue, this::narration, 0.1),
			mLucidRend = new SpellLucidRend(plugin, boss, this::dialogue),
			mAbhorrentHallucination = new SpellAbhorrentHallucination(plugin, boss, spawnLoc, this::dialogue, () -> forceCastSpell(SpellNightmarishCarvings.class)),
			mTwistedReplicants = new SpellTwistedReplicants(plugin, boss, mPlayers, spawnLoc.getBlockY(), this::dialogue, this::narration),
			new SpellSinkingNightmares(plugin, boss, spawnLoc, this::dialogue),
			new SpellParasomnicMist(plugin, boss, spawnLoc, this::dialogue),
			mShadowRealmClone = new SpellCognitiveDistortion(plugin, boss, mPlayers, spawnLoc, this::dialogue, this::narration),
			mAmalgamatingDreamscape = new SpellAmalgamatingDreamscape(plugin, boss, spawnLoc, this::desperation, this::dialogue, this::narration)
		);

		mSpellsDesperation = List.of(
			new SpellTwistedSwipe(boss, true),
			mDesperateCerebralOutburst = new SpellCerebralOutburst(plugin, boss, this::dialogue, true),
			mLiminalScar = new SpellLingeringScar(plugin, boss, spawnLoc.getY())
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, lBoss -> {
			summonSourcelessGazes();
			SongManager.playBossSong(mPlayers, new SongManager.Song("epic:music.intruder_phase1", SoundCategory.RECORDS, 244.8, true, 1, 1), true, boss, true, 0, 20);
		});
		events.put(97, lBoss -> {
			summonSourcelessGazes();
		});
		events.put(94, lBoss -> {
			summonSourcelessGazes();
		});
		events.put(90, lBoss -> {
			changePassivePhase(addSpell(getPassives(), mPsychicMiasma));
			forceCastSpell(SpellCognitiveDistortion.class);
			mPsychicMiasma.forceOnCooldown();
		});
		events.put(88, lBoss -> {
			summonSourcelessGazes();
		});
		events.put(85, lBoss -> {
			summonSourcelessGazes();
			forceCastSpell(SpellAbhorrentHallucination.class);
		});
		events.put(80, lBoss -> {
			changePhase(new SpellManager(addSpell(getActiveSpells(), mScreamroom)), getPassives(), null);
			mPsychicMiasma.cancel();
			forceCastSpell(SpellScreamroom.class);
		});
		events.put(76, lBoss -> {
			summonSourcelessGazes();
		});
		events.put(75, lBoss -> {
			mLiminalCorruption.forceOnCooldown();
			changePassivePhase(addSpell(getPassives(), mLiminalCorruption));
		});
		events.put(72, lBoss -> {
			summonSourcelessGazes();
		});
		events.put(66, lBoss -> {
			mIntruderAdvancements.checkFacelessOnes();
			changePhase(new SpellManager(addSpell(getActiveSpells(), mMalevolentConduit)), getPassives(), null);
			forceCastSpell(SpellMalevolentConduit.class);
		});
		events.put(60, lBoss -> {
			forceCastSpell(SpellAbhorrentHallucination.class);
		});
		events.put(50, lBoss -> {
			forceCastSpell(SpellTwistedReplicants.class);
			SongManager.playBossSong(mPlayers, new SongManager.Song("epic:music.intruder_phase2", SoundCategory.RECORDS, 328.3, true, 1, 1), true, boss, true, 0, 20);
		});
		events.put(45, lBoss -> {
			mPsychicMiasma.forceCast();
			changePhase(new SpellManager(addSpell(getActiveSpells(), mNightmareSlash)),
				removeSpells(getPassives(), List.of(SpellTwistedSwipe.class, SpellLiminalCorruption.class, SpellPsychicMiasma.class)), null);

			mSoundRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mPlayers.forEach(player -> player.playSound(player, Sound.AMBIENT_BASALT_DELTAS_LOOP, SoundCategory.HOSTILE, 3.0f, 2.0f, 1));
					if (EntityUtils.shouldCancelSpells(mBoss)) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					mPlayers.forEach(player -> player.stopSound(Sound.AMBIENT_BASALT_DELTAS_LOOP));
				}
			};
			/*
			 * 867 is the length of the audio file in ticks
			 * 433 is the length of the pitched up (sped up) audio in ticks.
			 */
			mSoundRunnable.runTaskTimer(plugin, 0, 433);
			forceCastSpell(SpellSinkingNightmares.class);
		});
		events.put(40, lBoss -> {
			forceCastSpell(SpellLucidRend.class);
		});
		events.put(35, lBoss -> {
			forceCastSpell(SpellAbhorrentHallucination.class);
		});
		events.put(30, lBoss -> {
			mActiveSpells.cancelAll();
			mPassives.forEach(Spell::cancel);
			forceCastSpell(SpellParasomnicMist.class);
		});
		events.put(27, lBoss -> {
			forceCastSpell(SpellCognitiveDistortion.class);
		});
		events.put(25, lBoss -> {
			mLucidRend.killLucidRends();

			mSpellAntiCheese.setAmalgamatingDreamscape(true);
			changePhase(new SpellManager(List.of(mAmalgamatingDreamscape)), mPassives, null, 40);
			forceCastSpell(SpellAmalgamatingDreamscape.class);
		});
		events.put(18, lBoss -> {
			mEngulfingPsyche.run();
		});

		events.put(10, lBoss -> {
			changePassivePhase(removeSpells(getPassives(), List.of(SpellTwistedSwipe.class)));
			mEngulfingPsyche.run();
			mLiminalScar.setLastStand();
			mDesperateCerebralOutburst.setLastStand();
		});

		HashMap<String, ArmorStand> markers = EntityUtils.getNearbyMobs(spawnLoc, DETECTION_RANGE, EnumSet.of(EntityType.ARMOR_STAND)).stream().filter(entity ->
			entity.getScoreboardTags().contains("IntruderMarker")).collect(HashMap::new,
			(hashMap, entity) -> hashMap.put(entity.getName(), (ArmorStand) entity),
			HashMap::putAll);
		mSeenCutscene = Objects.requireNonNull(markers.get("Seen Cutscene"));
		mAllDead = Objects.requireNonNull(markers.get("All Dead"));
		mRespawnArena = Objects.requireNonNull(markers.get("Respawn Arena"));
		mEnableDetectionCircle = Objects.requireNonNull(markers.get("Enable Detection Circle"));

		mBossBarManager = new BossBarManager(mBoss, DETECTION_RANGE, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events, false, true);

		// if the fight is on the first start
		if (!isTrue(mSeenCutscene)) { //First Fight Tag Armor Stand
			beginningCutscene();
			triggerMechanic(mSeenCutscene, true);
		} else {
			bossReveal();
			dialogue(0, List.of("I. WILL HAVE TO. PRY MY WAY. OUT."));
			startBoss();
		}
	}

	private void summonSourcelessGazes() {
		mSpawnLoc.getWorld().playSound(mSpawnLoc, Sound.AMBIENT_NETHER_WASTES_MOOD, SoundCategory.PLAYERS, 5.0f, 0.2f, 4);
		new BukkitRunnable() {
			private final List<Player> mPlayers = playersInRange(mSpawnLoc);
			int mCount = 0;

			@Override
			public void run() {
				Location summonLoc = LocationUtils.randomSafeLocationInDonut(mSpawnLoc, 4, 22, location ->
					!location.getBlock().getType().isSolid() && mPlayers.stream().noneMatch(player -> player.getLocation().distance(location) <= SpellFacelessOne.RANGE + 1)
				);
				Entity gaze = Objects.requireNonNull(LibraryOfSoulsIntegration.summon(summonLoc, "SourcelessGaze"));
				summonLoc.getWorld().playSound(summonLoc, Sound.AMBIENT_NETHER_WASTES_MOOD, SoundCategory.PLAYERS, 1.4f, 1.7f, 4);
				summonLoc.getWorld().playSound(summonLoc, Sound.AMBIENT_NETHER_WASTES_MOOD, SoundCategory.PLAYERS, 2.2f, 2.0f, 3);
				summonLoc.getWorld().playSound(summonLoc, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 1.6f, 0.1f, 26);
				EntityUtils.setRemoveEntityOnUnload(gaze);
				new PartialParticle(Particle.SMOKE_LARGE, summonLoc)
					.count(15)
					.extra(0.2)
					.spawnAsBoss();
				mCount++;
				if (mCount >= GAZE_COUNT) {
					this.cancel();
					mIntruderAdvancements.checkFacelessOnes();
				}
			}
		}.runTaskTimer(mPlugin, 0, GAZE_SUMMON_INTERVAL);
	}

	private boolean isTrue(ArmorStand armorStand) {
		return armorStand.getLocation().getBlock().isSolid();
	}

	private void triggerMechanic(ArmorStand armorStand, boolean value) {
		armorStand.getLocation().getBlock().setType(value ? Material.REDSTONE_BLOCK : Material.AIR);
	}

	private void beginningCutscene() {
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mBoss.setGravity(false);
		mBoss.setInvisible(true);

		ItemStack helmet = new ItemStack(Material.AIR);
		ItemStack chestplate = new ItemStack(Material.AIR);
		ItemStack leggings = new ItemStack(Material.AIR);
		ItemStack boots = new ItemStack(Material.AIR);
		EntityEquipment equipment = mBoss.getEquipment();
		if (equipment == null) {
			MMLog.severe("Intruder has no equipment.");
		} else {
			helmet = equipment.getHelmet();
			chestplate = equipment.getChestplate();
			leggings = equipment.getLeggings();
			boots = equipment.getBoots();
		}
		mBoss.getEquipment().clear();

		new PartialParticle(Particle.FLASH, mSpawnLoc).spawnAsBoss();
		ParticleUtils.explodingRingEffect(mPlugin, mSpawnLoc, 25, 1.1, 40, 0.15, loc -> {
			new PartialParticle(Particle.SMOKE_NORMAL, loc).distanceFalloff(30).spawnAsBoss();
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc)
				.data(new Particle.DustTransition(Color.fromRGB(0x6b0000), Color.BLACK, 1.7f))
				.distanceFalloff(30).spawnAsBoss();
		});
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 3.0f, 1.0f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 5.0f, 0.5f);
		ItemStack finalHelmet = helmet;
		ItemStack finalChestplate = chestplate;
		ItemStack finalLeggings = leggings;
		ItemStack finalBoots = boots;
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.SMOKE_LARGE, mSpawnLoc.clone().add(new Vector(0, 1, 0)))
					.count((int) (10 * mTicks / 80.0))
					.delta(0.4, 0.0, 0.4)
					.extra(0.25)
					.spawnAsBoss();
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, mSpawnLoc.clone().add(new Vector(0, 1, 0)))
					.count((int) (15 * mTicks / 80.0))
					.delta(0.3, 0.7, 0.3)
					.data(new Particle.DustTransition(Color.RED, Color.BLACK, 1.5f))
					.spawnAsBoss();

				if (mTicks == 20) {
					dialogue(20, List.of(
						"I.",
						"FOUND.",
						"YOU."
					), () -> {
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> dialogue(2 * 20, List.of(
							"YOU WILL GRANT ME. FREEDOM.",
							"BUT. NOT WHILE I. AM STUCK. IN THIS PLACE.",
							"I. WILL HAVE TO. PRY MY WAY. OUT."
						), () -> {
							if (!mBoss.isValid()) {
								return;
							}
							mBoss.setInvisible(false);

							if (equipment != null) {
								equipment.setHelmet(finalHelmet, true);
								equipment.setChestplate(finalChestplate, true);
								equipment.setLeggings(finalLeggings, true);
								equipment.setBoots(finalBoots, true);
							}
							mBoss.setAI(true);
							mBoss.setInvulnerable(false);
							mBoss.setGravity(true);

							startBoss();
						}), 3 * 20);
					}, 15);
				}

				if (mTicks == 60) {
					this.cancel();
					bossReveal();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void startBoss() {
		playersInRange(mSpawnLoc).forEach(IntruderBoss::clearBossSpecificEffects);
		constructBoss(new SpellManager(mBaseActiveSpells), mPassives, DETECTION_RANGE, mBossBarManager, 40, 1, true);

		mIntruderAdvancements.bossStarted();

		playersInRange(mBoss.getLocation()).forEach(player -> {
			player.showTitle(Title.title(
				Component.text("Twisted ", TextColor.color(0x6b0000), TextDecoration.BOLD).append(Component.text("lxxxxxxx", TextColor.color(0x6b0000), Set.of(TextDecoration.OBFUSCATED, TextDecoration.BOLD))),
				Component.text("The Intruding Nightmare", NamedTextColor.RED, TextDecoration.BOLD)
			));
			player.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.4f, 0.1f);
			player.playSound(mBoss.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, 6.0f, 1.5f);
		});
	}

	private static void clearBossSpecificEffects(Player player) {
		@Nullable
		List<EffectManager.EffectPair> effectPairs = EffectManager.getInstance().getEffectPairs(player);
		if (effectPairs != null) {
			effectPairs.stream()
				.filter(effect ->
					effect.mSource().startsWith("NightmarishCarvings") ||
						DEBUFFS_TO_CLEANSE.contains(effect.mSource())
				)
				.forEach(effectPair -> effectPair.mEffect().clearEffect());
		}
	}

	private void bossReveal() {
		GlowingManager.startGlowing(mBoss, NamedTextColor.BLACK, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);
		new PartialParticle(Particle.SQUID_INK, mSpawnLoc.clone().add(new Vector(0, 1, 0)))
			.count(60)
			.delta(0.5, 0.1, 0.5)
			.extra(0.3)
			.spawnAsBoss();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 5.0f, 2.0f);
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		mBoss.setHealth(MAX_HEALTH);

		triggerMechanic(mEnableDetectionCircle, false);
	}

	@Override
	public void unload() {
		resetBoss();
		super.unload();
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (!mDefeated) {
			mDefeated = true;
			if (event != null) {
				event.setCancelled(true);
				event.setReviveHealth(100);
			}
			mIntruderAdvancements.bossDefeated();
			mActiveSpells.cancelAll();
			getPassives().forEach(Spell::cancel);
			BossUtils.endBossFightEffects(mBoss, playersInRange(mSpawnLoc), 20 * 10, true, false);

			deathCutscene();
		}
	}

	@Override
	public double nearbyEntityDeathMaxRange() {
		return DETECTION_RANGE;
	}

	@Override
	public double maxPlayerDeathRange() {
		return DETECTION_RANGE;
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (mAbhorrentHallucination.getHallucinationActive() && source instanceof Player player) {
			new PartialParticle(Particle.SQUID_INK, mBoss.getLocation())
				.count(15)
				.extra(0.25)
				.spawnAsBoss();
			Location loc = event.getDamagee().getLocation();
			mBoss.getWorld().playSound(loc, Sound.ENTITY_BREEZE_HURT, SoundCategory.HOSTILE, 0.6f, 1.5f);
			mBoss.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.2f, 0.25f);

			if (!mWarned.contains(player)) {
				narration("Your attacks pass through the <obfuscated>lxxxxxxx</obfuscated>'s body.");
				mWarned.add(player);
			}

			new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0)).count(20).delta(0)
				.extra(0.3).spawnAsEntityActive(mBoss);
			event.setCancelled(true);
		}
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		mScreamroom.bossEntityDeathEvent(event);
		mNightmarishCarvings.bossEntityDeathEvent(event);
		mAbhorrentHallucination.bossEntityDeathEvent(event);
		mTwistedReplicants.bossNearbyEntityDeath(event);
		mAmalgamatingDreamscape.bossNearbyDeathEvent(event);
		mIntruderAdvancements.bossNearbyEntityDeath(event);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		// prevent reincarnation not being
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mShadowRealmClone.nearbyPlayerDeath(event);
			if (event.isCancelled()) {
				return;
			}
			mScreamroom.nearbyPlayerDeath(event);
			Player player = event.getPlayer();
			player.addScoreboardTag(DEAD_TAG);

			clearBossSpecificEffects(player);

			if (playersInRange(mBoss.getLocation()).isEmpty()) {
				lossCutscene();
			}
		});
	}

	private void lossCutscene() {
		if (!mPartyDefeated) {
			mPartyDefeated = true;
			mBoss.setAI(false);
			mEngulfingPsyche.cancel();
			mActiveSpells.cancelAll();
			dialogue(2 * 20, List.of("FLEETING MIND...", "NEW. BETTER. CONTROL."), mBoss::remove);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> triggerMechanic(mAllDead, true), 3 * 20);
		}
	}

	public List<Spell> addSpell(List<Spell> activeSpells, Spell spell) {
		List<Spell> result = new ArrayList<>(activeSpells);
		result.add(spell);
		return result;
	}

	public List<Spell> addSpells(List<Spell> activeSpells, List<Spell> spells) {
		List<Spell> result = new ArrayList<>(activeSpells);
		result.addAll(spells);
		return result;
	}

	private List<Spell> removeSpells(List<Spell> spells, List<Class<? extends Spell>> spellsToRemove) {
		List<Spell> result = new ArrayList<>();
		for (Spell spell : spells) {
			if (spellsToRemove.stream().noneMatch(testSpell -> testSpell.equals(spell.getClass()))) {
				result.add(spell);
			}
		}
		return result;
	}

	public void desperation() {
		if (mPartyDefeated) {
			return;
		}
		List<Player> mPlayers = playersInRange(mBoss.getLocation());
		resetBossArena();
		mSpellAntiCheese.setAmalgamatingDreamscape(false);
		mBossBarManager.setBossFog(true);
		SongManager.playBossSong(mPlayers, new SongManager.Song("epic:music.intruder_phase3", SoundCategory.RECORDS, 53, false, 1, 1), true, mBoss, true, 0, 20);

		List<Spell> spells = List.of(
			new SpellBaseSummon(mPlugin, mBoss, 12 * 20, 80, 0, 3, false, true, false,
				() -> 1,
				() -> List.of(LocationUtils.randomSafeLocationInDonut(mSpawnLoc, mEngulfingPsyche.getSafeRadius() - 1, 25, location -> !location.getBlock().isSolid())),
				(loc, times) -> Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, "AwakenedFacelessOne")),
				(mob, loc, ticks) -> {
					if (ticks == 0) {
						new PartialParticle(Particle.FLASH, loc.add(0, 1, 0)).minimumCount(1).spawnAsBoss();
					}
				},
				(mob, loc, ticks) -> {
					new PPCircle(Particle.SOUL_FIRE_FLAME, loc, 3 - (ticks * 3.0 / 80))
						.count(4)
						.delta(0.2)
						.spawnAsBoss();
					if (ticks <= 40) {
						mob.getWorld().playSound(loc, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 3.0f, 1.0f + ticks / 40.0f, 3);
					}
				}
			)
		);
		changePhase(new SpellManager(spells), addSpells(removeSpells(mPassives, List.of(
			SpellTwistedSwipe.class,
			SpellCerebralOutburst.class
		)), mSpellsDesperation), null, 8 * 20);

		dialogue(2 * 20, List.of(
				"THE CONNECTION. IS. WANING.",
				"YET. I STILL REMAIN.",
				"YOUR. MIND. BELONGS TO ME.",
				"SUCCUMB."
			),
			() -> {
				mBoss.setInvulnerable(false);
				mBoss.setAI(true);
			});
		mPlayers.forEach(player -> {
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3.0f, Constants.Note.C4.mPitch, 36);
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 1.8f, Constants.Note.FS4.mPitch, 36);
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 1.9f, Constants.Note.FS5.mPitch, 36);
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, Constants.Note.C5.mPitch, 36);
			player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 3.0f, 0.1f, 4);
		});

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mPlayers.forEach(player -> {
					double[] yawPitch = VectorUtils.vectorToRotation(mBoss.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector()));
					player.setRotation((float) yawPitch[0], (float) yawPitch[1]);
				});
				mTicks++;
				if (mTicks == 4 * 20 && !EntityUtils.shouldCancelSpells(mBoss)) {
					mEngulfingPsyche.prepareBorder();
					mEngulfingPsyche.run();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
		EntityEquipment equipment = mBoss.getEquipment();
		if (equipment != null) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> equipment.setHelmet(equipment.getChestplate(), true), 3 * 20);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> equipment.setHelmet(equipment.getLeggings(), true), 4 * 20);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> equipment.setHelmet(equipment.getBoots(), true), 5 * 20);
		}

		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
	}

	public void deathCutscene() {
		mBoss.setInvulnerable(true);
		mEngulfingPsyche.finishAnimation();
		List<Player> mPlayers = playersInRange(mBoss.getLocation());
		mPlayers.forEach(player -> {
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 1.5f, 28);
			player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 1.0f);
		});
		for (int i = 0; i < 3; i++) {
			new PPCircle(Particle.SQUID_INK, mBoss.getLocation().add(new Vector(0, 0.25, 0)), 0.5)
				.rotateDelta(true).directionalMode(true)
				.delta(1, 0, 0.1)
				.count(40)
				.extra(1.4 + i / 14.0)
				.spawnAsBoss();
		}
		if (mSoundRunnable != null) {
			mSoundRunnable.cancel();
		}
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		dialogue(2 * 20, List.of(
			"THE CONNECTION. HAS CLOSED.",
			"YOU ARE... AWAKE?",
			"I. MUST FIND. ANOTHER HOST.",
			"ANOTHER.",
			"DREAM."
		), () -> {
			mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

			narration("The <obfuscated>lxxxxxxx</obfuscated> retreats deep into the nothingness, as if it never existed.");
			mBoss.remove();

			mPlayers.forEach(player -> {
				player.showTitle(Title.title(
					Component.text("VICTORY", NamedTextColor.RED, TextDecoration.BOLD),
					Component.text("Twisted ", TextColor.color(0x6b0000), TextDecoration.BOLD).append(Component.text("lxxxxxxx", TextColor.color(0x6b0000), Set.of(TextDecoration.OBFUSCATED, TextDecoration.BOLD)))
				));
				player.playSound(mBoss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.1f);
			});
		});
	}

	private void resetBoss() {
		mActiveSpells.cancelAll();
		getPassives().forEach(Spell::cancel);
		mPsychicMiasma.cancel();
		mEngulfingPsyche.cancel();
		mLucidRend.killLucidRends();
		mAbhorrentHallucination.killHallucination();
		mNightmarishCarvings.killDisplays();
		// remove sourceless gaze
		EntityUtils.getNearbyMobs(mSpawnLoc, DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE, entity ->
			entity.getScoreboardTags().contains(SourcelessGazeBoss.identityTag)).forEach(Entity::remove);

		// Clear effects on win
		playersInRange(mSpawnLoc).forEach(IntruderBoss::clearBossSpecificEffects);

		if (mSoundRunnable != null) {
			mSoundRunnable.cancel();
		}
		if (!mDefeated) {
			triggerMechanic(mEnableDetectionCircle, true);
			// Only reset boss arena if you lose as win mech modifies arena
			resetBossArena();
		}
		EntityUtils.getNearbyMobs(mSpawnLoc, DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE, entity -> {
				Set<String> scoreboardTags = entity.getScoreboardTags();
				return EntityUtils.isHostileMob(entity) || scoreboardTags.contains("IronMaiden") || scoreboardTags.contains(SpellCerebralOnslaught.SPAWN_TAG);
			})
			.forEach(Entity::remove);

	}

	private void resetBossArena() {
		triggerMechanic(mRespawnArena, true);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> triggerMechanic(mRespawnArena, false), 1);
	}

	public void narration(String message) {
		playersInRange(mBoss.getLocation()).forEach(player ->
			player.sendMessage(MessagingUtils.fromMiniMessage(String.format("<color:gray><italic>%s</italic></color>", message))));
	}

	public void dialogue(int delay, List<String> messages, long seed) {
		dialogue(delay, messages, seed, () -> {
		}, 0);
	}

	public void dialogue(int delay, List<String> messages) {
		dialogue(delay, messages, 36, () -> {
		}, 0);
	}

	public void dialogue(int delay, List<String> messages, Runnable runAfterDialogue) {
		dialogue(delay, messages, 36, runAfterDialogue, 0);
	}

	public void dialogue(int delay, List<String> messages, Runnable runAfterDialogue, int runnableDelay) {
		dialogue(delay, messages, 36, runAfterDialogue, runnableDelay);
	}

	public void dialogue(int delay, List<String> messages, long seed, Runnable runAfterDialogue, int runnableDelay) {
		new BukkitRunnable() {
			int mSafety = 0;
			int mIndex = 0;

			@Override
			public void run() {
				// safety check
				if (mSafety >= messages.size() + 1 || mIndex >= messages.size()) {
					this.cancel();
					return;
				}
				mSafety++;

				float randomPitch = mIndex == messages.size() - 1 ? FastUtils.randomFloatInRange(0.8f, 1.2f) : FastUtils.randomFloatInRange(1.4f, 2f);

				// send message
				for (Player p : playersInRange(mSpawnLoc, true)) {
					if (p.getWorld().equals(mBoss.getWorld())) {
						String translated = translate(p, messages.get(mIndex));
						p.sendMessage(Component.text(translated, TEXT_COLOR, TextDecoration.BOLD));
						p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 1.5f, randomPitch, seed);
					}
				}

				mIndex++;
				if (mIndex >= messages.size()) {
					this.cancel();
					Bukkit.getScheduler().runTaskLater(mPlugin, runAfterDialogue, runnableDelay);
				}
			}
		}.runTaskTimer(mPlugin, 0, delay);
	}

	// Wrapper to not translate messages in testing
	public static String translate(Player player, String message) {
		if (!Plugin.IS_PLAY_SERVER) {
			return message;
		}
		return TranslationsManager.translate(player, message);
	}

	public static List<Player> playersInRange(Location bossLoc) {
		return playersInRange(bossLoc, false);
	}

	public static List<Player> playersInRange(Location bossLoc, boolean includeDead) {
		List<Player> players = new ArrayList<>();
		PlayerUtils.playersInRange(bossLoc, DETECTION_RANGE, true, true).forEach(player -> {
			if (includeDead || !player.getScoreboardTags().contains(DEAD_TAG)) {
				players.add(player);
			}
		});
		return players;
	}
}
