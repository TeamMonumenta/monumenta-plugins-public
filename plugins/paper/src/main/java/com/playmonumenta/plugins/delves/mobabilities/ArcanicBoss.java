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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;

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

		mMissileParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mMissileParameters.DETECTION, new EntityTargets.Limit(1), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));

		mMissileParameters.PARTICLE_LAUNCH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 100, 1.0, 1.0, 1.0, 0.0, new Particle.DustOptions(Color.fromRGB(0x9500ff), 1.0f)))
			.build();
		mMissileParameters.PARTICLE_PROJECTILE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 4, 0.0, 0.0, 0.0, 0.3))
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0.0, 0.0, 0.0, 0.0, new Particle.DustTransition(Color.fromRGB(0x9500ff), Color.GRAY, 2.0f)))
			.add(new ParticlesList.CParticle(Particle.SMOKE_NORMAL, 5, 0.0, 0.0, 0.0, 0.1))
			.build();
		mMissileParameters.PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 40, 0.0, 0.0, 0.0, 0.3))
			.build();

		mMissileParameters.SOUND_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.5f))
			.build();
		mMissileParameters.SOUND_LAUNCH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.7f))
			.add(new SoundsList.CSound(Sound.ENTITY_BREEZE_SHOOT, 1.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.65f))
			.build();
		mMissileParameters.SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_BREEZE_HURT, 1.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_PHANTOM_DEATH, 1.0f, 0.75f))
			.build();
		mMissileParameters.SOUND_PROJECTILE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 0.5f))
			.build();

		/* ---------------- Arcanic Arrow ----------------*/
		final ProjectileBoss.Parameters mArrowParameters = new ProjectileBoss.Parameters();
		final int[] ARROW_SPELL_DAMAGE = {12, 30, 40};
		mArrowParameters.SPELL_NAME = Arcanic.MAGIC_ARROW_SPELL_NAME;

		mArrowParameters.SPEED = 0.8;
		mArrowParameters.SPELL_DELAY = 20;
		mArrowParameters.COOLDOWN = 160;
		mArrowParameters.TURN_RADIUS = 0;
		mArrowParameters.DISTANCE = 32;

		mArrowParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mArrowParameters.DETECTION, new EntityTargets.Limit(1), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));

		mArrowParameters.PARTICLE_LAUNCH = ParticlesList.EMPTY;
		mArrowParameters.PARTICLE_PROJECTILE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 5, 0.1, 0.1, 0.1, 0.05))
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 20, 0.2, 0.2, 0.2, 0.1))
			.build();
		mArrowParameters.PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 30, 0.0, 0.0, 0.0, 0.25))
			.build();

		mArrowParameters.SOUND_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.ITEM_CROSSBOW_QUICK_CHARGE_3, 1.0f, 0.75f))
			.build();
		mArrowParameters.SOUND_LAUNCH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.75f))
			.add(new SoundsList.CSound(Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.75f))
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.35f))
			.build();
		mArrowParameters.SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.25f))
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_HIT, 1.0f, 1.75f))
			.build();
		mArrowParameters.SOUND_PROJECTILE = SoundsList.EMPTY;

		/* ---------------- Arcanic Charge ----------------*/
		final ChargerBoss.Parameters mChargeParameters = new ChargerBoss.Parameters();
		final int[] CHARGE_SPELL_DAMAGE = {15, 30, 45};
		mChargeParameters.SPELL_NAME = Arcanic.CHARGE_SPELL_NAME;

		mChargeParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mChargeParameters.DETECTION, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));

		mChargeParameters.PARTICLE_WARNING = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.VILLAGER_ANGRY, 15, 0.25, 1.25, 0.25, 0.0))
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 100, 1.0, 1.0, 1.0, 0.0, new Particle.DustOptions(Color.RED, 1.0f)))
			.build();
		mChargeParameters.PARTICLE_TELL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 2, 0.5, 0.5, 0.5, 0.0))
			.build();
		mChargeParameters.PARTICLE_ROAR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 45, 0.0, 0.0, 0.0, 0.0))
			.build();
		mChargeParameters.PARTICLE_ATTACK = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 2, 0.5, 0.5, 0.5, 0.01))
			.add(new ParticlesList.CParticle(Particle.FLAME, 1, 0.1, 0.1, 0.1, 0.15))
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 3, 0.5, 0.5, 0.5, 0.0, new Particle.DustOptions(Color.fromRGB(0x2d2b2d), 1.25f)))
			.add(new ParticlesList.CParticle(Particle.SMOKE_NORMAL, 2, 0.1, 0.1, 0.1, 0.15))
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0.2, 0.2, 0.2, 0.0, new Particle.DustTransition(Color.RED, Color.fromRGB(0x2d2b2d), 2.0f)))
			.build();

		mChargeParameters.SOUND_WARNING = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0f, 2.0f))
			.add(new SoundsList.CSound(Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.65f))
			.build();
		mChargeParameters.SOUND_ROAR = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f))
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 0.5f))
			.build();

		mChargeParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mChargeParameters.DETECTION, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));
		/* ---------------- Arcanic Rejuvenation ----------------*/
		final RejuvenationBoss.Parameters mRejuvenationParameters = new RejuvenationBoss.Parameters();
		final int[] REJUVENATION_SPELL_HEAL = {12, 25, 35};

		mRejuvenationParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, mRejuvenationParameters.RANGE);
		mRejuvenationParameters.PARTICLE_RADIUS = mRejuvenationParameters.RANGE;

		mRejuvenationParameters.PARTICLE_CHARGE_AIR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.COMPOSTER, 2, 0.0, 0.0, 0.0, 0.0))
			.build();
		mRejuvenationParameters.PARTICLE_CHARGE_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_INSTANT, 2, 0.0, 0.0, 0.0, 0.0))
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0.25, 0.25, 0.25, 0.0, new Particle.DustTransition(Color.LIME, Color.YELLOW, 1.1f)))
			.add(new ParticlesList.CParticle(Particle.TOTEM, 1, 0.0, 0.0, 0.0, 0.0))
			.build();
		mRejuvenationParameters.PARTICLE_OUTBURST_AIR = ParticlesList.EMPTY;
		mRejuvenationParameters.PARTICLE_HEAL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 4, 0.25, 0.5, 0.25, 0.3))
			.add(new ParticlesList.CParticle(Particle.HEART, 3, 0.4, 0.5, 0.4, 0.0))
			.build();
		mRejuvenationParameters.PARTICLE_OUTBURST_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_INSTANT, 2, 0.25, 0.25, 0.25, 0.35))
			.add(new ParticlesList.CParticle(Particle.TOTEM, 2, 0.25, 0.25, 0.25, 0.15))
			.build();

		mRejuvenationParameters.SOUND_CHARGE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.0f))
			.add(new SoundsList.CSound(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f))
			.build();
		mRejuvenationParameters.SOUND_OUTBURST_CIRCLE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0f, 2.0f))
			.add(new SoundsList.CSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 3.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 3.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 3.0f, 1.5f))
			.build();

		mRejuvenationParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, mRejuvenationParameters.RANGE);

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
			case CHARGE -> spell = new SpellBaseCharge(plugin, mBoss, CHARGE_PARAMETERS.get(mParameters.REGION));
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
		if (mArcaneRune != null) {
			mArcaneRune.remove();
		}
	}

	// Might not be necessary?
	private static <T extends BossParameters> void cloneParams(T param, int value, List<T> parameterList, BiConsumer<T, Integer> biConsumer) {
		T clone = BossParameters.shallowClone(param);
		biConsumer.accept(clone, value);
		parameterList.add(clone);
	}
}
