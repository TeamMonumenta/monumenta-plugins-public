package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.TouchofRadianceCS;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.TouchofRadianceEnhancement;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TouchofRadiance extends Ability {
	public static final String NAME = "Touch of Radiance";
	public static final ClassAbility ABILITY = ClassAbility.TOUCH_OF_RADIANCE;

	private static final String WEAKNESS_EFFECT_NAME = "TouchofRadianceWeakness";
	public static final String CDR_EFFECT_NAME = "TouchofRadianceCDR";

	private static final double WEAKNESS = 0.2;
	private static final double CDR_1 = 0.5;
	private static final double CDR_1_ALLY = 1 * CDR_1;
	private static final double CDR_2 = 0.75;
	private static final double CDR_2_ALLY = 1 * CDR_2;
	private static final int STUN_DURATION = 30;
	private static final int WEAKNESS_DURATION = 7 * 20;
	private static final int DURATION = 6 * 20;
	private static final double RANGE = 16;
	private static final double RADIUS = 3.5;
	private static final double ENHANCE_DAMAGE = 9;
	private static final int ENHANCE_STUN_DURATION = 10;
	private static final int COOLDOWN = 30 * 20;

	public static final String CHARM_RANGE = "Touch of Radiance Cast Range";
	public static final String CHARM_WEAKNESS = "Touch of Radiance Weakness Amplifier";
	public static final String CHARM_RADIUS = "Touch of Radiance Weakness Radius";
	public static final String CHARM_CDR = "Touch of Radiance Self Cooldown Recharge Rate";
	public static final String CHARM_CDR_ALLY = "Touch of Radiance Ally Cooldown Recharge Rate";
	public static final String CHARM_STUN_DURATION = "Touch of Radiance Stun Duration";
	public static final String CHARM_WEAKNESS_DURATION = "Touch of Radiance Weakness Duration";
	public static final String CHARM_DURATION = "Touch of Radiance Buff Duration";
	public static final String CHARM_ENHANCE_DAMAGE = "Touch of Radiance Enhancement Damage";
	public static final String CHARM_ENHANCE_STUN_DURATION = "Touch of Radiance Enhancement Stun Duration";
	public static final String CHARM_COOLDOWN = "Touch of Radiance Cooldown";

	public static final AbilityInfo<TouchofRadiance> INFO =
		new AbilityInfo<>(TouchofRadiance.class, NAME, TouchofRadiance::new)
			.linkedSpell(ABILITY)
			.scoreboardId("TouchofRadiance")
			.shorthandName("ToR")
			.actionBarColor(TextColor.color(255, 160, 30))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Link with a player or a Heretic to speed up your cooldown recharge rate and weaken enemies.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TouchofRadiance::cast,
				new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)))
			.displayItem(Material.GLOW_BERRIES);

	private final double mWeakness;
	private final double mCDR;
	private final double mCDRAlly;
	private final int mStunDuration;
	private final int mWeaknessDuration;
	private final int mBuffDuration;
	private final double mRange;
	private final double mWeaknessRadius;
	private final double mEnhanceDamage;
	private final int mEnhanceStunDuration;
	private final TouchofRadianceCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public TouchofRadiance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mWeakness = WEAKNESS + CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS);
		mCDR = (isLevelOne() ? CDR_1 : CDR_2) + CharmManager.getLevelPercentDecimal(player, CHARM_CDR);
		mCDRAlly = (isLevelOne() ? CDR_1_ALLY : CDR_2_ALLY) + CharmManager.getLevelPercentDecimal(player, CHARM_CDR_ALLY);
		mStunDuration = CharmManager.getDuration(player, CHARM_STUN_DURATION, STUN_DURATION);
		mWeaknessDuration = CharmManager.getDuration(player, CHARM_WEAKNESS_DURATION, WEAKNESS_DURATION);
		mBuffDuration = CharmManager.getDuration(player, CHARM_DURATION, DURATION);
		mRange = CharmManager.getRadius(player, CHARM_RANGE, RANGE);
		mWeaknessRadius = CharmManager.getRadius(player, CHARM_RADIUS, RADIUS);
		mEnhanceDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCE_DAMAGE, ENHANCE_DAMAGE);
		mEnhanceStunDuration = CharmManager.getDuration(player, CHARM_ENHANCE_STUN_DURATION, ENHANCE_STUN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new TouchofRadianceCS());

		Bukkit.getScheduler().runTask(plugin, () -> mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		//Ally cast
		Player targetPlayer = EntityUtils.getPlayerAtCursor(mPlayer, mRange, 1);
		if (targetPlayer != null) {
			GlowingManager.startGlowing(targetPlayer, NamedTextColor.YELLOW, mBuffDuration, 1);
			mPlugin.mEffectManager.addEffect(targetPlayer, CDR_EFFECT_NAME, new AbilityCooldownRechargeRate(mBuffDuration, mCDRAlly, ABILITY) {
				@Override
				public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
					super.entityTickEffect(entity, fourHertz, twoHertz, oneHertz);
					mCosmetic.tickEffect(targetPlayer);
				}

				@Override
				public void entityLoseEffect(Entity entity) {
					mCosmetic.loseEffect(targetPlayer);
				}
			}.deleteOnAbilityUpdate(true));
			if (isLevelTwo()) {
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(targetPlayer.getLocation(), mWeaknessRadius);
				mobs.forEach(m -> {
					mPlugin.mEffectManager.addEffect(m, WEAKNESS_EFFECT_NAME, new PercentDamageDealt(mBuffDuration, -mWeakness));
					Crusade.addCrusadeTag(m, mCrusade);
					if (isEnhanced()) {
						DamageUtils.damage(mPlayer, m, DamageEvent.DamageType.MAGIC, mEnhanceDamage, ClassAbility.TOUCH_OF_RADIANCE, true);
					}
				});
				mCosmetic.applyWeakness(mPlayer, targetPlayer, mWeaknessRadius);
			}
			mCosmetic.castOnPlayer(mPlayer, targetPlayer);
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mPlayer, "ToRenhance-" + mPlayer.getName(), new TouchofRadianceEnhancement(mPlugin, mPlayer, mEnhanceDamage, mEnhanceStunDuration, mBuffDuration, null, true).deleteOnAbilityUpdate(true));
				mPlugin.mEffectManager.addEffect(targetPlayer, "ToRenhance-" + targetPlayer.getName(), new TouchofRadianceEnhancement(mPlugin, targetPlayer, mEnhanceDamage, mEnhanceStunDuration, mBuffDuration, null, true).deleteOnAbilityUpdate(true));
			}
		} else {
			//Heretic cast
			LivingEntity targetHeretic = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange, e -> Crusade.enemyTriggersAbilities((LivingEntity) e));
			if (targetHeretic != null) {
				EntityUtils.applyStun(mPlugin, mStunDuration, targetHeretic);
				if (isLevelTwo()) {
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(targetHeretic.getLocation(), mWeaknessRadius);
					mobs.forEach(m -> {
						mPlugin.mEffectManager.addEffect(m, WEAKNESS_EFFECT_NAME, new PercentDamageDealt(mWeaknessDuration, -mWeakness));
						Crusade.addCrusadeTag(m, mCrusade);
					});
					mCosmetic.applyWeakness(mPlayer, targetHeretic, mWeaknessRadius);
				}
				if (isEnhanced()) {
					mPlugin.mEffectManager.addEffect(mPlayer, "ToRenhance-" + mPlayer.getName(), new TouchofRadianceEnhancement(mPlugin, mPlayer, mEnhanceDamage, mEnhanceStunDuration, mBuffDuration, targetHeretic).deleteOnAbilityUpdate(true));
					GlowingManager.startGlowing(targetHeretic, NamedTextColor.YELLOW, mBuffDuration, 1, null, "ToRenhanceGlowing");
				}
				mCosmetic.castOnHeretic(mPlayer, targetHeretic);
			} else {
				return false;
			}
		}

		GlowingManager.startGlowing(mPlayer, NamedTextColor.YELLOW, mBuffDuration, 1);
		mPlugin.mEffectManager.addEffect(mPlayer, CDR_EFFECT_NAME, new AbilityCooldownRechargeRate(mBuffDuration, mCDR, ABILITY) {
			@Override
			public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
				super.entityTickEffect(entity, fourHertz, twoHertz, oneHertz);
				mCosmetic.tickEffect(mPlayer);
			}

			@Override
			public void entityLoseEffect(Entity entity) {
				mCosmetic.loseEffect(mPlayer);
			}
		}.deleteOnAbilityUpdate(true));

		putOnCooldown();
		return true;
	}

	private static Description<TouchofRadiance> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger(" while looking at another player or a Heretic")
			.add(" within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks to gain ")
			.addPercent(a -> a.mCDR, CDR_1, false, Predicate.not(Ability::isLevelTwo))
			.add(" faster cooldown recharge rate for all other abilities for the next ")
			.addDuration(a -> a.mBuffDuration, DURATION)
			.add(" seconds. If looking at a player, they gain the same buff. If looking at a Heretic, it is stunned for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds. Players are prioritized when searching for a target.")
			.addCooldown(COOLDOWN);
	}

	private static Description<TouchofRadiance> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Cooldown recharge rate is increased to ")
			.addPercent(a -> a.mCDR, CDR_2, false, Predicate.not(Ability::isLevelOne))
			.add(". Additionally, all mobs within a ")
			.add(a -> a.mWeaknessRadius, RADIUS)
			.add(" block radius of the target are inflicted with ")
			.addPercent(a -> a.mWeakness, WEAKNESS)
			.add(" weakness for ")
			.addDuration(a -> a.mWeaknessDuration, WEAKNESS_DURATION)
			.add(" seconds.");
	}


	private static Description<TouchofRadiance> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After killing a targeted mob, your next attack on a Heretic deals ")
			.add(a -> a.mEnhanceDamage, ENHANCE_DAMAGE)
			.add(" magic damage to it and stuns it for ")
			.addDuration(a -> a.mEnhanceStunDuration, ENHANCE_STUN_DURATION)
			.add(" seconds. That mob becomes the new target, and killing it grants the same effect, repeating until Touch of Radiance runs out. If the first target was a player, the effect is primed for both of you immediately.");
	}
}
