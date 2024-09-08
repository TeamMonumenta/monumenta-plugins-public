package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SequentialSpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.*;
import com.playmonumenta.plugins.effects.hexfall.BluePercentDamageDealt;
import com.playmonumenta.plugins.effects.hexfall.DeathImmunity;
import com.playmonumenta.plugins.effects.hexfall.DeathVulnerability;
import com.playmonumenta.plugins.effects.hexfall.LifeImmunity;
import com.playmonumenta.plugins.effects.hexfall.LifeVulnerability;
import com.playmonumenta.plugins.effects.hexfall.Reincarnation;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HyceneaRageOfTheWolf extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_hycenea";
	public static final int detectionRange = 52;
	public static final double centerArenaRadius = 17.5;
	public static final int mHealth = 260000;
	public boolean mSteelAdvancement;
	public boolean mSpellAdvancement;
	public final List<Player> mPlayersStartingFight;
	private final SequentialSpellManager mSpellQueue;
	private final Plugin mMonumentaPlugin;
	private int mPhase;


	public HyceneaRageOfTheWolf(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mMonumentaPlugin = plugin;
		mPhase = 1;
		mBoss.setRemoveWhenFarAway(false);
		EntityUtils.selfRoot(mBoss, 1000000);
		mPlayersStartingFight = new ArrayList<>();
		mSteelAdvancement = true;
		mSpellAdvancement = true;

		mSpellQueue = new SequentialSpellManager(getActiveSpellsByPhase(mPhase));

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

		BossBarManager bossBar = new BossBarManager(boss, detectionRange * 2, BarColor.RED, BarStyle.SEGMENTED_6, events);
		super.constructBoss(mSpellQueue, getPassiveSpellsByPhase(mPhase), detectionRange * 2, bossBar, 0, 1);

		PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).stream().filter(p -> p.getScoreboardTags().contains("HyceneaFighter")).forEach(p -> {
				plugin.mEffectManager.addEffect(p, Reincarnation.GENERIC_NAME, new Reincarnation(20 * 6000, 1));
				mPlayersStartingFight.add(p);
			}
		);
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, mHealth);
		mBoss.setHealth(mHealth);
		mBoss.setGravity(false);
		mBoss.teleport(mSpawnLoc.clone().add(0, 2, 0));

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).stream().filter(p -> p.getScoreboardTags().contains("HyceneaFighter")).toList()) {
			MessagingUtils.sendBoldTitle(player, Component.text("Hycenea", NamedTextColor.GOLD), Component.text("Rage of the Wolf", NamedTextColor.RED));
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event == null) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(mMonumentaPlugin, () -> {
			boolean flawless = !event.getEntity().getWorld().getEntities().stream().filter(entity -> entity.getScoreboardTags().contains("DHFFlawless")).toList().isEmpty();

			for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
				if (mMonumentaPlugin.mEffectManager.hasEffect(p, Reincarnation.class)) {
					AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/hycenea_master");

					if (flawless) {
						AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/hexfall_flawless");
					}
				}

				if (mSteelAdvancement) {
					AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/steel_squad");
				}
				if (mSpellAdvancement) {
					AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/spell_squad");
				}

				mMonumentaPlugin.mEffectManager.clearEffects(p, Reincarnation.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(p, VoodooBindings.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(p, LifeVulnerability.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(p, DeathVulnerability.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(p, LifeImmunity.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(p, DeathImmunity.GENERIC_NAME);
			}


			if (!HexfallUtils.getPlayersInHycenea(mSpawnLoc).isEmpty()) {
				for (Player player : mPlayersStartingFight) {
					if (player.isOnline() && player.getWorld().equals(mSpawnLoc.getWorld())) {
						player.addScoreboardTag("HyceneaFighter");
					}
				}
			}

			for (Entity entity : mSpawnLoc.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
				entity.removeScoreboardTag("Hycenea_TotemicDestruction_Target");
				entity.removeScoreboardTag("Hycenea_TotemicDestruction_ShieldActive");
				entity.removeScoreboardTag("Hycenea_StranglingRupture_Target");
				entity.removeScoreboardTag("Hycenea_StranglingRupture_KillzoneActive");
				entity.removeScoreboardTag("Hycenea_Totem_NoThrow");

				if (entity instanceof LivingEntity livingEntity && (EntityUtils.isHostileMob(livingEntity) || livingEntity instanceof Wolf)) {
					livingEntity.remove();
				}
			}

			mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		}, 20);
	}

	public void addSpellsToQueue() {
		for (Spell spell : getActiveSpellsByPhase(mPhase)) {
			mSpellQueue.addSpellToQueue(spell);
		}
	}

	private List<Spell> getActiveSpellsByPhase(int phase) {

		List<Integer> yawOptions = new ArrayList<>();
		yawOptions.add(90);
		yawOptions.add(180);
		yawOptions.add(270);
		yawOptions.add(360);

		//----------------

		List<Spell> activeSpells = new ArrayList<>();
		switch (phase) {
			case 1 -> {
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Now I... will break thee. Link by pitiful link.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellAllowTotemThrow(mBoss, true));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 5 * 20, mSpawnLoc));
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (e.getScoreboardTags().contains("boss_totemplatform") && e instanceof LivingEntity livingEntity) {
								livingEntity.setHealth(0);
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});


				// Earthen Cleave + AOEs

				List<List<Spell>> earthenRotations = new ArrayList<>();
				List<Spell> earthenRotation1 = new ArrayList<>();
				earthenRotation1.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 5, 20 * 5, yawOptions.get(0)));
				earthenRotation1.add(new SpellHyceneaDialogue(Component.text("Hycenea casts a spell to follow your every move...", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				earthenRotation1.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 3, (yawOptions.get(0) + 180) % 360));
				earthenRotation1.add(new SpellMysticMaelstrom(mMonumentaPlugin, mBoss, detectionRange, 8, 500, 20 * 4, 20 * 5, mSpawnLoc));
				earthenRotations.add(earthenRotation1);
				List<Spell> earthenRotation2 = new ArrayList<>();
				earthenRotation2.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 5, 20 * 5, (yawOptions.get(0) + 90) % 360));
				earthenRotation2.add(new SpellHyceneaDialogue(Component.text("Hycenea casts a spell to mark where you stand...", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				earthenRotation2.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 3, (yawOptions.get(0) + 270) % 360));
				earthenRotation2.add(new SpellAbyssalSigil(mMonumentaPlugin, mBoss, detectionRange, 500, 12, 20 * 4, 20 * 5, mSpawnLoc));
				earthenRotations.add(earthenRotation2);
				Collections.shuffle(earthenRotations);
				activeSpells.addAll(earthenRotations.get(0));
				activeSpells.addAll(earthenRotations.get(1));

				// Voodoo
				activeSpells.add(new SpellHyceneaDialogue(Component.text("You know not how little thy... struggle actually means. I am the trees... I am the world all... around ye. You will not survive.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				List<List<Spell>> voodooSequencesPhase1 = new ArrayList<>();
				List<Spell> voodooSequence1 = new ArrayList<>();
				voodooSequence1.add(new SpellVoodooBindings(mMonumentaPlugin, mBoss, detectionRange, 20 * 5, 20 * 6, mSpawnLoc, getRandomVoodooSet(phase)));
				voodooSequence1.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 4, mSpawnLoc));

				voodooSequence1.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 4, mSpawnLoc));

				voodooSequence1.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence1.add(new SpellVoodooExecution(mMonumentaPlugin, 0, mSpawnLoc));
				voodooSequencesPhase1.add(voodooSequence1);
				List<Spell> voodooSequence2 = new ArrayList<>();
				voodooSequence2.add(new SpellVoodooBindings(mMonumentaPlugin, mBoss, detectionRange, 20 * 5, 20 * 6, mSpawnLoc, getRandomVoodooSet(phase)));
				voodooSequence2.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 4, mSpawnLoc));
				voodooSequence2.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 10, 500, 20 * 2, 20 * 4, mSpawnLoc));
				voodooSequence2.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 0, 5, 20 * 2, mSpawnLoc, true));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 4, 20 * 4, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 2, 20 * 2, mSpawnLoc));
				voodooSequence2.add(new SpellVoodooExecution(mMonumentaPlugin, 0, mSpawnLoc));
				voodooSequencesPhase1.add(voodooSequence2);
				Collections.shuffle(voodooSequencesPhase1);
				activeSpells.addAll(voodooSequencesPhase1.get(0));

				// Totemic Destruction 1
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Hycenea is channeling a massive combined infusion of life and death. Find a way to protect yourselves!", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellAllowTotemThrow(mBoss, true));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 0, mSpawnLoc));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 10 * 20));
				activeSpells.add(new SpellFloralFlechettes(mMonumentaPlugin, mBoss, detectionRange, 18, 90, 4 * 20, 20, 42, true, mSpawnLoc, 4 * 20, 2, 15));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 14 * 20));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 14 * 20));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 18 * 20));

				// Strangling Rupture
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Gah... little... human. I have lived... eons. I have seen the Architect... fall into slumber... I have seen empires crumble into ash... I will not be slain by... someone of thy meager lot.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				List<Spell> stranglingSequence1 = new ArrayList<>();
				stranglingSequence1.add(new SpellMysticMaelstrom(mMonumentaPlugin, mBoss, 50, 8, 500, 20 * 8, 20 * 4, mSpawnLoc));
				stranglingSequence1.add(new SpellAbyssalSigil(mMonumentaPlugin, mBoss, detectionRange, 500, 9, 20 * 8, 20 * 4, mSpawnLoc));
				stranglingSequence1.add(new SpellMysticMaelstrom(mMonumentaPlugin, mBoss, 50, 8, 500, 20 * 8, 20 * 4, mSpawnLoc));
				stranglingSequence1.add(new SpellAbyssalSigil(mMonumentaPlugin, mBoss, detectionRange, 500, 9, 20 * 8, 20 * 4, mSpawnLoc));
				List<Spell> stranglingSequence2 = new ArrayList<>();
				stranglingSequence2.add(new SpellAbyssalSigil(mMonumentaPlugin, mBoss, detectionRange, 500, 9, 20 * 8, 20 * 8, mSpawnLoc));
				stranglingSequence2.add(new SpellMysticMaelstrom(mMonumentaPlugin, mBoss, 50, 8, 500, 20 * 8, 20 * 8, mSpawnLoc));
				stranglingSequence2.add(new SpellAbyssalSigil(mMonumentaPlugin, mBoss, detectionRange, 500, 9, 20 * 8, 20 * 8, mSpawnLoc));
				stranglingSequence2.add(new SpellMysticMaelstrom(mMonumentaPlugin, mBoss, 50, 8, 500, 20 * 8, 20 * 8, mSpawnLoc));
				List<List<Spell>> stranglingSequences = new ArrayList<>();
				stranglingSequences.add(stranglingSequence1);
				stranglingSequences.add(stranglingSequence2);
				Collections.shuffle(stranglingSequences);
				activeSpells.add(new SpellAllowTotemThrow(mBoss, false));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 0, mSpawnLoc));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 18, 500, 20 * 7, 20 * 7, mSpawnLoc));
				activeSpells.add(new SpellDestroyCenterPlatform(mSpawnLoc, 0, 18, 0));
				activeSpells.add(stranglingSequences.get(0).get(0));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 8, 9, 9, 20 * 3, mSpawnLoc, 20 * 12));
				activeSpells.add(stranglingSequences.get(0).get(1));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 8, 9, 9, 20 * 3, mSpawnLoc, 20 * 12));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Hah... bah.... Ha... ha ha... HA HA HA HA HA HA.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(stranglingSequences.get(0).get(2));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 8, 9, 9, 20 * 3, mSpawnLoc, 20 * 12));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("A deafening crack shakes the arena. You see the Blue Wool emerge from your bag and levitate, slowly, into Hycenea's hands. She smiles.", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(stranglingSequences.get(0).get(3));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 8, 9, 9, 20 * 3, mSpawnLoc, 20 * 5));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Hycenea casts her fingers over the wool and seems as if she's weaving the very threads of reality.", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaArena", detectionRange, new Vector(0, -1, 0), 1, 3000));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 17, 500, 20 * 3, 20 * 6, mSpawnLoc));

				// Transition
				activeSpells.add(new SpellHyceneaDialogue(Component.text("A deafening roar shakes the ground below you. You feel the energy of the fractured leyline pull you into the Blue Wool...", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellBlueTransition(mSpawnLoc, 0));
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (e.getScoreboardTags().contains("boss_totemplatform")) {
								e.remove();
							}
							if (e.getScoreboardTags().contains("Hycenea_Island")) {
								e.removeScoreboardTag("Hycenea_StranglingRupture_Target");
								e.removeScoreboardTag("Hycenea_TotemicDestruction_Target");
								e.removeScoreboardTag("Hycenea_TotemicDestruction_ShieldActive");

								e.addScoreboardTag("Hycenea_StranglingRupture_KillzoneActive");
								GrowableAPI.grow("stranglingRupture1", e.getLocation().add(0, -1, 0), 1, 1000, true);
							}
							if (e instanceof LivingEntity entity && (EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && !entity.getScoreboardTags().contains("Boss")) {
								entity.remove();
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaBlueArena", detectionRange, new Vector(0, -1, 0), 0, 100));
				activeSpells.add(new SpellSetHyceneaPhase(this, mPhase + 1, 20));
			}
			case 3 -> {
				activeSpells.add(new SpellDestroyCenterPlatform(mSpawnLoc, 0, 18, 0));
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaArena", detectionRange, new Vector(0, -1, 0), 0, 3000));
				activeSpells.add(new SpellAllowTotemThrow(mBoss, true));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 0, mSpawnLoc));

				// Cascades 1
				activeSpells.add(new SpellCascadingHex(mMonumentaPlugin, mBoss, detectionRange, 5, 1000, 20 * 20, 20 * 10, 20 * 20, 20 * 5, 20 * 5, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("It’s... it’s you. It must be. You’ve wounded him. You’ve... taken my... love’s mind from him. You vile thing. You creature of the Knights... you foolish pawn... you... YOU... ", NamedTextColor.WHITE), 20 * 10, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("You see tendrils of Blue light begin to wrap themselves around Hycenea.", NamedTextColor.GRAY, TextDecoration.ITALIC), 20 * 13, mSpawnLoc, false));

				// Layered Flechettes + Organic
				activeSpells.add(new SpellFloralFlechettes(mMonumentaPlugin, mBoss, detectionRange, 18, 90, 3 * 20, 20, 12, true, mSpawnLoc, 0, 2, 0));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 3, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("PITIFUL WORM. I WILL CAST YOU INTO NOTHINGNESS. NOT EVEN THE BEYOND WILL HARBOR YOUR MANGLED SOUL.", NamedTextColor.BLUE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 3, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 3, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("YOU AROSE FROM MY CONSORT’S VERY FOOTSTEPS. THE TREAD OF HIS PAW SPAWNED YOUR VILE RACE. YOU ARE... NOTHING.", NamedTextColor.BLUE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 3, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 10, 18, 500, 20 * 3, 20 * 6, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("I ALONE HAVE WATCHED AS YOU MANGLED THE CHILDREN OF MY BELOVED.", NamedTextColor.BLUE), 0, mSpawnLoc, true));

				// Mortal Chains 1
				Collections.shuffle(yawOptions);
				activeSpells.add(new SpellMortalChains(mMonumentaPlugin, mBoss, detectionRange, 5, 1000, 20 * 8, 20 * 30, 20 * 8, 22, 20 * 6, 20 * 10, mSpawnLoc));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(0)));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(1)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("THOUSANDS HAVE...  THOUSANDS HAVE YOU SLAIN.", NamedTextColor.BLUE).append(Component.text(" this... rage... Love, I...", NamedTextColor.WHITE)).append(Component.text(" THOUSANDS HAVE YOU SLAIN.", NamedTextColor.BLUE)), 0, mSpawnLoc, true));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(2)));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(3)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("I WILL FREE THIS PLANE FROM YOUR VICIOUS PRESENCE.", NamedTextColor.BLUE), 2 * 20, mSpawnLoc, true));

				// Strangling Rupture + Voodoo 2
				activeSpells.add(new SpellAllowTotemThrow(mBoss, false));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 0, mSpawnLoc));
				activeSpells.add(new SpellVoodooBindings(mMonumentaPlugin, mBoss, detectionRange, 20 * 5, 20 * 6, mSpawnLoc, getRandomVoodooSet(phase)));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 18, 500, 20 * 7, 20 * 7, mSpawnLoc));
				activeSpells.add(new SpellDestroyCenterPlatform(mSpawnLoc, 0, 18, 0));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 5, 5, 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 9, 9, 20 * 3, mSpawnLoc, 20 * 6));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 5, 5, 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 9, 9, 20 * 3, mSpawnLoc, 20 * 6));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 5, 5, 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 9, 9, 20 * 3, mSpawnLoc, 20 * 8));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("He is... he is barely himself... his...", NamedTextColor.WHITE).append(Component.text(" HIS CHAINS HAVE BOUND HIM FOR COUNTLESS CENTURIES. HE THRASHES. HE ACHES. HE HOWLS FOR YOUR BLOOD.", NamedTextColor.BLUE)), 0, mSpawnLoc, true));
				activeSpells.add(new SpellVoodooExecution(mMonumentaPlugin, 0, mSpawnLoc));
				activeSpells.add(new SpellStranglingRupture(mMonumentaPlugin, mBoss, detectionRange, 20 * 8, 9, 9, 20 * 3, mSpawnLoc, 20 * 5));
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaArena", detectionRange, new Vector(0, -1, 0), 1, 3000));
				activeSpells.add(new SpellOrganicShock(mMonumentaPlugin, mBoss, 0, 18, 500, 20 * 3, 20 * 6, mSpawnLoc));

				// Transition
				activeSpells.add(new SpellHyceneaDialogue(Component.text("A deafening roar shakes the ground below you. You feel the energy of the fractured leyline pull you into the Blue Wool once again...", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellBlueTransition(mSpawnLoc, 0));
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (e.getScoreboardTags().contains("boss_totemplatform")) {
								e.remove();
							}
							if (e.getScoreboardTags().contains("Hycenea_Island")) {
								e.removeScoreboardTag("Hycenea_StranglingRupture_Target");
								e.removeScoreboardTag("Hycenea_TotemicDestruction_Target");
								e.removeScoreboardTag("Hycenea_TotemicDestruction_ShieldActive");

								e.addScoreboardTag("Hycenea_StranglingRupture_KillzoneActive");
								GrowableAPI.grow("stranglingRupture1", e.getLocation().add(0, -1, 0), 1, 1000, true);
							}

							if (e instanceof LivingEntity entity && (EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && !entity.getScoreboardTags().contains("Boss")) {
								entity.remove();
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaBlueArena", detectionRange, new Vector(0, -1, 0), 0, 100));
				activeSpells.add(new SpellSetHyceneaPhase(this, mPhase + 1, 20));
			}
			case 5 -> {
				activeSpells.add(new SpellDestroyCenterPlatform(mSpawnLoc, 0, 18, 0));
				activeSpells.add(new SpellGrowableAtMarker(mBoss, "Hycenea_Center", "hyceneaArena", detectionRange, new Vector(0, -1, 0), 0, 3000));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("No... no... my love... what are you doing to me?!", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellAllowTotemThrow(mBoss, true));
				activeSpells.add(new SpellHyceneaSummonTotemPlatforms(detectionRange, 0, mSpawnLoc));

				// Totemic & Voodoo
				activeSpells.add(new SpellVoodooBindings(mMonumentaPlugin, mBoss, detectionRange, 20 * 5, 20 * 6, mSpawnLoc, getRandomVoodooSet(phase)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Hycenea’s eyes blink and refocus, the tendrils of blue light now unraveled from her newly frail-looking form. She speaks not to you, but to herself.", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("I see it now... That feeling... that fury... My love... he is... he is gone.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 9 * 20));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("The millenia in... chains... his mind has been destroyed... The Wolf that... once roamed the Woods is... gone.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 9 * 20));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("This beast... skulking within is all... left for me to... reclaim.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 9 * 20));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Hycenea looks exhausted and conflicted inside.", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellCommandingIncantation(mMonumentaPlugin, mBoss, detectionRange, 20 * 10, 20 * 7, 5, 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellTotemicDestruction(mMonumentaPlugin, mBoss, detectionRange, 14 * 20, mSpawnLoc, 9 * 20));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("I find ye vile. I despise ye wholly for thy lies and deceptions.", NamedTextColor.WHITE), 6 * 20, mSpawnLoc, true));
				activeSpells.add(new SpellVoodooExecution(mMonumentaPlugin, 0, mSpawnLoc));

				// Mortal Chains 2
				Collections.shuffle(yawOptions);
				activeSpells.add(new SpellMortalChains(mMonumentaPlugin, mBoss, detectionRange, 5, 1000, 20 * 8, 20 * 30, 20 * 8, 22, 20 * 6, 20 * 5, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("But my lover... I need to be with him. I... need to restore him. I can't... unleash this monster out into the world.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellCascadingHex(mMonumentaPlugin, mBoss, detectionRange, 5, 1000, 20 * 20, 20 * 10, 20 * 20, 20 * 5, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(0)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("I need your power. No matter... your nature. He is soon to reemerge. Beat him back.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(1)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("When the time is right, I will... I will take on his power. Then, you must close... us both within that... thing.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(2)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Oh Love... if you must be caged forever... at least let me comfort you in your prison...", NamedTextColor.WHITE), 0, mSpawnLoc, true));
				activeSpells.add(new SpellEarthenCleave(mMonumentaPlugin, mBoss, mSpawnLoc, centerArenaRadius, 500, 20 * 7, 20 * 7, yawOptions.get(3)));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("Steel yourself... He arrives in frenzied... fury...", NamedTextColor.WHITE), 5 * 20, mSpawnLoc, true));

				// Enrage
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (e.getScoreboardTags().contains("boss_totemplatform") && e instanceof LivingEntity livingEntity) {
								livingEntity.setHealth(0);
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new SpellReunion(mMonumentaPlugin, mBoss, detectionRange, (int) centerArenaRadius + 1, 20 * 4, 20 * 32, 20 * 2, 20 * 3, mSpawnLoc));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("My love... share your burden with me.", NamedTextColor.WHITE), 20, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("The tendrils begin to reach out to Hycenea again.", NamedTextColor.GRAY, TextDecoration.ITALIC), 20 * 2, mSpawnLoc, false));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("They took your body from you... the least I can do... is offer myself.", NamedTextColor.WHITE), 20 * 4, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("The tendrils spear into Hycenea. She grows and morphs before your eyes, and speaks with two voices in one.", NamedTextColor.GRAY, TextDecoration.ITALIC), 20 * 4, mSpawnLoc, false));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("LOVERS BORN AT AGE’S END", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("CLEFT APART BY SILVER’S CHAINS", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("ROOT AND LEAF MADE ONE AGAIN", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("SUCCORED NOW ON NAUGHT BUT PAINS", NamedTextColor.BLUE), 20 * 4, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("ONE, THEN TWO, THEN ONE AGAIN", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("PUPPETED BY HIS DESIGN", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new SpellHyceneaDialogue(Component.text("NOW TOGETHER WE MAY REST", NamedTextColor.BLUE), 20 * 2, mSpawnLoc, true));
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
							AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/cage_match");
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new SpellHyceneaDialogue(Component.text("CAGED ONCE MORE, BUT INTERTWINED", NamedTextColor.BLUE), 600 * 20, mSpawnLoc, true));
			}
			case 6, 7 -> {
				activeSpells.add(new SpellHyceneaDialogue(Component.text("As you strike Harrakfar, he unleashes a maddened, hideous roar and forces you back out of his prison" + (mPhase == 6 ? "." : " once more."), NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity mob : mSpawnLoc.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (mob instanceof LivingEntity entity && (EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && !entity.getScoreboardTags().contains("boss_hycenea")) {
								mob.teleport(mob.getLocation().clone().add(0, -10, 0));
								mob.remove();
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
							if (e.getScoreboardTags().contains("Hycenea_Island")) {
								e.removeScoreboardTag("Hycenea_StranglingRupture_KillzoneActive");
							}
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new Spell() {
					@Override
					public void run() {
						for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
							mMonumentaPlugin.mEffectManager.clearEffects(player, BluePercentDamageDealt.GENERIC_NAME);
						}
					}

					@Override
					public int cooldownTicks() {
						return 0;
					}
				});
				activeSpells.add(new Spell() {
					final int ANIM_TICKS = 60;

					@Override
					public void run() {
						List<Entity> mFindHarrakfar = mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange).stream().filter(entity -> entity.getScoreboardTags().contains("boss_harrakfar")).limit(1).toList();
						if (!mFindHarrakfar.isEmpty() && mFindHarrakfar.get(0) instanceof LivingEntity harrakfar) {
							BukkitRunnable runnable = new BukkitRunnable() {
								int mT = 0;
								final double mToHeal = mHealth - harrakfar.getHealth();

								@Override
								public void run() {
									harrakfar.setHealth(Math.min(harrakfar.getHealth() + mToHeal / ANIM_TICKS, EntityUtils.getMaxHealth(harrakfar)));
									if (mT++ >= ANIM_TICKS) {
										this.cancel();
									}
								}
							};
							runnable.runTaskTimer(mPlugin, 0, 1);
							mActiveRunnables.add(runnable);
						}
					}

					@Override
					public int cooldownTicks() {
						return ANIM_TICKS;
					}
				});
				activeSpells.add(new SpellBlueTransition(mSpawnLoc, 0));
				activeSpells.add(new SpellSetHyceneaPhase(this, mPhase == 6 ? 3 : 5, 20));
			}
			default -> {
			}
		}

		activeSpells.add(new SpellGenerateHyceneaSpells(this));

		return activeSpells;
	}

	private List<Spell> getPassiveSpellsByPhase(int phase) {
		List<Spell> passiveSpells = new ArrayList<>();
		switch (phase) {
			case 1, 3, 5 -> {
				passiveSpells.add(new SpellHyceneaRecover(mSpawnLoc, mBoss));
				passiveSpells.add(new SpellPassiveStranglingKillzones(mSpawnLoc, mBoss));
				passiveSpells.add(new SpellPassiveTotemicShields(mMonumentaPlugin, mSpawnLoc, mBoss));
			}
			case 2, 4 -> {
				passiveSpells.add(new SpellPassiveStranglingKillzones(mSpawnLoc, mBoss));
				passiveSpells.add(new SpellSummonManageBlue(mMonumentaPlugin, mSpawnLoc, this));
			}
			case 6, 7 -> passiveSpells.add(new SpellHyceneaRecover(mSpawnLoc, mBoss));
			default -> {

			}
		}
		passiveSpells.add(new SpellHyceneaLeyline(mSpawnLoc, mBoss, detectionRange * 2));
		passiveSpells.add(new SpellHyceneaAnticheat(this, mBoss, mSpawnLoc));

		return passiveSpells;
	}

	public void setPhase(int phase) {
		mPhase = phase;
		mSpellQueue.clearSpellQueue();
		addSpellsToQueue();
		changePassivePhase(getPassiveSpellsByPhase(mPhase));
		positionHycenea(mPhase);
	}

	public int getPhase() {
		return mPhase;
	}

	public void positionHycenea(int phase) {
		switch (phase) {
			case 1, 3, 5 -> {
				mBoss.teleport(mSpawnLoc.clone().add(0, 2, 0));
				mBoss.setInvisible(false);
				mBoss.setInvulnerable(false);
			}
			case 2, 4 -> {
				mBoss.teleport(mSpawnLoc.clone().add(0, 10, 0));
				mBoss.setInvisible(true);
				mBoss.setInvulnerable(true);
			}
			default -> {

			}
		}
	}

	private List<String> getRandomVoodooSet(int phase) {
		List<List<String>> phaseVoodooSets = new ArrayList<>();
		switch (phase) {
			case 1 -> {
				List<String> voodooSetOneOne = new ArrayList<>();
				voodooSetOneOne.add("GCWCRC");
				voodooSetOneOne.add("GCYDWC");
				voodooSetOneOne.add("WCYCWC");
				voodooSetOneOne.add("YCWDWC");
				phaseVoodooSets.add(voodooSetOneOne);
				List<String> voodooSetOneTwo = new ArrayList<>();
				voodooSetOneTwo.add("RCWCGD");
				voodooSetOneTwo.add("WCYCGD");
				voodooSetOneTwo.add("WCWDGD");
				voodooSetOneTwo.add("WCYDGD");
				phaseVoodooSets.add(voodooSetOneTwo);
				List<String> voodooSetOneThree = new ArrayList<>();
				voodooSetOneThree.add("GCWDRD");
				voodooSetOneThree.add("GCYDWD");
				voodooSetOneThree.add("WCYCWD");
				voodooSetOneThree.add("YCWCWD");
				phaseVoodooSets.add(voodooSetOneThree);
				List<String> voodooSetOneFour = new ArrayList<>();
				voodooSetOneFour.add("RDWDGC");
				voodooSetOneFour.add("WDYDGC");
				voodooSetOneFour.add("WDWCGC");
				voodooSetOneFour.add("WDYCGC");
				phaseVoodooSets.add(voodooSetOneFour);
			}
			case 3 -> {
				List<String> voodooSetTwoOne = new ArrayList<>();
				voodooSetTwoOne.add("YCWDGC");
				voodooSetTwoOne.add("WCYDGC");
				voodooSetTwoOne.add("WDYDGC");
				voodooSetTwoOne.add("YDWDGC");
				phaseVoodooSets.add(voodooSetTwoOne);
				List<String> voodooSetTwoTwo = new ArrayList<>();
				voodooSetTwoTwo.add("WCGCYC");
				voodooSetTwoTwo.add("YCGCWC");
				voodooSetTwoTwo.add("YDGCWC");
				voodooSetTwoTwo.add("WDGCYC");
				phaseVoodooSets.add(voodooSetTwoTwo);
			}
			case 5 -> {
				List<String> voodooSetThreeOne = new ArrayList<>();
				voodooSetThreeOne.add("RDGCWCGDWCGDWCGD");
				voodooSetThreeOne.add("WDGCWDGDRCGDWDGD");
				voodooSetThreeOne.add("WDGDYDGCWCGCYDGC");
				voodooSetThreeOne.add("WDGDYCGCWCGCYCGC");
				phaseVoodooSets.add(voodooSetThreeOne);
				List<String> voodooSetThreeTwo = new ArrayList<>();
				voodooSetThreeTwo.add("RCGDWCGDWDGCWCGD");
				voodooSetThreeTwo.add("WCGDWDGDRDGCWDGD");
				voodooSetThreeTwo.add("WCGCYDGCWDGDYDGC");
				voodooSetThreeTwo.add("WCGCYCGCWDGDYCGC");
				phaseVoodooSets.add(voodooSetThreeTwo);
			}
			default -> {

			}
		}
		Collections.shuffle(phaseVoodooSets);
		return phaseVoodooSets.get(0);
	}
}
