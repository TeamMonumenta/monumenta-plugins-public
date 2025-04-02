package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishCoreLevitation;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveCoreInstability;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveHeatAttack;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellCoreExpansion;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellInfernoSpit;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellMagmaticConvergence;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellPyroclasticSlam;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellRumblingEmergence;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CoreElemental extends Quarry {
	// This is mainly used for core instability (spoil), telegraph is a by-product
	public interface CoreElementalBase {
		// Primary material of fissures
		Material FISSURE_MATERIAL = Material.MAGMA_BLOCK;
		// Secondary material of fissures
		Material LAVA_MATERIAL = Material.LAVA;

		String getSpellName();

		String getSpellChargePrefix();

		int getChargeDuration();

		int getSpellDuration();

		default Component getTitle() {
			return Component.empty()
				.append(Component.text(getSpellChargePrefix()))
				.append(Component.text(" "))
				.append(Component.text(getSpellName(), NamedTextColor.RED));
		}
	}

	public static final String identityTag = "boss_coreelemental";
	private static final int HEALTH = 6000;
	private static final double KNOCKBACK_RESISTANCE = 0.7;
	private static final int MELEE_DAMAGE = 45;
	public static final int INNER_RADIUS = 30;
	public static final int OUTER_RADIUS = 50;
	private static final int NUMBER_OF_FISSURE = 5;
	private static final int NUMBER_OF_PHASE = 3;
	private static final int[] PHASE_HEALTH = {30, 50, 70};
	private static final double[] FISSURE_LENGTH = {5, 6, 9};
	public static final TextColor COLOR = TextColor.color(255, 142, 61);

	public final PassiveCoreInstability mCoreInstability;
	public boolean mIsCastingBanish = false;

	private final Map<Block, Material> mChangedBlocks = new HashMap<>();

	public CoreElemental(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.CORE_ELEMENTAL);
		// Boss initialization
		mBoss.setRemoveWhenFarAway(false);
		mBoss.customName(Component.text("Core Elemental", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
		mBoss.setCustomNameVisible(false);
		EntityUtils.setSize(mBoss, 3);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, MELEE_DAMAGE);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
		mBoss.addScoreboardTag("Boss");
		mBoss.addScoreboardTag("CoreElemental");
		mBoss.addScoreboardTag("boss_nosplit");

		// Generate vectors for banish particles
		Vector[][] fissureVector = new Vector[NUMBER_OF_FISSURE][NUMBER_OF_PHASE];
		for (int i = 0; i < NUMBER_OF_FISSURE; i++) {
			for (int j = 0; j < NUMBER_OF_PHASE; j++) {
				// Generate crack vectors and store them into 2D array
				Vector vector = generateParticleSegmentVector(j == 0 ? i * 360 / NUMBER_OF_FISSURE :
					(int) VectorUtils.vectorToRotation(fissureVector[i][j - 1])[0], FISSURE_LENGTH[j]);
				fissureVector[i][j] = vector;
			}
		}

		// Spells
		mCoreInstability = new PassiveCoreInstability(plugin, boss, this, OUTER_RADIUS);
		mBanishSpell = new BanishCoreLevitation(plugin, boss, this, spawnLoc, fissureVector, mCoreInstability);

		SpellManager activeSpells = new SpellManager(List.of(
			new SpellPyroclasticSlam(plugin, boss, this, spawnLoc),
			new SpellMagmaticConvergence(plugin, boss, this, spawnLoc),
			new SpellRumblingEmergence(plugin, boss, this, spawnLoc),
			new SpellInfernoSpit(plugin, boss, this, spawnLoc),
			new SpellCoreExpansion(plugin, boss, this)
		));

		List<Spell> passiveSpells = List.of(
			new PassiveHeatAttack(boss),
			mCoreInstability,
			new SpellBlockBreak(mBoss, true, false) {
				@Override
				public boolean canRun() {
					// SpellRumblingEmergence makes the boss invulnerable while digging in the ground
					// We don't want it to make a hole while doing this spell
					return !mBoss.isInvulnerable();
				}
			}
		);

		// Events
		Map<Integer, BossBarManager.BossHealthAction> events = getBaseHealthEvents();
		for (int health : PHASE_HEALTH) {
			changePhase(activeSpells, passiveSpells, null); // In reality, this cancels currently running spells
			events.put(health, mBoss -> forceCastSpell(SpellPyroclasticSlam.class));
		}

		BossBarManager bossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events, true, true, mSpawnLoc);
		super.constructBoss(activeSpells, passiveSpells, OUTER_RADIUS, bossBar);
	}

	private Vector generateParticleSegmentVector(int angle, double length) {
		int range = 10;
		return VectorUtils.rotationToVector(angle + FastUtils.randomIntInRange(-range, range), 0)
			.multiply(length * FastUtils.randomDoubleInRange(0.75, 1));
	}

	@Override
	public void init() {
		EntityUtils.setMaxHealthAndHealth(mBoss, HEALTH);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mCoreInstability.mUnstable > 0 && event.getType() == DamageEvent.DamageType.MELEE) {
			event.setFlatDamage(0);
		}
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/core_elemental_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/core_elemental_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/core_elemental";
	}

	@Override
	public String getQuestTag() {
		return "HuntSlime";
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);
		// Remove players from banish list
		if (mBanishSpell != null && event.getDamager() != null) {
			mBanishSpell.onHurtByEntity(event, event.getDamager());
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);
		revertChangedBlocks();
	}

	@Override
	public void onDespawn() {
		revertChangedBlocks();
	}

	public void revertChangedBlocks() {
		mChangedBlocks.forEach(TemporaryBlockChangeManager.INSTANCE::revertChangedBlock);
	}

	public void addChangedBlock(Block block) {
		mChangedBlocks.put(block, block.getType());
	}
}
