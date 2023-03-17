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
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullAway") // so many...
public class ImperialConstruct extends BossAbilityGroup {

	public static final String identityTag = "boss_imperialconstruct";
	public static final int detectionRange = 50;
	private static final String START_TAG = "Construct_Center";
	private static final String PHASE_TWO_TAG = "Construct_PhaseTwo";
	private static final String PHASE_THREE_TAG = "Construct_PhaseThree";
	private LivingEntity mStart;
	private final int mHealth;

	private final Location mSpawnLoc;
	private final Location mEndLoc;
	//Changes based on the current phase
	private Location mCurrentLoc;
	private Location mPhase2Loc;
	private Location mPhase3Loc;
	public @Nullable SpellLingeringParadox mParadox;
	public @Nullable SpellLingeringParadox mParadox2;
	public @Nullable SpellLingeringParadox mParadox3;
	private final SpellCrash mCrash;
	private final SpellRush mRush;
	private final SpellRush mRush2;
	private final SpellRecover mRecover;
	private final MinionSpawn mSpawner;
	private final SpellFloor mFloor;
	private final SpellSlice mSlice;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) ->
			new ImperialConstruct(plugin, boss, spawnLoc, endLoc));
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
					default -> {
					}
					case START_TAG -> mStart = e;
					case PHASE_TWO_TAG -> mPhase2Loc = e.getLocation();
					case PHASE_THREE_TAG -> mPhase3Loc = e.getLocation();
				}
			}
		}

		String mEncounterType = "Normal";
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

		switch (mEncounterType) {
			case "Hard" -> {
				mHealth = 27225;
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

				SpellManager activeSpellsPhase1 = new SpellManager(Arrays.asList(
					mRush,
					new SpellStonemason(boss, plugin, mSpawnLoc, 30, 110),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 7, 20 * 3, 300)
				));

				SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
					mRush2,
					new SpellStonemason(boss, plugin, mPhase2Loc, 30, 110),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 7, 20 * 3, 300)
				));

				SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
					new SpellStonemason(boss, plugin, mPhase3Loc, 30, 110),
					new SilverBolts(boss, plugin)
				));

				List<Spell> passiveSpells = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss)
				);

				List<Spell> passiveSpellsPhase3 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 16, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 12, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
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
					getDialogueAction1().run(mob);
				});

				events.put(90, getSteelboreAction(plugin, true, false));

				events.put(80, (mob) -> {
					getSteelboreAction(plugin, true, false).run(mob);
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
				});

				events.put(75, (mob) -> {
					if (mParadox != null) {
						mParadox.run();
					}
				});

				events.put(70, (mob) -> {
					getSteelboreAction(plugin, true, false).run(mob);
					mSlice.run();
				});

				events.put(66, (mob) -> {
					setSpellLocations(mPhase2Loc);
					mCrash.run();
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
					changePhase(activeSpellsPhase2, passiveSpells, null);
				});

				events.put(60, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, true, false).run(mob);
				});

				events.put(50, (mob) -> {
					if (mParadox2 != null) {
						mParadox2.run();
					}
					getSteelboreAction(plugin, true, false).run(mob);
				});

				events.put(40, getSteelboreAction(plugin, true, false));

				events.put(33, (mob) -> {
					setSpellLocations(mPhase3Loc);
					getDialogueAction2().run(mob);
					mCrash.setLocation(mPhase2Loc);
					mCrash.run();
					PlayerUtils.playersInRange(spawnLoc, detectionRange, true).forEach(this::clearParadox);
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
					changePhase(finalStandActiveSpells, passiveSpells, null);
				});

				events.put(30, (mob) -> {
					mSlice.setRingMode(true);
					mSlice.run();
					getSteelboreAction(plugin, true, true).run(mob);
					changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
				});

				events.put(25, (mob) -> {
					PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true).forEach(this::clearParadox);
					getDialogueAction3().run(mob);
					if (mParadox3 != null) {
						mParadox3.run();
					}
				});

				events.put(20, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, true, true).run(mob);
					if (mParadox3 != null) {
						mParadox3.run();
					}
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
				});

				events.put(10, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, true, true).run(mob);
					if (mParadox3 != null) {
						mParadox3.run();
					}
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
				});

				events.put(5, (mob) -> {
					if (mParadox3 != null) {
						mParadox3.run();
					}
					getDialogueAction4().run(mob);
				});
				BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
				super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);
			}
			case "Normal" -> {
				mHealth = 19000;
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

				SpellManager activeSpellsPhase1 = new SpellManager(Arrays.asList(
					mRush,
					new SpellStonemason(boss, plugin, mSpawnLoc, 30, 70),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 7, 20 * 3, 110)
				));

				SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
					mRush2,
					new SpellStonemason(boss, plugin, mPhase2Loc, 30, 70),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 7, 20 * 3, 110)
				));

				SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
					new SpellStonemason(boss, plugin, mPhase3Loc, 30, 70),
					new SilverBolts(boss, plugin)
				));

				List<Spell> passiveSpells = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss)
				);

				List<Spell> passiveSpellsPhase3 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 16, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 12, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
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
					getDialogueAction1().run(mob);
				});

				events.put(90, getSteelboreAction(plugin, false, false));

				events.put(80, (mob) -> {
					getSteelboreAction(plugin, false, false).run(mob);
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
				});

				events.put(75, (mob) -> {
					if (mParadox != null) {
						mParadox.run();
					}
				});

				events.put(70, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, false, false).run(mob);
				});

				events.put(66, (mob) -> {
					setSpellLocations(mPhase2Loc);
					mCrash.run();
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
					changePhase(activeSpellsPhase2, passiveSpells, null);
				});

				events.put(60, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, false, false).run(mob);
				});

				events.put(50, getSteelboreAction(plugin, false, false));

				events.put(40, getSteelboreAction(plugin, false, false));

				events.put(33, (mob) -> {
					setSpellLocations(mPhase2Loc);
					getDialogueAction2().run(mob);
					mCrash.setLocation(mPhase2Loc);
					mCrash.run();
					PlayerUtils.playersInRange(spawnLoc, detectionRange, true).forEach(this::clearParadox);
					if (mParadox != null) {
						mParadox.spawnExchanger(mCurrentLoc);
					}
					changePhase(finalStandActiveSpells, passiveSpells, null);
				});

				events.put(30, (mob) -> {
					mSlice.setRingMode(true);
					mSlice.run();
					getSteelboreAction(plugin, false, true).run(mob);
					changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
				});

				events.put(25, (mob) -> {
					PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true).forEach(this::clearParadox);
					getDialogueAction3().run(mob);
					if (mParadox3 != null) {
						mParadox3.run();
					}
				});

				events.put(20, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, false, true).run(mob);
					if (mParadox3 != null) {
						mParadox3.run();
					}
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
				});

				events.put(10, (mob) -> {
					mSlice.run();
					getSteelboreAction(plugin, false, true).run(mob);
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
				});

				events.put(5, getDialogueAction4());

				BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
				super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);

			}
			default -> {
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
					mRush,
					new SpellStonemason(boss, plugin, mSpawnLoc, 30, 70),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 10, (int) (20 * 4.5), 110)
				));

				SpellManager activeSpellsPhase2 = new SpellManager(Arrays.asList(
					mRush2,
					new SpellStonemason(boss, plugin, mPhase2Loc, 30, 70),
					new SilverBolts(boss, plugin),
					new SpellEchoCharge(plugin, boss, 20 * 10, (int) (20 * 4.5), 110)
				));

				SpellManager finalStandActiveSpells = new SpellManager(Arrays.asList(
					new SpellStonemason(boss, plugin, mPhase3Loc, 30, 70),
					new SilverBolts(boss, plugin)
				));

				List<Spell> passiveSpells = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss)
				);

				List<Spell> passiveSpellsPhase3 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 16, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part2 = Arrays.asList(
					new SpellBlockBreak(boss, 2, 2, 2),
					mFloor,
					mRecover,
					mSpawner,
					new SpellConstructAggro(boss),
					new SpellFinalStandPassive(boss, 12, mPhase3Loc)
				);

				List<Spell> passiveSpellsPhase3Part3 = Arrays.asList(
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
					getDialogueAction1().run(mob);
				});

				events.put(70, (mob) -> mSlice.run());

				events.put(66, (mob) -> {
					setSpellLocations(mPhase2Loc);
					mCrash.run();
					changePhase(activeSpellsPhase2, passiveSpells, null);
				});

				events.put(60, (mob) -> mSlice.run());

				events.put(33, (mob) -> {
					setSpellLocations(mPhase3Loc);
					getDialogueAction2().run(mob);
					mCrash.setLocation(mPhase2Loc);
					mCrash.run();
					PlayerUtils.playersInRange(spawnLoc, detectionRange, true).forEach(this::clearParadox);
					changePhase(finalStandActiveSpells, passiveSpells, null);
				});

				events.put(30, (mob) -> {
					mSlice.setRingMode(true);
					mSlice.run();
					changePhase(finalStandActiveSpells, passiveSpellsPhase3, null);
				});

				events.put(25, (mob) -> {
					PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true).forEach(this::clearParadox);
					getDialogueAction3().run(mob);
				});

				events.put(20, (mob) -> {
					mSlice.run();
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part2, null);
				});

				events.put(10, (mob) -> {
					mSlice.run();
					changePhase(finalStandActiveSpells, passiveSpellsPhase3Part3, null);
				});

				events.put(5, getDialogueAction4());

				BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
				super.constructBoss(activeSpellsPhase1, passiveSpells, detectionRange, bossBar);
			}
		}
	}

	private void clearParadox(Player p) {
		Plugin.getInstance().mEffectManager.clearEffects(p, "Paradox");
	}

	private BossBarManager.BossHealthAction getSteelboreAction(Plugin plugin, boolean savage, boolean phase3) {
		double damage = savage ? 1 : 0.6;
		int radius = savage ? (phase3 ? 6 : 11) : (phase3 ? 3 : 7);
		return boss -> new SpellSteelboreSpread(plugin, boss, radius, mCurrentLoc, 40, damage).run();
	}

	private Component getMessageComponent(String text) {
		return Component.text("", NamedTextColor.WHITE)
			.append(Component.text("[Silver Construct] ", NamedTextColor.GOLD))
			.append(Component.text(text));
	}

	private BossBarManager.BossHealthAction getDialogueAction(String text) {
		return mBoss -> PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange).sendMessage(getMessageComponent(text));
	}

	private BossBarManager.BossHealthAction getDialogueAction1() {
		return getDialogueAction("UNAUTHORIZED ENTITY DETECTED: IMPLEMENTING REMOVAL PROCEDURE");
	}

	private BossBarManager.BossHealthAction getDialogueAction2() {
		return getDialogueAction("INTERNAL DAMAGE CRITICAL: REALIGNING CURRENT DIRECTIVE: DESTROY INTRUDERS");
	}

	private BossBarManager.BossHealthAction getDialogueAction3() {
		return getDialogueAction("TEMPORAL ANOMALY DETECTED: INTRUDERS BEWARE");
	}

	private BossBarManager.BossHealthAction getDialogueAction4() {
		return getDialogueAction("TOMB DEFENSES COMPROMISED: FORGE DEFENSES ACTIVATED");
	}

	private void setSpellLocations(Location loc) {
		mCurrentLoc = loc;
		mRush.setLocation(loc);
		mRecover.setLocation(loc);
		mSpawner.setLocation(loc);
		mFloor.setLocation(loc);
		mSlice.setLocation(loc);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getEvent().getEntity() instanceof Player player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			if (player.isBlocking()) {
				// set shield cooldown if boss hits player
				player.setCooldown(Material.SHIELD, 20 * 6);
			}
		}
	}

	@Override
	public void init() {
		EntityUtils.setMaxHealthAndHealth(mBoss, mHealth);

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.GOLD + "Silver Construct", ChatColor.RED + "Forgotten Defender");
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event == null) {
			return;
		}
		List<Player> players = PlayerUtils.playersInRange(event.getEntity().getLocation(), detectionRange, true);
		players.forEach(this::clearParadox);
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
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK), 20 * 3);
					}

					if (mTicks % 10 == 0) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
					}
					new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1).spawnAsEntityActive(mBoss);

					mTicks += 2;
				}
			}.runTaskTimer(mPlugin, 0, 2);
			for (Player player : players) {
				player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
				player.sendMessage(getMessageComponent("PRIME DIRECTIVE FAILED: TOMB HAS BEEN BREACHED. ENABLING FORGE DEFENCES."));
			}
		}

		mSpawner.removeMobs();
		if (mParadox != null) {
			mParadox.deleteExchangers();
		}

	}

}
