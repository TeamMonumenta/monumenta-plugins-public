package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.MinionSpawn;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SilverBolts;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellConstructAggro;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellCrash;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellEchoCharge;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellFinalStandPassive;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellFloor;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellLingeringParadox;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellRecover;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellRush;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellSlice;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellSteelboreSpread;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellStonemason;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ImperialConstruct extends BossAbilityGroup {

	public static final String identityTag = "boss_imperialconstruct";
	public static final int detectionRange = 50;
	private static final String START_TAG = "Construct_Center";
	private static final String PHASE_TWO_TAG = "Construct_PhaseTwo";
	private static final String PHASE_THREE_TAG = "Construct_PhaseThree";
	private LivingEntity mStart;
	private int mHealth = 22500;

	private final Location mSpawnLoc;
	private final Location mEndLoc;
	//Changes based on the current phase
	private Location mCurrentLoc;
	//private int mPhase = 1;
	private Location mPhase2Loc;
	private Location mPhase3Loc;
	public @Nullable SpellLingeringParadox mParadox;
	public @Nullable SpellLingeringParadox mParadox2;
	public @Nullable SpellLingeringParadox mParadox3;
	private SpellCrash mCrash;
	private SpellRush mRush;
	private SpellRush mRush2;
	private SpellRecover mRecover;
	private MinionSpawn mSpawner;
	private SpellFloor mFloor;
	private SpellSlice mSlice;
	private SpellSteelboreSpread mSpread;
	private SpellSteelboreSpread mSpread2;
	private SpellSteelboreSpread mSpreadSmall;
	private String mEncounterType;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new ImperialConstruct(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}


	public ImperialConstruct(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		boss.setRemoveWhenFarAway(false);
		boss.addScoreboardTag("Boss");

		for (LivingEntity e : EntityUtils.getNearbyMobs(mSpawnLoc, 75, EnumSet.of(EntityType.ARMOR_STAND))) {
			Set<String> tags = e.getScoreboardTags();
			for (String tag : tags) {
				switch (tag) {
					default:
						break;
					case START_TAG:
						mStart = e;
						break;
					case PHASE_TWO_TAG:
						mPhase2Loc = e.getLocation();
						break;
					case PHASE_THREE_TAG:
						mPhase3Loc = e.getLocation();
						break;
				}
			}
		}

		for (Player p : PlayerUtils.playersInRange(mSpawnLoc, 75, true)) {
			if (p.getScoreboardTags().contains("SKTQuest")) {
				mEncounterType = "Story";
				break;
			} else if (p.getScoreboardTags().contains("SKTHard")) {
				mEncounterType = "Hard";
				break;
			} else {
				mEncounterType = "Normal";
				break;
			}
		}
		mCurrentLoc = mStart.getLocation();

		if (mEncounterType.equals("Hard")) {
			mHealth = 22500;
			// Hard Mode Abilities
			mParadox = new SpellLingeringParadox(boss, mSpawnLoc, 30);
			mParadox2 = new SpellLingeringParadox(boss, mPhase2Loc, 30);
			mParadox3 = new SpellLingeringParadox(boss, mPhase3Loc, 30);
			mCrash = new SpellCrash(boss, plugin, mCurrentLoc);
			mRush = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRush2 = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRecover = new SpellRecover(boss, mCurrentLoc);
			mSpawner = new MinionSpawn(boss, mCurrentLoc, 20 * 8, 2);
			mFloor = new SpellFloor(plugin, boss, 5, mCurrentLoc);
			mSlice = new SpellSlice(boss, plugin, mCurrentLoc);
			mSpread = new SpellSteelboreSpread(plugin, boss, 11, mSpawnLoc, 35, 0.75);
			mSpread2 = new SpellSteelboreSpread(plugin, boss, 11, mPhase2Loc, 35, 0.75);
			mSpreadSmall = new SpellSteelboreSpread(plugin, boss, 6, mPhase3Loc, 35, 0.75);

			SpellManager activeSpellsPhase1 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush,
				new SpellStonemason(boss, plugin, mSpawnLoc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 7), (int) (20 * 3))
			));

			SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush2,
				new SpellStonemason(boss, plugin, mPhase2Loc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 7), (int) (20 * 3))
			));

			SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
				new SpellStonemason(boss, plugin, mPhase3Loc, 30),
				new SilverBolts(boss, plugin)
			));

			List<Spell> passiveSpells = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss)
			);

			List<Spell> passiveSpellsPhase3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 16, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 12, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 8, mPhase3Loc)
			);

			Map<Integer, BossHealthAction> events = new HashMap<>();
			events.put(100, (mob) -> {
				mCurrentLoc = mStart.getLocation();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct (Hard)] " + ChatColor.WHITE + "UNAUTHORIZED ENTITY DETECTED: IMPLEMENTING REMOVAL PROCEDURE\",\"color\":\"purple\"}]");
			});

			events.put(90, (mob) -> {
				mSpread.run();
			});

			events.put(80, (mob) -> {
				mSpread.run();
				mParadox.spawnExchanger(mCurrentLoc);
			});

			events.put(75, (mob) -> {
				mParadox.run();
			});

			events.put(70, (mob) -> {
				mSlice.run();
				mSpread.run();
			});

			events.put(66, (mob) -> {
				mCurrentLoc = mPhase2Loc;
				mRush.setLocation(mPhase2Loc);
				mRecover.setLocation(mPhase2Loc);
				mSpawner.setLocation(mPhase2Loc);
				mFloor.setLocation(mPhase2Loc);
				mSlice.setLocation(mPhase2Loc);
				mCrash.run();
				mParadox.spawnExchanger(mCurrentLoc);
				changePhase(activeSpellsPhase2, passiveSpells, null);
			});

			events.put(60, (mob) -> {
				mSlice.run();
				mSpread2.run();
			});

			events.put(50, (mob) -> {
				mParadox2.run();
				mSpread2.run();
			});

			events.put(40, (mob) -> {
				mSpread2.run();
			});

			events.put(33, (mob) -> {
				mCurrentLoc = mPhase3Loc;
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "INTERNAL DAMAGE CRITICAL: REALIGNING CURRENT DIRECTIVE: DESTROY INTRUDERS\",\"color\":\"purple\"}]");
				mCrash.setLocation(mPhase2Loc);
				mRush.setLocation(mPhase3Loc);
				mRecover.setLocation(mPhase3Loc);
				mSpawner.setLocation(mPhase3Loc);
				mFloor.setLocation(mPhase3Loc);
				mSlice.setLocation(mPhase3Loc);
				mCrash.run();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "customeffect @s clear paradox");
				mParadox.spawnExchanger(mCurrentLoc);
				changePhase(finalStandActiveSpells, passiveSpells, null);
			});

			events.put(30, (mob) -> {
				mSlice.setRingMode(true);
				mSlice.run();
				mSpreadSmall.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
			});

			events.put(25, (mob) -> {
				for (Player p : PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true)) {
					Plugin.getInstance().mEffectManager.clearEffects(p, "Paradox");
				}
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TEMPORAL ANOMALY DETECTED: INTRUDERS BEWARE\",\"color\":\"purple\"}]");
				mParadox3.run();
			});

			events.put(20, (mob) -> {
				mSlice.run();
				mSpreadSmall.run();
				mParadox3.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
			});

			events.put(10, (mob) -> {
				mSlice.run();
				mSpreadSmall.run();
				mParadox3.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
			});

			events.put(5, (mob) -> {
				mParadox3.run();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TOMB DEFENSES COMPROMISED: FORGE DEFENSES ACTIVATED\",\"color\":\"purple\"}]");
			});
			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);
		} else if (mEncounterType.equals("Normal")) {
			mHealth = 22500;
			// Normal Mode Abilities
			mParadox = new SpellLingeringParadox(boss, mSpawnLoc, 30);
			mParadox2 = new SpellLingeringParadox(boss, mPhase2Loc, 30);
			mParadox3 = new SpellLingeringParadox(boss, mPhase3Loc, 30);
			mCrash = new SpellCrash(boss, plugin, mCurrentLoc);
			mRush = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRush2 = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRecover = new SpellRecover(boss, mCurrentLoc);
			mSpawner = new MinionSpawn(boss, mCurrentLoc, 20 * 8, 2);
			mFloor = new SpellFloor(plugin, boss, 5, mCurrentLoc);
			mSlice = new SpellSlice(boss, plugin, mCurrentLoc);
			mSpread = new SpellSteelboreSpread(plugin, boss, 8, mSpawnLoc, 35, 0.5);
			mSpread2 = new SpellSteelboreSpread(plugin, boss, 8, mPhase2Loc, 35, 0.5);
			mSpreadSmall = new SpellSteelboreSpread(plugin, boss, 4, mPhase3Loc, 35, 0.5);

			SpellManager activeSpellsPhase1 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush,
				new SpellStonemason(boss, plugin, mSpawnLoc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 7), (int) (20 * 3))
			));

			SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush2,
				new SpellStonemason(boss, plugin, mPhase2Loc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 7), (int) (20 * 3))
			));

			SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
				new SpellStonemason(boss, plugin, mPhase3Loc, 30),
				new SilverBolts(boss, plugin)
			));

			List<Spell> passiveSpells = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss)
			);

			List<Spell> passiveSpellsPhase3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 16, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 12, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 8, mPhase3Loc)
			);

			Map<Integer, BossHealthAction> events = new HashMap<>();
			events.put(100, (mob) -> {
				mCurrentLoc = mStart.getLocation();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct (Hard)] " + ChatColor.WHITE + "UNAUTHORIZED ENTITY DETECTED: IMPLEMENTING REMOVAL PROCEDURE\",\"color\":\"purple\"}]");
			});

			events.put(90, (mob) -> {
				mSpread.run();
			});

			events.put(80, (mob) -> {
				mSpread.run();
				mParadox.spawnExchanger(mCurrentLoc);
			});

			events.put(75, (mob) -> {
				mParadox.run();
			});

			events.put(70, (mob) -> {
				mSlice.run();
				mSpread.run();
			});

			events.put(66, (mob) -> {
				mCurrentLoc = mPhase2Loc;
				mRush.setLocation(mPhase2Loc);
				mRecover.setLocation(mPhase2Loc);
				mSpawner.setLocation(mPhase2Loc);
				mFloor.setLocation(mPhase2Loc);
				mSlice.setLocation(mPhase2Loc);
				mCrash.run();
				mParadox.spawnExchanger(mCurrentLoc);
				changePhase(activeSpellsPhase2, passiveSpells, null);
			});

			events.put(60, (mob) -> {
				mSlice.run();
				mSpread2.run();
			});

			events.put(50, (mob) -> {
				mParadox2.run();
				mSpread2.run();
			});

			events.put(40, (mob) -> {
				mSpread2.run();
			});

			events.put(33, (mob) -> {
				mCurrentLoc = mPhase3Loc;
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "INTERNAL DAMAGE CRITICAL: REALIGNING CURRENT DIRECTIVE: DESTROY INTRUDERS\",\"color\":\"purple\"}]");
				mCrash.setLocation(mPhase2Loc);
				mRush.setLocation(mPhase3Loc);
				mRecover.setLocation(mPhase3Loc);
				mSpawner.setLocation(mPhase3Loc);
				mFloor.setLocation(mPhase3Loc);
				mSlice.setLocation(mPhase3Loc);
				mCrash.run();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "customeffect @s clear paradox");
				mParadox.spawnExchanger(mCurrentLoc);
				changePhase(finalStandActiveSpells, passiveSpells, null);
			});

			events.put(30, (mob) -> {
				mSlice.setRingMode(true);
				mSlice.run();
				mSpreadSmall.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
			});

			events.put(25, (mob) -> {
				for (Player p : PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true)) {
					Plugin.getInstance().mEffectManager.clearEffects(p, "Paradox");
				}
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TEMPORAL ANOMALY DETECTED: INTRUDERS BEWARE\",\"color\":\"purple\"}]");
				mParadox3.run();
			});

			events.put(20, (mob) -> {
				mSlice.run();
				mSpreadSmall.run();
				mParadox3.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
			});

			events.put(10, (mob) -> {
				mSlice.run();
				mSpreadSmall.run();
				mParadox3.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
			});

			events.put(5, (mob) -> {
				mParadox3.run();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TOMB DEFENSES COMPROMISED: FORGE DEFENSES ACTIVATED\",\"color\":\"purple\"}]");
			});
			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);

		} else if (mEncounterType.equals("Story")) {
			mHealth = 10000;
			// Story Mode Abilities
			mCrash = new SpellCrash(boss, plugin, mCurrentLoc);
			mRush = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRush2 = new SpellRush(plugin, boss, mSpawnLoc, 30);
			mRecover = new SpellRecover(boss, mCurrentLoc);
			mSpawner = new MinionSpawn(boss, mCurrentLoc, 20 * 12, 2);
			mFloor = new SpellFloor(plugin, boss, 5, mCurrentLoc);
			mSlice = new SpellSlice(boss, plugin, mCurrentLoc);

			SpellManager activeSpellsPhase1 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush,
				new SpellStonemason(boss, plugin, mSpawnLoc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 10), (int) (20 * 4.5))
			));

			SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
				// Active Spell List
				mRush2,
				new SpellStonemason(boss, plugin, mPhase2Loc, 30),
				new SilverBolts(boss, plugin),
				new SpellEchoCharge(plugin, boss, (int) (20 * 10), (int) (20 * 4.5))
			));

			SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
				new SpellStonemason(boss, plugin, mPhase3Loc, 30),
				new SilverBolts(boss, plugin)
			));

			List<Spell> passiveSpells = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss)
			);

			List<Spell> passiveSpellsPhase3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 16, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 12, mPhase3Loc)
			);

			List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
				// passiveSpells
				new SpellBlockBreak(boss, 2, 2, 2),
				mFloor,
				mRecover,
				mSpawner,
				new SpellConstructAggro(boss),
				new SpellFinalStandPassive(boss, 8, mPhase3Loc)
			);

			Map<Integer, BossHealthAction> events = new HashMap<>();
			events.put(100, (mob) -> {
				mCurrentLoc = mStart.getLocation();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct (Quest)] " + ChatColor.WHITE + "UNAUTHORIZED ENTITY DETECTED: IMPLEMENTING REMOVAL PROCEDURE \",\"color\":\"purple\"}]");
			});

			events.put(70, (mob) -> {
				mSlice.run();
			});

			events.put(66, (mob) -> {
				mCurrentLoc = mPhase2Loc;
				mRush.setLocation(mPhase2Loc);
				mRecover.setLocation(mPhase2Loc);
				mSpawner.setLocation(mPhase2Loc);
				mFloor.setLocation(mPhase2Loc);
				mSlice.setLocation(mPhase2Loc);
				mCrash.run();
				changePhase(activeSpellsPhase2, passiveSpells, null);
			});

			events.put(60, (mob) -> {
				mSlice.run();
			});

			events.put(33, (mob) -> {
				mCurrentLoc = mPhase3Loc;
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "INTERNAL DAMAGE CRITICAL: REALIGNING CURRENT DIRECTIVE: DESTROY INTRUDERS\",\"color\":\"purple\"}]");
				mCrash.setLocation(mPhase2Loc);
				mRush.setLocation(mPhase3Loc);
				mRecover.setLocation(mPhase3Loc);
				mSpawner.setLocation(mPhase3Loc);
				mFloor.setLocation(mPhase3Loc);
				mSlice.setLocation(mPhase3Loc);
				mCrash.run();
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "customeffect @s clear paradox");
				changePhase(finalStandActiveSpells, passiveSpells, null);
			});

			events.put(30, (mob) -> {
				mSlice.setRingMode(true);
				mSlice.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
			});

			events.put(25, (mob) -> {
				for (Player p : PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true)) {
					Plugin.getInstance().mEffectManager.clearEffects(p, "Paradox");
				}
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TEMPORAL ANOMALY DETECTED: INTRUDERS BEWARE\",\"color\":\"purple\"}]");
			});

			events.put(20, (mob) -> {
				mSlice.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
			});

			events.put(10, (mob) -> {
				mSlice.run();
				changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
			});

			events.put(5, (mob) -> {
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Imperial Construct] " + ChatColor.WHITE + "TOMB DEFENSES COMPROMISED: FORGE DEFENSES ACTIVATED\",\"color\":\"purple\"}]");
			});

			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getEvent().getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEvent().getEntity();
			if (player.isBlocking()) {
				// set shield cooldown if boss hits player
				player.setCooldown(Material.SHIELD, 20 * 6);
			}
		}
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, mHealth);
		mBoss.setHealth(mHealth);
		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Silver Construct\",\"color\":\"gold\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Forgotten Defender\",\"color\":\"red\",\"yellow\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death(EntityDeathEvent event) {
		for (Player p : PlayerUtils.playersInRange(event.getEntity().getLocation(), detectionRange, true)) {
			Plugin.getInstance().mEffectManager.clearEffects(p, "Paradox");
		}
		World world = mBoss.getWorld();
		if (event.getEntity().getKiller() != null) {
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mTicks >= 20 * 5) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 10, 0);
						this.cancel();
						mBoss.remove();
						new BukkitRunnable() {
							@Override
							public void run() {

								mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
							}
						}.runTaskLater(mPlugin, 20 * 3);
					}

					if (mTicks % 10 == 0) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
					}
					world.spawnParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1);

					mTicks += 2;
				}
			}.runTaskTimer(mPlugin, 0, 2);
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"" + ChatColor.GOLD + "[Silver Construct] " + ChatColor.WHITE + "PRIME DIRECTIVE FAILED: TOMB HAS BEEN BREACHED. ENABLING FORGE DEFENCES.\",\"color\":\"purple\"}]");
		}


		mSpawner.removeMobs();
		if (mParadox != null) {
			mParadox.deleteExchangers();
		}

	}

}
