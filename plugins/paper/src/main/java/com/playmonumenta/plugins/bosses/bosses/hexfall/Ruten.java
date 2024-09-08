package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SequentialSpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellAnimaExpulsion;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellCreepingDeathApply;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellCreepingDeathSpread;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellGenerateRutenSpells;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRagingRoots;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRazorVine;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRingOfThorns;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenAnticheat;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenDialogue;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenRecover;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenSummon;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenSummonTotems;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellRutenThreat;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellSliceOfLife;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellStranglingRoot;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellSurgingDeath;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellToxicFumes;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellVerdantGrasp;
import com.playmonumenta.plugins.bosses.spells.hexfall.ruten.SpellVileBloom;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.effects.hexfall.CreepingDeath;
import com.playmonumenta.plugins.effects.hexfall.InfusedLife;
import com.playmonumenta.plugins.effects.hexfall.Reincarnation;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class Ruten extends SerializedLocationBossAbilityGroup {

	public static final String identityTag = "boss_ruten";
	public static final int detectionRange = 34;
	public static final int arenaHeightY = 75;
	public static final int arenaRadius = 26;
	public static final int matrixCoordsFromCenterOffset = 28;
	public static final int lifeTemplateYOffset = 50;
	public static final int deathTemplateYOffset = 40;
	public static final int mHealth = 77000;
	private final SequentialSpellManager mSpellQueue;
	private final Plugin mMonumentaPlugin;
	private final List<Player> mPlayersStartingFight;
	private Integer mPhase;
	public byte[][] mTendencyMatrix = new byte[56][56];
	public int mDeathCount = 0;

	public Ruten(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mMonumentaPlugin = plugin;
		mPhase = 1;
		mBoss.setRemoveWhenFarAway(false);
		mPlayersStartingFight = new ArrayList<>();

		AnimaTendency tendency;
		for (int i = 0; i < mTendencyMatrix.length; i++) {
			for (int j = 0; j < mTendencyMatrix[0].length; j++) {
				tendency = getTendencyAtLocation(mSpawnLoc.clone().add(i - matrixCoordsFromCenterOffset, -1, j - matrixCoordsFromCenterOffset).getBlock().getLocation());
				if (tendency == AnimaTendency.LIFE) {
					mTendencyMatrix[i][j] = 2;
				} else if (tendency == AnimaTendency.DEATH) {
					mDeathCount++;
					mTendencyMatrix[i][j] = 1;
				} else {
					mTendencyMatrix[i][j] = 0;
				}

			}
		}

		mSpellQueue = new SequentialSpellManager(List.of(
			new SpellGenerateRutenSpells(this)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRutenRecover(spawnLoc, boss),
			new SpellRutenThreat(mBoss, 10 * 20, mSpawnLoc),
			new SpellToxicFumes(arenaRadius + 1, detectionRange, mBoss, mSpawnLoc, 1),
			new SpellRutenSummonTotems(mSpawnLoc, arenaRadius - 1, 1, 20 * 60, mSpawnLoc),
			new SpellCreepingDeathApply(plugin, mBoss, mSpawnLoc),
			new SpellCreepingDeathSpread(this, mSpawnLoc),
			new SpellRutenAnticheat(mBoss, mSpawnLoc)
		);

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

		events.put(66, mBoss -> {
			mPhase = 4; // Phase 1->2 Transition
			mMonumentaPlugin.mEffectManager.addEffect(mBoss, "RutenPhaseTransitionNegateDamage", new NegateDamage(27 * 20, 999999));
			mSpellQueue.clearSpellQueue();
			new SpellGenerateRutenSpells(this).run();
		});

		events.put(33, mBoss -> {
			mPhase = 3;
			mSpellQueue.clearSpellQueue();
			new SpellGenerateRutenSpells(this).run();
		});

		events.put(10, mBoss -> new SpellRutenDialogue(Component.text("How could muddling conquer the soul? Do not see a path... To the mud Ru'Ten must... return. Ye come with, it be decreed...", NamedTextColor.WHITE), 0, mSpawnLoc).run());

		BossBarManager bossBar = new BossBarManager(boss, detectionRange * 2, BarColor.RED, BarStyle.SEGMENTED_6, events);
		super.constructBoss(mSpellQueue, passiveSpells, detectionRange * 2, bossBar, 0, 1);

		Creeper ruten = (Creeper) boss;
		ruten.setMaxFuseTicks(10000);

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).stream().filter(p -> p.getScoreboardTags().contains("RutenFighter")).toList()) {
			mPlayersStartingFight.add(player);
			plugin.mEffectManager.addEffect(player, InfusedLife.GENERIC_NAME, new InfusedLife(20 * 6000));
			plugin.mEffectManager.addEffect(player, Reincarnation.GENERIC_NAME, new Reincarnation(20 * 6000, 1));
		}
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, mHealth);
		mBoss.setHealth(mHealth);

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).stream().filter(p -> p.getScoreboardTags().contains("RutenFighter")).toList()) {
			MessagingUtils.sendBoldTitle(player, Component.text("Ru'Ten", NamedTextColor.GOLD), Component.text("Arcane Abomination", NamedTextColor.RED));
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event == null) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(mMonumentaPlugin, () -> {
			for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
				if (mMonumentaPlugin.mEffectManager.hasEffect(player, Reincarnation.class)) {
					AdvancementUtils.grantAdvancement(player, "monumenta:dungeons/hexfall/ruten_master");
				}

				mMonumentaPlugin.mEffectManager.clearEffects(player, InfusedLife.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(player, CreepingDeath.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(player, Reincarnation.GENERIC_NAME);
			}

			if (!HexfallUtils.getPlayersInRuten(mSpawnLoc).isEmpty()) {
				for (Player player : mPlayersStartingFight) {
					if (player.isOnline() && player.getWorld().equals(mSpawnLoc.getWorld())) {
						player.addScoreboardTag("RutenFighter");
					}
				}
			}

			for (Entity mob : mSpawnLoc.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
				if (mob instanceof LivingEntity livingEntity && EntityUtils.isHostileMob(livingEntity)) {
					mob.remove();
				}
			}

			mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		}, 20);
	}

	@Override
	public double maxEntityDeathRange() {
		return detectionRange * 2;
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		LivingEntity target = event.getEntity();
		if (target.getScoreboardTags().contains("boss_lifetotem")) {
			for (double deg = 0; deg < 360; deg += 2) {
				double cos = FastUtils.cosDeg(deg);
				double sin = FastUtils.sinDeg(deg);
				for (double rad = 0; rad < 15; rad += 0.5) {
					Location correctY = target.getLocation().clone().add(cos * rad, -1, sin * rad);
					correctY.setY(arenaHeightY);
					modifyAnimaAtLocation(correctY, AnimaTendency.LIFE);
				}
			}
		}
		if (target.getScoreboardTags().contains("boss_deathtotem")) {
			for (double deg = 0; deg < 360; deg += 2) {
				double cos = FastUtils.cosDeg(deg);
				double sin = FastUtils.sinDeg(deg);
				for (double rad = 0; rad < 9; rad += 0.5) {
					Location correctY = target.getLocation().clone().add(cos * rad, -1, sin * rad);
					correctY.setY(arenaHeightY);
					modifyAnimaAtLocation(correctY, AnimaTendency.DEATH);
				}
			}
		}
	}

	public enum AnimaTendency {
		DEATH, LIFE, NONE
	}

	public void generateSequentialActiveSpells() {
		Spell summons = new SpellRutenSummon(mSpawnLoc, arenaRadius, 20, 8);

		switch (mPhase) {
			case 1 -> {

				// Phase 1 Movesets (100-75)
				/*
				 * Goal here should be to teach the base mechanics of the fight
				 *
				 * Phase has 2 rotations that alternate between one another.
				 * Fight will start with a random one.
				 */


				List<Spell> conalCleaveRot = new ArrayList<>();
				// Conal Cleave Rotation
				conalCleaveRot.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 2, mSpawnLoc));
				addGrenadeAbility(conalCleaveRot);
				conalCleaveRot.add(summons);
				conalCleaveRot.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(conalCleaveRot);
				conalCleaveRot.add(new SpellSliceOfLife(mMonumentaPlugin, mBoss, 20 * 6, 160, detectionRange, 20 * 12, mSpawnLoc, 60, 0.65));
				addGrenadeAbility(conalCleaveRot);
				conalCleaveRot.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				conalCleaveRot.add(summons);

				List<Spell> dangerDonutRot = new ArrayList<>();
				// Danger Donut Rotation
				dangerDonutRot.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				addGrenadeAbility(dangerDonutRot);
				dangerDonutRot.add(summons);
				dangerDonutRot.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(dangerDonutRot);
				dangerDonutRot.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 2, mSpawnLoc));
				addGrenadeAbility(dangerDonutRot);
				dangerDonutRot.add(new SpellRingOfThorns(mMonumentaPlugin, mBoss, 3, 120, 20 * 7, 4, detectionRange, 20 * 9, mSpawnLoc));
				dangerDonutRot.add(summons);

				List<List<Spell>> rotationPool = new ArrayList<>();
				rotationPool.add(conalCleaveRot);
				rotationPool.add(dangerDonutRot);
				Collections.shuffle(rotationPool);

				// Add randomized spell queue to rotation
				for (List<Spell> rotation : rotationPool) {
					for (Spell spell : rotation) {
						mSpellQueue.addSpellToQueue(spell);
					}
				}
			}
			case 2 -> {
				// Phase 2 Movesets (75 - 25)
				/*
				 * This section should be an endurance test making sure they know how to deal
				 * with every mechanic of the fight
				 *
				 * Will have a randomized "basic" rotation mixed with a "major" rotation in between.
				 * Basic rotations will be similar to phase 1 in that they are more breather phases that still test some minor
				 * mechanics. Will have a bit more variation than in Phase 1.
				 * Major rotations will be skill checks that are extremely lethal. Will require coordination between players
				 * to survive. Should come with unique indicators
				 */

				// Basic Rotations
				// Rotation 1
				List<Spell> rot1 = new ArrayList<>();
				rot1.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(rot1);
				rot1.add(summons);
				rot1.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 5, mSpawnLoc));
				addGrenadeAbility(rot1);
				rot1.add(new SpellVerdantGrasp(mMonumentaPlugin, mBoss, detectionRange, 120, 10 * 20, 3 * 20, 7 * 20, 4, mSpawnLoc));
				addGrenadeAbility(rot1);
				rot1.add(summons);


				// Rotation 2
				List<Spell> rot2 = new ArrayList<>();
				rot2.add(summons);
				rot2.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				addGrenadeAbility(rot2);
				rot2.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(rot2);
				rot2.add(summons);

				// Rotation 3
				List<Spell> rot3 = new ArrayList<>();
				rot3.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 5, mSpawnLoc));
				addGrenadeAbility(rot3);
				rot3.add(summons);
				rot3.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				addGrenadeAbility(rot3);
				rot3.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(rot3);
				rot3.add(summons);

				// Major Rotations
				// Maj Rot 1
				List<Spell> majRot1 = new ArrayList<>();
				majRot1.add(summons);
				majRot1.add(new SpellRingOfThorns(mMonumentaPlugin, mBoss, 3, 120, 20 * 7, 4, detectionRange, 20 * 9, mSpawnLoc));
				addGrenadeAbility(majRot1);
				majRot1.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 2, mSpawnLoc));
				addGrenadeAbility(majRot1);
				majRot1.add(summons);
				majRot1.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(majRot1);
				majRot1.add(new SpellSliceOfLife(mMonumentaPlugin, mBoss, 20 * 6, 160, detectionRange, 20 * 12, mSpawnLoc, 60, 0.65));
				addGrenadeAbility(majRot1);
				majRot1.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				addGrenadeAbility(majRot1);
				majRot1.add(summons);

				// Maj Rot 2
				List<Spell> majRot2 = new ArrayList<>();
				majRot2.add(summons);
				majRot2.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 2, mSpawnLoc));
				addGrenadeAbility(majRot1);
				majRot2.add(new SpellVerdantGrasp(mMonumentaPlugin, mBoss, detectionRange, 120, 10 * 20, 3 * 20, 7 * 20, 4, mSpawnLoc));
				addGrenadeAbility(majRot2);
				majRot2.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 3, 20 * 2, mSpawnLoc));
				addGrenadeAbility(majRot2);
				majRot2.add(new SpellVileBloom(mMonumentaPlugin, mBoss, 20 * 2, 20 * 12, 24, 500, 20 * 9, mSpawnLoc));
				addGrenadeAbility(majRot2);
				majRot1.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 10 * 20, 500, 4 * 20, 6, mSpawnLoc));
				addGrenadeAbility(majRot2);
				majRot2.add(summons);

				// Maj Rot 3
				List<Spell> majRot3 = new ArrayList<>();
				majRot3.add(summons);
				majRot3.add(new SpellVileBloom(mMonumentaPlugin, mBoss, 20 * 2, 20 * 12, 24, 500, 20 * 8, mSpawnLoc));
				majRot3 = addGrenadeAbility(majRot2);
				majRot3.add(new SpellStranglingRoot(mMonumentaPlugin, mBoss, detectionRange, 40, 20 * 2, 0.65f, 6, 10, 20 * 5, 20 * 8, mSpawnLoc));
				addGrenadeAbility(majRot2);
				majRot3.add(new SpellSliceOfLife(mMonumentaPlugin, mBoss, 20 * 6, 160, detectionRange, 20 * 12, mSpawnLoc, 60, 0.65));
				majRot3.add(summons);
				addGrenadeAbility(majRot2);
				List<List<Spell>> basicRotPool = new ArrayList<>();
				basicRotPool.add(rot1);
				basicRotPool.add(rot2);
				basicRotPool.add(rot3);
				Collections.shuffle(basicRotPool);
				List<List<Spell>> majRotPool = new ArrayList<>();
				majRotPool.add(majRot1);
				majRotPool.add(majRot2);
				majRotPool.add(majRot3);
				Collections.shuffle(majRotPool);
				int maxSize = basicRotPool.size();
				if (majRotPool.size() > maxSize) {
					maxSize = majRotPool.size();
				}
				for (int i = 0; i < maxSize; i++) {
					int basicIndex = i;
					if (basicIndex > (basicRotPool.size() - 1)) {
						basicIndex = basicRotPool.size() - i;
					}
					for (Spell spell : basicRotPool.get(basicIndex)) {
						mSpellQueue.addSpellToQueue(spell);
					}

					int majIndex = i;
					if (majIndex > (majRotPool.size() - 1)) {
						majIndex = majRotPool.size() - i;
					}
					for (Spell spell : majRotPool.get(majIndex)) {
						mSpellQueue.addSpellToQueue(spell);
					}
				}
			}
			case 3 -> {
				// Phase 3 Movesets (25 - 0)
				/*
				 * This section should be the ultimate test, constant danger, shorter DPS windows,
				 * and mastery of the cleansing mechanic.
				 */

				List<Spell> fillerSpellsFinal = new ArrayList<>();
				fillerSpellsFinal.add(new SpellRazorVine(mMonumentaPlugin, mBoss, detectionRange, 12 * 20, 500, 6 * 20, 6, mSpawnLoc));
				fillerSpellsFinal.add(new SpellVileBloom(mMonumentaPlugin, mBoss, 20 * 2, 20 * 12, 24, 500, 20 * 8, mSpawnLoc));
				Collections.shuffle(fillerSpellsFinal);
				List<Spell> phase3Casts = new ArrayList<>();
				phase3Casts.add(new SpellRutenDialogue(Component.text("More needed, hah. Nalatia soul knew death. Ru'Ten bring death to ye now! Shrine must be sacred. Wolf must return!", NamedTextColor.WHITE), 0, mSpawnLoc));

				phase3Casts.add(new SpellSurgingDeath(mMonumentaPlugin, mBoss, mSpawnLoc, detectionRange, 20 * 3, 3, 20 * 2, 20 * 3, 20 * 12));
				phase3Casts.add(fillerSpellsFinal.get(0));
				addGrenadeAbility(phase3Casts);
				phase3Casts.add(new SpellSurgingDeath(mMonumentaPlugin, mBoss, mSpawnLoc, detectionRange, 20 * 3, 3, 20 * 2, 20 * 3, 20 * 12));
				phase3Casts.add(fillerSpellsFinal.get(1));
				addGrenadeAbility(phase3Casts);
				phase3Casts.add(new SpellRagingRoots(mMonumentaPlugin, mBoss, detectionRange, 20 * 90, 30, 20 * 2, 20 * 2, 0.65f, 20 * 8, mSpawnLoc, 0));
				phase3Casts.add(new SpellSurgingDeath(mMonumentaPlugin, mBoss, mSpawnLoc, detectionRange, 20 * 3, 3, 20 * 2, 20 * 3, 20 * 12));
				phase3Casts.add(summons);
				for (int mEnrageSurges = 0; mEnrageSurges < 8; mEnrageSurges++) {
					phase3Casts.add(new SpellSurgingDeath(mMonumentaPlugin, mBoss, mSpawnLoc, detectionRange, 20 * 3, 3, 20 * 2, 20 * 3, 20 * 12));
					phase3Casts.add(summons);
				}
				for (Spell spell : phase3Casts) {
					mSpellQueue.addSpellToQueue(spell);
				}
			}
			case 4 -> {
				// Phase 1 -> 2 Transition
				/*
				 * Introduce Surging Death
				 */
				mPhase = 2;

				List<Spell> surgingFiller = new ArrayList<>();
				surgingFiller.add(new SpellRingOfThorns(mMonumentaPlugin, mBoss, 3, 120, 20 * 7, 4, detectionRange, 20 * 12, mSpawnLoc));
				surgingFiller.add(new SpellAnimaExpulsion(mMonumentaPlugin, mBoss, detectionRange, 2.5f, 20 * 7, 20 * 12, mSpawnLoc));
				Collections.shuffle(surgingFiller);
				List<Spell> surgingDeathCasts = new ArrayList<>();
				surgingDeathCasts.add(new SpellRutenDialogue(Component.text("Death beget ye now, still!", NamedTextColor.WHITE), 0, mSpawnLoc));
				surgingDeathCasts.add(new SpellSurgingDeath(mMonumentaPlugin, mBoss, mSpawnLoc, detectionRange, 20 * 3, 4, 20 * 2, 20 * 3, 20 * 3));
				surgingDeathCasts.add(surgingFiller.get(0));
				surgingDeathCasts.add(surgingFiller.get(1));
				surgingDeathCasts.add(new SpellRutenDialogue(Component.text("Ru'Ten need more energy, hmph. Laurey seem essence worthwhile now. Feast!", NamedTextColor.WHITE), 0, mSpawnLoc));

				for (Spell spell : surgingDeathCasts) {
					mSpellQueue.addSpellToQueue(spell);
				}
			}
			default -> {

			}
		}

		// Always append this spell to the queue or it will just repeat the previous
		mSpellQueue.addSpellToQueue(new SpellGenerateRutenSpells(this));

	}

	private List<Spell> addGrenadeAbility(List<Spell> spells) {
		EntityTargets bombTarget = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
		EntityTargets explosionTarget = EntityTargets.GENERIC_PLAYER_TARGET;

		spells.add(new SpellBaseGrenadeLauncher(mMonumentaPlugin, mBoss, Material.POLISHED_BLACKSTONE, true, 20 * 2, 1, 20 * 3, 20 * 5, 0, 0, 30,
			() -> {
				// Targets
				return bombTarget.getTargetsList(mBoss);
			},
			(Location loc) -> {
				// Explosion targets
				return explosionTarget.getTargetsListByLocation(mBoss, loc);
			},
			(LivingEntity bosss, Location loc) -> {
				// Launch aesthetics
			},
			(LivingEntity bosss, Location loc) -> {
				// grenade aesthetics
				new PartialParticle(Particle.SMOKE_LARGE, loc)
					.count(2)
					.spawnAsBoss();
			},
			(LivingEntity bosss, Location loc) -> {
				// Explosion aesthetics
				new PPExplosion(Particle.BLOCK_DUST, mBoss.getLocation())
					.speed(1)
					.count(15)
					.extraRange(0.15, 0.5)
					.data(Material.DEEPSLATE_TILES.createBlockData())
					.spawnAsBoss();

			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				loc.setY(arenaHeightY);
				// Hit action
				for (double deg = 0; deg < 360; deg += 2) {
					double cos = FastUtils.cosDeg(deg);
					double sin = FastUtils.sinDeg(deg);
					for (double rad = 0; rad < 3; rad += 1) {
						Location l = loc.clone().add(cos * rad, 0, sin * rad);
						modifyAnimaAtLocation(l, AnimaTendency.DEATH);
					}
				}
			},
			(Location loc) -> {
				// Ring aesthetics
			},
			(Location loc, int ticks) -> {
				// Ring center aesthetics
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Lingering hit action
			},
			"",
			0.7f
		));

		return spells;
	}

	public static AnimaTendency getTendencyAtLocation(Location loc) {
		if (!loc.getBlock().getType().isAir()) {
			if (loc.getBlock().getType() == loc.clone().subtract(0, lifeTemplateYOffset, 0).getBlock().getType()) {
				return AnimaTendency.LIFE;
			} else if (loc.getBlock().getType() == loc.clone().subtract(0, deathTemplateYOffset, 0).getBlock().getType() || loc.getBlock().getType() == Material.POLISHED_DEEPSLATE) {
				return AnimaTendency.DEATH;
			}
		}

		return AnimaTendency.NONE;
	}

	public static AnimaTendency getTendencyAtPlayer(Player player) {
		Location playerLocation = player.getLocation().clone();
		playerLocation.setY(arenaHeightY);
		return getTendencyAtLocation(playerLocation);
	}

	public static void modifyAnimaAtLocation(Location loc, AnimaTendency type) {
		if (getTendencyAtLocation(loc) == type || loc.getY() != arenaHeightY) {
			return;
		}
		Location blockToCopy = loc.clone();
		switch (type) {
			case LIFE -> blockToCopy = loc.clone().subtract(0, lifeTemplateYOffset, 0);
			case DEATH -> blockToCopy = loc.clone().subtract(0, deathTemplateYOffset, 0);
			case NONE -> blockToCopy = loc.clone();
			default -> {
			}
		}

		Material current = loc.getBlock().getType();
		Material toSet = blockToCopy.getBlock().getType();

		if (!current.isAir()
			&& current != Material.BARRIER
			&& current != Material.BEDROCK
			&& current != Material.POLISHED_DEEPSLATE
			&& !toSet.isAir()
			&& toSet != Material.BEDROCK) {
			loc.getBlock().setType(blockToCopy.getBlock().getType());
		}
	}
}
