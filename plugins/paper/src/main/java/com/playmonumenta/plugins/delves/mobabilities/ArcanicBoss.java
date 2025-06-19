package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.bosses.ChargerBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.bosses.spells.SpellMobHealAoE;
import com.playmonumenta.plugins.delves.abilities.Arcanic;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ArcanicBoss extends BossAbilityGroup {
	public static final String identityTag = "arcanic_ability";
	public static final int detectionRange = 50;

	private static final String REJUVENATION_RUNE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZThkZGEyN2JhNzU0NWIyZDg0YTIwNzZiMzlkMGVkMzEwOGExYzE4ZWY3YTYxNDg2ZmRiZDUwNzYwM2NjMmEyNiJ9fX0=";
	private static final String ARROW_RUNE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmEzODZiZGY0YTM0MzU1MzcyODI3ZjBkZmNiMWRkYmRkN2JmYjM3ZmY4OGQ1MzAzNGY3ZmJmNDQ2YTNiMmM2YSJ9fX0=";
	private static final String CHARGE_RUNE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzFmYzE1YmM1OWY3YjUxYjZlM2EwNDc0YTk2ZTI0OTkxOWEyZTM3ZjE3ZjI1N2I1MGE1OTk4Njc5YWY2YmU4MCJ9fX0=";
	private static final String MISSILE_RUNE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjI3MjE0MmJkZjFjZDQ0NDdiMzM1MzkxNGUxNThiMGI5M2EyOWQxMTI3ZjQ3MDY5Y2RiZjFlNDBiZGM2Yjc3NiJ9fX0=";

	private static final ArrayList<ProjectileBoss.Parameters> MISSILE_PARAMETERS = new ArrayList<>();
	private static final ArrayList<ProjectileBoss.Parameters> ARROW_PARAMETERS = new ArrayList<>();
	private static final ArrayList<ChargerBoss.Parameters> CHARGE_PARAMETERS = new ArrayList<>();
	private static final ArrayList<RejuvenationBoss.Parameters> REJUVENATION_PARAMETERS = new ArrayList<>();

	static {
		/* ---------------- Arcanic Missile ----------------*/
		final ProjectileBoss.Parameters mMissileParameters = new ProjectileBoss.Parameters();
		final int[] MISSILE_SPELL_DAMAGE = {12, 25, 35};
		mMissileParameters.SPELL_NAME = Arcanic.TRACKING_SPELL_NAME;

		mMissileParameters.SPEED = 0.2;
		mMissileParameters.SPELL_DELAY = 20;
		mMissileParameters.COOLDOWN = 320;
		mMissileParameters.TURN_RADIUS = 3.14;

		mMissileParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mMissileParameters.DETECTION, false, new EntityTargets.Limit(1), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));

		mMissileParameters.PARTICLE_LAUNCH = ParticlesList.fromString("[(REDSTONE,100,1,1,1,0,#9500ff)]");
		mMissileParameters.PARTICLE_PROJECTILE = ParticlesList.fromString("[(SPELL_WITCH,4,0,0,0,0.3),(DUST_COLOR_TRANSITION,1,0,0,0,0,#9500ff,GRAY,2),(SMOKE_NORMAL,5,0,0,0,0.1)]");
		mMissileParameters.PARTICLE_HIT = ParticlesList.fromString("[(SPELL_WITCH,40,0,0,0,0.3)]");

		mMissileParameters.SOUND_START = SoundsList.fromString("[(BLOCK_BEACON_POWER_SELECT,1,0.5),(ENTITY_WARDEN_SONIC_CHARGE,1,1.5)]");
		mMissileParameters.SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_EVOKER_CAST_SPELL,1,0.7),(ENTITY_BREEZE_SHOOT,1,0.5),(ENTITY_BLAZE_SHOOT,1,0.65)]");
		mMissileParameters.SOUND_HIT = SoundsList.fromString("[(BLOCK_BEACON_DEACTIVATE,1,0.5),(ENTITY_BREEZE_HURT,1,0.5),(ENTITY_PHANTOM_DEATH,1,0.75)]");
		mMissileParameters.SOUND_PROJECTILE = SoundsList.fromString("[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)]");

		/* ---------------- Arcanic Arrow ----------------*/
		final ProjectileBoss.Parameters mArrowParameters = new ProjectileBoss.Parameters();
		final int[] ARROW_SPELL_DAMAGE = {12, 30, 40};
		mArrowParameters.SPELL_NAME = Arcanic.MAGIC_ARROW_SPELL_NAME;

		mArrowParameters.SPEED = 0.8;
		mArrowParameters.SPELL_DELAY = 20;
		mArrowParameters.COOLDOWN = 160;
		mArrowParameters.TURN_RADIUS = 0;
		mArrowParameters.DISTANCE = 32;

		mArrowParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mArrowParameters.DETECTION, false, new EntityTargets.Limit(1), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));

		mArrowParameters.PARTICLE_LAUNCH = ParticlesList.EMPTY;
		mArrowParameters.PARTICLE_PROJECTILE = ParticlesList.fromString("[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)]");
		mArrowParameters.PARTICLE_HIT = ParticlesList.fromString("[(FIREWORKS_SPARK,30,0,0,0,0.25)]");

		mArrowParameters.SOUND_START = SoundsList.fromString("[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5),(ITEM_CROSSBOW_QUICK_CHARGE_3,1,0.75)]");
		mArrowParameters.SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,0.75),(ITEM_CROSSBOW_SHOOT,1,1.75),(ENTITY_BLAZE_SHOOT,1.0,1.35)]");
		mArrowParameters.SOUND_HIT = SoundsList.fromString("[(ENTITY_FIREWORK_ROCKET_TWINKLE,1,1.25),(ITEM_TRIDENT_HIT,1,1.75)]");
		mArrowParameters.SOUND_PROJECTILE = SoundsList.EMPTY;

		/* ---------------- Arcanic Charge ----------------*/
		final ChargerBoss.Parameters mChargeParameters = new ChargerBoss.Parameters();
		final int[] CHARGE_SPELL_DAMAGE = {15, 30, 45};
		mChargeParameters.SPELL_NAME = Arcanic.CHARGE_SPELL_NAME;

		mChargeParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mChargeParameters.DETECTION, false, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));

		mChargeParameters.PARTICLE_WARNING = ParticlesList.fromString("[(VILLAGER_ANGRY,15,0.25,1.25,0.25,0.0),(REDSTONE,100,1,1,1,0,RED)]");
		mChargeParameters.PARTICLE_TELL = ParticlesList.fromString("[(CRIT,2,0.5,0.5,0.5,0.0)]");
		mChargeParameters.PARTICLE_ROAR = ParticlesList.fromString("[(SMOKE_LARGE,45,0.0,0.0,0.0,0.0)]");
		mChargeParameters.PARTICLE_ATTACK = ParticlesList.fromString("[(FLAME,2,0.5,0.5,0.5,0.01),(FLAME,1,0.1,0.1,0.1,0.15),(REDSTONE,3,0.5,0.5,0.5,0,#2D2B2D,1.25),(SMOKE_NORMAL,2,0.1,0.1,0.1,0.15),(DUST_COLOR_TRANSITION,1,0.2,0.2,0.2,0,RED,#2D2B2D,2)]");

		mChargeParameters.SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,1,1.5),(ENTITY_ELDER_GUARDIAN_DEATH,1,2),(ENTITY_ILLUSIONER_PREPARE_BLINDNESS,1,1.65)]");
		mChargeParameters.SOUND_ROAR = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,1.0,0.7),(ENTITY_WARDEN_ATTACK_IMPACT,1,0.5)]");

		mChargeParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mChargeParameters.DETECTION, false, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
		/* ---------------- Arcanic Rejuvenation ----------------*/
		final RejuvenationBoss.Parameters mRejuvenationParameters = new RejuvenationBoss.Parameters();
		final int[] REJUVENATION_SPELL_HEAL = {12, 25, 35};

		mRejuvenationParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, mRejuvenationParameters.RANGE, false);
		mRejuvenationParameters.PARTICLE_RADIUS = mRejuvenationParameters.RANGE;

		mRejuvenationParameters.PARTICLE_CHARGE_AIR = ParticlesList.fromString("[(COMPOSTER,2,0.0,0.0,0.0,0.0)]");
		mRejuvenationParameters.PARTICLE_CHARGE_CIRCLE = ParticlesList.fromString("[(SPELL_INSTANT,2,0.0,0.0,0.0,0.0),(DUST_COLOR_TRANSITION,1,0.25,0.25,0.25,0,LIME,YELLOW,1.1),(TOTEM,1,0,0,0,0)]");
		mRejuvenationParameters.PARTICLE_OUTBURST_AIR = ParticlesList.EMPTY;
		mRejuvenationParameters.PARTICLE_HEAL = ParticlesList.fromString("[(FIREWORKS_SPARK,4,0.25,0.5,0.25,0.3),(HEART,3,0.4,0.5,0.4,0.0)]");
		mRejuvenationParameters.PARTICLE_OUTBURST_CIRCLE = ParticlesList.fromString("[(SPELL_INSTANT,2,0.25,0.25,0.25,0.35),(TOTEM,2,0.25,0.25,0.25,0.15)]");

		mRejuvenationParameters.SOUND_CHARGE = SoundsList.fromString("[(ITEM_TRIDENT_RETURN,0.8),(BLOCK_AMETHYST_BLOCK_RESONATE,1)]");
		mRejuvenationParameters.SOUND_OUTBURST_CIRCLE = SoundsList.fromString("[(ENTITY_EVOKER_CAST_SPELL,2,1.5),(ENTITY_ZOMBIE_VILLAGER_CONVERTED,2,2.0),(BLOCK_AMETHYST_BLOCK_CHIME,3,1.5),(BLOCK_AMETHYST_BLOCK_CHIME,3,1.5),(BLOCK_AMETHYST_BLOCK_CHIME,3,1.5)]");

		mRejuvenationParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, mRejuvenationParameters.RANGE, false);

		for (int i = 0; i < 3; i++) {
			cloneParams(mMissileParameters, MISSILE_SPELL_DAMAGE[i], MISSILE_PARAMETERS, (param, value) -> param.DAMAGE = value);
			cloneParams(mArrowParameters, ARROW_SPELL_DAMAGE[i], ARROW_PARAMETERS, (param, value) -> param.DAMAGE = value);
			cloneParams(mChargeParameters, CHARGE_SPELL_DAMAGE[i], CHARGE_PARAMETERS, (param, value) -> param.DAMAGE = value);
			cloneParams(mRejuvenationParameters, REJUVENATION_SPELL_HEAL[i], REJUVENATION_PARAMETERS, (param, value) -> param.HEAL = value);
		}
	}

	public enum ArcanicSpell {
		REJUVENATION(REJUVENATION_RUNE), ARROW(ARROW_RUNE), CHARGE(CHARGE_RUNE), MISSILE(MISSILE_RUNE);

		private final String rune;

		ArcanicSpell(String rune) {
			this.rune = rune;
		}

		private String getRune() {
			return rune;
		}

	}

	public static class Parameters extends BossParameters {
		public ArcanicSpell SPELL = ArcanicSpell.CHARGE;
		public int REGION = 1;
	}

	final ArcanicBoss.Parameters mParameters;
	private static final com.playmonumenta.plugins.Plugin rejuvPlugin = com.playmonumenta.plugins.Plugin.getInstance();
	@Nullable
	private ItemDisplay mArcaneRune;
	private static final float mRuneOffset = 0.85f;

	public ArcanicBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParameters = ArcanicBoss.Parameters.getParameters(boss, identityTag, new ArcanicBoss.Parameters());
		if (mParameters.REGION > 3 || mParameters.REGION < 1) {
			mParameters.REGION = 1;
		}
		mParameters.REGION--;
		Spell spell;
		switch (mParameters.SPELL) {
			case ARROW ->
				spell = new SpellBaseSeekingProjectile(plugin, mBoss, ARROW_PARAMETERS.get(mParameters.REGION));
			case MISSILE ->
				spell = new SpellBaseSeekingProjectile(plugin, mBoss, MISSILE_PARAMETERS.get(mParameters.REGION));
			case CHARGE ->
				spell = new SpellBaseCharge(plugin, mBoss, CHARGE_PARAMETERS.get(mParameters.REGION));
			case REJUVENATION ->
				spell = new SpellMobHealAoE(rejuvPlugin, mBoss, REJUVENATION_PARAMETERS.get(mParameters.REGION));
			default -> spell = new SpellBaseCharge(plugin, mBoss, CHARGE_PARAMETERS.get(mParameters.REGION));
		}
		mArcaneRune = DisplayEntityUtils.spawnItemDisplayWithBase64Head(boss.getLocation(), mParameters.SPELL.getRune());

		if (mArcaneRune == null) {
			return;
		}

		mArcaneRune.addScoreboardTag("REMOVE_ON_UNLOAD");
		Transformation transformation = mArcaneRune.getTransformation();
		boss.addPassenger(mArcaneRune);

		new BukkitRunnable() {
			private float yaw = 0f;
			private float verticalOscillation = 15f;

			@Override
			public void run() {
				if (mArcaneRune == null || mArcaneRune.isDead() || !mArcaneRune.isValid()) {
					this.cancel();
					return;
				}
				if (!mBoss.isValid()) {
					mArcaneRune.remove();
					this.cancel();
					return;
				}
				mArcaneRune.setRotation(yaw, 0f);
				yaw = (yaw + 12f) % 360; // 12f is rotation speed

				float yOffset = (float) (mRuneOffset + FastUtils.sinDeg(verticalOscillation * 6) * 0.1);
				verticalOscillation = (verticalOscillation + 1) % 60;
				transformation.getTranslation().set(0, yOffset, 0);
				mArcaneRune.setTransformation(transformation);
			}
		}.runTaskTimer(mPlugin, 0, 1);

		super.constructBoss(spell, 24, null, 20); // Every arcanic spell has an initial delay of a second
	}

	@Override
	public void unload() {
		super.unload();
		if (!mBoss.isDead()) {
			mArcaneRune = null;
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (mArcaneRune != null) mArcaneRune.remove();
	}

	// Might not be necessary?
	private static <T extends BossParameters> void cloneParams(T param, int value, List<T> parameterList, BiConsumer<T, Integer> biConsumer) {
		T clone = BossParameters.shallowClone(param);
		biConsumer.accept(clone, value);
		parameterList.add(clone);
	}
}
