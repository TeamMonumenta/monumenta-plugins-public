package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.MelancholicLamentCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class MelancholicLament extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.3;
	private static final int SILENCE_WINDOW = 4 * 20;
	private static final int SILENCE_RADIUS = 3;
	private static final int SILENCE_DURATION = 3 * 20;
	private static final int COOLDOWN = 20 * 16;
	private static final int RADIUS = 8;
	private static final int CLEANSE_REDUCTION = 20 * 10;
	private static final int ENHANCE_DURATION = 8 * 20;
	private static final int ENHANCE_RADIUS = 16;
	private static final double ENHANCE_DAMAGE = .025;
	private static final String ENHANCE_EFFECT_NAME = "LamentDamage";
	private static final String ENHANCE_EFFECT_PARTICLE_NAME = "LamentParticle";
	private static final int ENHANCE_EFFECT_DURATION = 20;
	private static final int ENHANCE_MAX_MOBS = 6;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageEvent.DamageType.MELEE);

	public static final String CHARM_RADIUS = "Melancholic Lament Radius";
	public static final String CHARM_COOLDOWN = "Melancholic Lament Cooldown";
	public static final String CHARM_WEAKNESS = "Melancholic Lament Weakness Amplifier";
	public static final String CHARM_WEAKNESS_DURATION = "Melancholic Lament Weakness Duration";
	public static final String CHARM_SILENCE_RADIUS = "Melancholic Lament Silence Radius";
	public static final String CHARM_SILENCE_DURATION = "Melancholic Lament Silence Duration";
	public static final String CHARM_RECOVERY = "Melancholic Lament Negative Effect Recovery";
	public static final String CHARM_ENHANCE_RADIUS = "Melancholic Lament Enhancement Radius";
	public static final String CHARM_ENHANCE_DAMAGE = "Melancholic Lament Enhancement Damage Modifier";
	public static final String CHARM_ENHANCE_MAX_MOBS = "Melancholic Lament Enhancement Max Mobs";
	public static final String CHARM_ENHANCE_DURATION = "Melancholic Lament Enhancement Duration";

	public static final AbilityInfo<MelancholicLament> INFO =
		new AbilityInfo<>(MelancholicLament.class, "Melancholic Lament", MelancholicLament::new)
			.linkedSpell(ClassAbility.MELANCHOLIC_LAMENT)
			.scoreboardId("Melancholic")
			.shorthandName("MLa")
			.actionBarColor(TextColor.color(235, 235, 224))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Weaken nearby mobs and force them to target you.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MelancholicLament::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.GHAST_TEAR);

	private final double mRadius;
	private final double mWeakenEffect;
	private final int mWeakenDuration;
	private final double mSilenceRadius;
	private final int mSilenceDuration;
	private final int mReductionTime;
	private final double mEnhanceRadius;
	private final double mEnhanceDamage;
	private final int mEnhanceCap;
	private final int mEnhanceDuration;

	private @Nullable BukkitRunnable mSilenceRunnable;

	private final MelancholicLamentCS mCosmetic;

	public MelancholicLament(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mWeakenDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKNESS_DURATION, DURATION);
		mSilenceRadius = CharmManager.getRadius(player, CHARM_SILENCE_RADIUS, SILENCE_RADIUS);
		mSilenceDuration = CharmManager.getDuration(player, CHARM_SILENCE_DURATION, SILENCE_DURATION);
		mReductionTime = CharmManager.getDuration(mPlayer, CHARM_RECOVERY, CLEANSE_REDUCTION);
		mEnhanceRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCE_RADIUS, ENHANCE_RADIUS);
		mEnhanceDamage = ENHANCE_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_DAMAGE);
		mEnhanceCap = ENHANCE_MAX_MOBS + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCE_MAX_MOBS);
		mEnhanceDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_DURATION, ENHANCE_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MelancholicLamentCS());

		mSilenceRunnable = null;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlayer, world, loc, mRadius);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			EntityUtils.applyWeaken(mPlugin, mWeakenDuration, mWeakenEffect, mob);
			EntityUtils.applyTaunt(mob, mPlayer);
			mPlugin.mEffectManager.addEffect(mob, "MelancholicLamentParticles", new Aesthetics(mWeakenDuration,
				(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.debuffTick(mob),
				(entity) -> {
				})
			);

			mCosmetic.onWeakenApply(mPlayer, mob);
		}

		if (isLevelTwo()) {
			if (mSilenceRunnable != null) {
				mSilenceRunnable.cancel();
			}
			mSilenceRunnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mCosmetic.silenceReadyTick(mPlayer);

					mTicks += 5;
					if (mTicks >= SILENCE_WINDOW) {
						this.cancel();
						mSilenceRunnable = null;
					}
				}
			};
			mSilenceRunnable.runTaskTimer(mPlugin, 0, 5);
		}

		if (isEnhanced()) {

			cancelOnDeath(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {

					if (!mPlayer.isOnline() || mPlayer.isDead()) {
						this.cancel();
						return;
					}

					Hitbox enhanceHitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mEnhanceRadius);
					int numTargeting = (int) enhanceHitbox
						.getHitMobs().stream()
						.filter(entity -> entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget().equals(mPlayer))
						.limit(mEnhanceCap)
						.count();
					for (Player player : enhanceHitbox.getHitPlayers(true)) {
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_NAME, new PercentDamageDealt(ENHANCE_EFFECT_DURATION, mEnhanceDamage * numTargeting).damageTypes(AFFECTED_DAMAGE_TYPES).displaysTime(false).deleteOnAbilityUpdate(true));
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_PARTICLE_NAME, new Aesthetics(ENHANCE_EFFECT_DURATION,
							(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.enhancementTick(player, mPlayer, fourHertz, twoHertz, oneHertz),
							(entity) -> {
							}).deleteOnAbilityUpdate(true)
						);
					}

					mTicks += 1;
					if (mTicks > mEnhanceDuration) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));

		}

		if (isLevelTwo()) {
			for (Player player : hitbox.getHitPlayers(true)) {
				mCosmetic.onCleanse(player, mPlayer);
				for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
					PotionEffect effect = player.getPotionEffect(effectType);
					if (effect != null) {
						player.removePotionEffect(effectType);
						if (effect.getDuration() > mReductionTime) {
							player.addPotionEffect(new PotionEffect(effectType, effect.getDuration() - mReductionTime, effect.getAmplifier()));
						}
					}
				}
				EntityUtils.setWeakenTicks(mPlugin, player, Math.max(0, EntityUtils.getWeakenTicks(mPlugin, player) - mReductionTime));
				EntityUtils.setSlowTicks(mPlugin, player, Math.max(0, EntityUtils.getSlowTicks(mPlugin, player) - mReductionTime));
			}
		}
		putOnCooldown();
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())
			&& mSilenceRunnable != null && !mSilenceRunnable.isCancelled()) {
			mSilenceRunnable.cancel();
			mSilenceRunnable = null;

			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), mSilenceRadius)) {
				EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
			}

			mCosmetic.onSilenceHit(mPlayer, enemy, mSilenceRadius);
		}
		return false;
	}

	private static Description<MelancholicLament> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to recite a haunting song, causing all mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks to target you and afflicting them with ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_1, false, Ability::isLevelOne)
			.add(" Weaken for ")
			.addDuration(a -> a.mWeakenDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<MelancholicLament> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The Weaken is increased to ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_2, false, Ability::isLevelTwo)
			.add(". Players in the radius have their negative potion effect durations decreased by ")
			.addDuration(a -> a.mReductionTime, CLEANSE_REDUCTION)
			.add(" seconds. Your next melee scythe attack within the next ")
			.addDuration(SILENCE_WINDOW)
			.add(" seconds will silence all mobs within ")
			.add(a -> a.mSilenceRadius, SILENCE_RADIUS)
			.add(" blocks for ")
			.addDuration(a -> a.mSilenceDuration, SILENCE_DURATION)
			.add(" seconds.");
	}

	private static Description<MelancholicLament> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("For ")
			.addDuration(a -> a.mEnhanceDuration, ENHANCE_DURATION)
			.add(" seconds after casting this ability, you and other players within ")
			.add(a -> a.mEnhanceRadius, ENHANCE_RADIUS)
			.add(" blocks gain ")
			.addPercent(a -> a.mEnhanceDamage, ENHANCE_DAMAGE)
			.add(" melee damage for each mob targeting you in that radius (capped at ")
			.add(a -> a.mEnhanceCap, ENHANCE_MAX_MOBS)
			.add(" mobs).");
	}
}
