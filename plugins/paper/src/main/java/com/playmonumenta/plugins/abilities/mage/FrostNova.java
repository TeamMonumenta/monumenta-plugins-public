package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.FrostNovaCS;
import com.playmonumenta.plugins.effects.CholericFlamesAntiHeal;
import com.playmonumenta.plugins.effects.Frozen;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perLevel;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class FrostNova extends Ability {

	public static final String NAME = "Frost Nova";
	public static final ClassAbility ABILITY = ClassAbility.FROST_NOVA;

	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 10;
	public static final int SIZE = 7;
	public static final double SLOW_MULTIPLIER_1 = 0.2;
	public static final double SLOW_MULTIPLIER_2 = 0.4;
	public static final double ELITE_SLOW_MULTIPLIER_REDUCTION = 0.1;
	public static final double ENHANCED_DAMAGE_MODIFIER = 1.15;
	public static final int DURATION_TICKS = 4 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_FROZEN_DURATION = 2 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_1 = 18 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_2 = 16 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_COOLDOWN_TICKS = Constants.TICKS_PER_SECOND;
	public static final String ANTIHEAL_EFFECT = "FrostNovaAntiHeal";

	public static final String CHARM_DAMAGE = "Frost Nova Damage";
	public static final String CHARM_COOLDOWN = "Frost Nova Cooldown";
	public static final String CHARM_RANGE = "Frost Nova Range";
	public static final String CHARM_SLOW = "Frost Nova Slowness Amplifier";
	public static final String CHARM_DURATION = "Frost Nova Slowness Duration";
	public static final String CHARM_FROZEN = "Frost Nova Frozen Duration";

	public static final AbilityInfo<FrostNova> INFO =
		new AbilityInfo<>(FrostNova.class, NAME, FrostNova::new)
			.linkedSpell(ABILITY)
			.scoreboardId("FrostNova")
			.shorthandName("FN")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Damage and slow nearby mobs.")
			.cooldown(COOLDOWN_TICKS_1, COOLDOWN_TICKS_2, COOLDOWN_TICKS_1 - ENHANCED_COOLDOWN_TICKS, COOLDOWN_TICKS_2 - ENHANCED_COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FrostNova::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.ICE);

	private final double mBaseDamage;
	private final double mLevelDamage;
	private final double mLevelSlowMultiplier;
	private final int mDuration;
	private final double mRadius;
	private final int mFrozenDuration;

	private final FrostNovaCS mCosmetic;

	public FrostNova(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelDamage = mBaseDamage * (isEnhanced() ? ENHANCED_DAMAGE_MODIFIER : 1);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE);
		mFrozenDuration = CharmManager.getDuration(mPlayer, CHARM_FROZEN, ENHANCED_FROZEN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new FrostNovaCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				EntityUtils.applySlow(mPlugin, mDuration, mLevelSlowMultiplier - ELITE_SLOW_MULTIPLIER_REDUCTION, mob);
			} else {
				EntityUtils.applySlow(mPlugin, mDuration, mLevelSlowMultiplier, mob);
				if (isEnhanced()) {
					EntityUtils.applyFreeze(mPlugin, mFrozenDuration, mob);
					// Choleric Flame already uses an anti heal effect, no need to create a new one as long as name is different.
					mPlugin.mEffectManager.addEffect(mob, ANTIHEAL_EFFECT, new CholericFlamesAntiHeal(mFrozenDuration));
				}
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);
			mCosmetic.enemyEffect(mPlugin, mPlayer, mob);
			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlugin, mPlayer, world, mRadius);

		return true;
	}

	private static Description<FrostNova> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Deal *Ice* damage and slow all nearby mobs.").styles(Mage.ICE_COLOR)
			.addLine("Elites and Bosses receive -%p less slowness.")
				.statValues(stat(ELITE_SLOW_MULTIPLIER_REDUCTION))
			.addLine()
			.addLine("Mobs and players on fire are extinguished.")
			.addLine()
			.addStat("Damage: %d1e (s)")
				.statValues(stat(a -> a.mBaseDamage, DAMAGE_1))
			.addStat("Normal Effect: %p1 Slowness for %t")
				.statValues(stat(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_1), stat(a -> a.mDuration, DURATION_TICKS))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, SIZE))
			.addStat("Cooldown: %t1e")
				.statValues(cooldown(COOLDOWN_TICKS_1))
			.addDashedLine();
	}

	private static Description<FrostNova> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Frost Nova*'s damage and").styles(UNDERLINED)
			.addLine("slowness, and reduce its cooldown.")
			.addLine()
			.addStatComparison("Damage: %d1e -> %d2e (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mBaseDamage, DAMAGE_2))
			.addStatComparison("Effect: %p1 -> %p2 Slowness")
				.statValues(stat(SLOW_MULTIPLIER_1), stat(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_2))
			.addStatComparison("Cooldown: %t1e -> %t2e")
				.statValues(cooldown(COOLDOWN_TICKS_1), cooldown(COOLDOWN_TICKS_2))
			.addDashedLine();
	}

	private static Description<FrostNova> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Frost Nova*'s damage by").styles(UNDERLINED)
			.addLine("an additional +%p and reduce its")
				.statValues(stat(ENHANCED_DAMAGE_MODIFIER - 1))
			.addLine("cooldown by %t.")
				.statValues(stat(ENHANCED_COOLDOWN_TICKS))
			.addLine()
			.addLine("Mobs hit are temporarily *Frozen*").styles(Frozen.FROZEN_COLOR)
			.addLine("and are afflicted with anti-heal.")
			.addLine("(Elites/Bosses aren't frozen)")
			.addLine()
			.addStatComparison("Damage: %d1e -> %d3 (s)")
				.statValues(perLevel(DAMAGE_1, DAMAGE_2), perLevel(a -> a.mLevelDamage, DAMAGE_1 * ENHANCED_DAMAGE_MODIFIER, DAMAGE_2 * ENHANCED_DAMAGE_MODIFIER))
			.addStat("Effect: Frozen for %t")
				.statValues(stat(a -> a.mFrozenDuration, ENHANCED_FROZEN_DURATION))
			.addStat("Effect: -100% Healing for %t")
				.statValues(stat(a -> a.mFrozenDuration, ENHANCED_FROZEN_DURATION))
			.addStatComparison("Cooldown: %t1e -> %t3")
				.statValues(perLevel(COOLDOWN_TICKS_1, COOLDOWN_TICKS_2), perLevel(a -> a.getCharmCooldown(a.isLevelOne() ? COOLDOWN_TICKS_1 - ENHANCED_COOLDOWN_TICKS : COOLDOWN_TICKS_2 - ENHANCED_COOLDOWN_TICKS), COOLDOWN_TICKS_1 - ENHANCED_COOLDOWN_TICKS, COOLDOWN_TICKS_2 - ENHANCED_COOLDOWN_TICKS))
			.addDashedLine();
	}

}
