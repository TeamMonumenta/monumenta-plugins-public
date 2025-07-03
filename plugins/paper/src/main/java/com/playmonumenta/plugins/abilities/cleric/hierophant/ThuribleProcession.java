package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.ThuribleProcessionCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ThuribleBonusHealing;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ThuribleProcession extends Ability implements AbilityWithChargesOrStacks {
	private static final int EFFECTS_DURATION = 20 * 8;
	private static final int PASSIVE_DURATION = 50; //50 ticks; 20 * 2.5
	private static final int THURIBLE_RADIUS = 30;
	private static final int THURIBLE_COOLDOWN = 8 * 20;
	private static final double EFFECT_PERCENT_1 = 0.10;
	private static final double EFFECT_PERCENT_2 = 0.15;
	private static final double THURIBLE_HEALING_PERCENT_1 = 1.0;
	private static final double THURIBLE_HEALING_PERCENT_2 = 2.0;
	private static final int MAX_BUFFS = 4;
	private static final double DAMAGE_BREAK_PERCENT = 0.6;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL
	);
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "ThuribleProcessionPercentAttackSpeedEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "ThuribleProcessionPercentSpeedEffect";
	public static final String PERCENT_DAMAGE_EFFECT_NAME = "ThuribleProcessionPercentDamageEffect";
	public static final String PERCENT_HEAL_EFFECT_NAME = "ThuribleProcessionPercentHealEffect";
	private static final String[] EFFECTS_NAMES = new String[] {PERCENT_ATTACK_SPEED_EFFECT_NAME, PERCENT_SPEED_EFFECT_NAME, PERCENT_DAMAGE_EFFECT_NAME, PERCENT_HEAL_EFFECT_NAME};

	public static final String CHARM_DAMAGE = "Thurible Procession Damage Amplifier";
	public static final String CHARM_ATTACK = "Thurible Procession Attack Speed Amplifier";
	public static final String CHARM_SPEED = "Thurible Procession Speed Amplifier";
	public static final String CHARM_EFFECT_DURATION = "Thurible Procession Effect Duration";
	public static final String CHARM_COOLDOWN = "Thurible Procession Cooldown";
	public static final String CHARM_HEAL = "Thurible Procession Heal";

	public static final AbilityInfo<ThuribleProcession> INFO =
		new AbilityInfo<>(ThuribleProcession.class, "Thurible Procession", ThuribleProcession::new)
			.linkedSpell(ClassAbility.THURIBLE_PROCESSION)
			.scoreboardId("Thurible")
			.shorthandName("TP")
			.actionBarColor(TextColor.color(255, 195, 0))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Keep high health to passively build up buffs that are shared with nearby players.")
			.cooldown(THURIBLE_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GLOWSTONE_DUST);

	private final double mAttackSpeedPotency;
	private final double mSpeedPotency;
	private final double mDamagePotency;
	private final double mHealingPotency;
	private final int mEffectDuration;
	private final int mPassiveDuration;
	private final ThuribleProcessionCS mCosmetic;
	private int mSeconds = 0;
	private int mBuffs = 0;

	public ThuribleProcession(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		final double effectPotency = isLevelTwo() ? EFFECT_PERCENT_2 : EFFECT_PERCENT_1;
		mAttackSpeedPotency = effectPotency + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK);
		mSpeedPotency = effectPotency + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDamagePotency = effectPotency + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mHealingPotency = (isLevelTwo() ? THURIBLE_HEALING_PERCENT_2 : THURIBLE_HEALING_PERCENT_1)
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEAL);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, EFFECTS_DURATION);
		mPassiveDuration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, PASSIVE_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new ThuribleProcessionCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!isOnCooldown() && mBuffs > 0 && mPlayer.getHealth() - event.getFinalDamage(true) <= EntityUtils.getMaxHealth(mPlayer) * DAMAGE_BREAK_PERCENT) {
			//Give everyone buffs from the array
			if (mBuffs == 4) {
				applyBuffs(mEffectDuration);
				putOnCooldown();
			}

			mCosmetic.endBuildupEffect(mPlayer);
			mSeconds = 0;
			updateBuffs();
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isOnCooldown()) {
			return;
		}
		if (oneSecond) {
			mSeconds++;
			updateBuffs();
			applyBuffs(mPassiveDuration);
		}
		if (mBuffs == 1) {
			mCosmetic.firstBuffs(mPlayer);
		} else if (mBuffs == 2) {
			mCosmetic.secondBuffs(mPlayer);
		} else if (mBuffs == 3) {
			mCosmetic.thirdBuffs(mPlayer);
		} else if (mBuffs == 4) {
			mCosmetic.fourthBuffs(mPlayer);
		}
	}

	//Recounts number of buffs and applies the passive ones
	private void updateBuffs() {
		int previousBuffs = mBuffs;

		//Convert time into number of buffs and cap to maximum effect index for that level
		mBuffs = mSeconds / 4;

		//Cap to 4 effects
		if (mBuffs > MAX_BUFFS) {
			mBuffs = MAX_BUFFS;
		}

		if (previousBuffs != mBuffs) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	//Applies built up buffs to all around them and themselves, take the duration as parameter (passive/built-up)
	private void applyBuffs(int duration) {
		//Give everyone buffs from the array
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), THURIBLE_RADIUS, true);
		for (Player pl : players) {
			Effect[] effects = getEffectArray(duration);
			for (int i = 0; i < mBuffs; i++) {
				mPlugin.mEffectManager.addEffect(pl, EFFECTS_NAMES[i], effects[i]);
			}
		}
	}

	private Effect[] getEffectArray(int duration) {
		return new Effect[] {new PercentAttackSpeed(duration, mAttackSpeedPotency, PERCENT_ATTACK_SPEED_EFFECT_NAME)
								.deleteOnAbilityUpdate(true),
		                     new PercentSpeed(duration, mSpeedPotency, PERCENT_SPEED_EFFECT_NAME)
								.deleteOnAbilityUpdate(true),
		                     new PercentDamageDealt(duration, mDamagePotency).damageTypes(AFFECTED_DAMAGE_TYPES)
								 .deleteOnAbilityUpdate(true),
		                     new ThuribleBonusHealing(duration, mHealingPotency)
								 .deleteOnAbilityUpdate(true)};
	}

	@Override
	public int getCharges() {
		return mBuffs;
	}

	@Override
	public int getMaxCharges() {
		return MAX_BUFFS;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		ClassAbility classAbility = INFO.getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), classAbility);
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GOLD));
		} else {
			output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));
		}
		return output;
	}

	private static Description<ThuribleProcession> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The Hierophant passively builds up buffs, which are applied to all players within ")
			.add(a -> THURIBLE_RADIUS, THURIBLE_RADIUS)
			.add(" blocks. Buffs end and the buildup resets upon taking damage that causes you to drop below ")
			.addPercent(DAMAGE_BREAK_PERCENT)
			.add(" of your max health, unless the full set of buffs have been obtained. Then all players (including the Hierophant) get ")
			.addDuration(a -> a.mEffectDuration, EFFECTS_DURATION)
			.add(" seconds of all built-up buffs. After that, the timer resets and the Procession begins anew. Progression - ")
			.addPercent(a -> a.mAttackSpeedPotency, EFFECT_PERCENT_1, false, Ability::isLevelOne)
			.add(" Attack Speed (after 4s of no health threshold reached), ")
			.addPercent(a -> a.mSpeedPotency, EFFECT_PERCENT_1, false, Ability::isLevelOne)
			.add(" Speed (after 8s of no health threshold reached), ")
			.addPercent(a -> a.mDamagePotency, EFFECT_PERCENT_1, false, Ability::isLevelOne)
			.add(" Attack and Projectile Damage (after 12s of no health threshold reached), Cleric's passive heal is doubled, to ")
			.addPercent(a -> (1 + a.mHealingPotency) * Rejuvenation.PERCENT_HEAL, (1 + THURIBLE_HEALING_PERCENT_1) * Rejuvenation.PERCENT_HEAL, false, Ability::isLevelOne)
			.add(" of max health every 5s (after 16s of no health threshold reached)");
	}

	private static Description<ThuribleProcession> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Progression - ")
			.addPercent(a -> a.mAttackSpeedPotency, EFFECT_PERCENT_2, false, Ability::isLevelTwo)
			.add(" Attack Speed (after 4s of no health threshold reached), ")
			.addPercent(a -> a.mSpeedPotency, EFFECT_PERCENT_2, false, Ability::isLevelTwo)
			.add(" Speed (after 8s of no health threshold reached), ")
			.addPercent(a -> a.mDamagePotency, EFFECT_PERCENT_2, false, Ability::isLevelTwo)
			.add(" Attack and Projectile Damage (after 12s of no health threshold reached), Cleric's passive heal is doubled, to ")
			.addPercent(a -> (1 + a.mHealingPotency) * Rejuvenation.PERCENT_HEAL, (1 + THURIBLE_HEALING_PERCENT_2) * Rejuvenation.PERCENT_HEAL, false, Ability::isLevelTwo)
			.add(" of max health every 5s (after 16s of no health threshold reached)");
	}
}
