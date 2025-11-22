package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ByMyBladeCS;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class ByMyBlade extends Ability {
	private static final int HASTE_POTENCY = 1;
	private static final double ATTACK_SPEED_2 = 0.2;
	private static final String ATTACK_SPEED_SRC = "ByMyBladeAttackSpeed";
	private static final int ATTACK_SPEED_DURATION = TICKS_PER_SECOND * 4;
	private static final int DAMAGE_1 = 12;
	private static final int DAMAGE_2 = 24;
	private static final int COOLDOWN = TICKS_PER_SECOND * 10;
	private static final double ENHANCEMENT_HEAL_PERCENT = 0.05;
	private static final double ENHANCEMENT_HEAL_PERCENT_ELITE = 0.15;
	private static final double ENHANCEMENT_DAMAGE_MULT = 0.2;

	public static final String CHARM_DAMAGE = "By My Blade Damage";
	public static final String CHARM_COOLDOWN = "By My Blade Cooldown";
	public static final String CHARM_ATTACK_SPEED_AMPLIFIER = "By My Blade Attack Speed Amplifier";
	public static final String CHARM_HASTE_AMPLIFIER = "By My Blade Haste Amplifier";
	public static final String CHARM_ATTACK_SPEED_DURATION = "By My Blade Attack Speed Duration";
	public static final String CHARM_HEALTH = "By My Blade Enhancement Health";
	public static final String CHARM_ELITE_HEALTH = "By My Blade Enhancement Elite Health";

	public static final AbilityInfo<ByMyBlade> INFO =
		new AbilityInfo<>(ByMyBlade.class, "By My Blade", ByMyBlade::new)
			.linkedSpell(ClassAbility.BY_MY_BLADE)
			.scoreboardId("ByMyBlade")
			.shorthandName("BmB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Critical melee hits periodically do more damage and give Attack Speed.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SKELETON_SKULL);

	private final double mDamageBonusBase;
	private final double mDamageBonus;
	private final double mAttackSpeedAmplifier;
	private final int mHasteAmplifier;
	private final int mAttackSpeedDuration;
	private final double mEnhancementHeal;
	private final double mEnhancementHealElite;
	private final ByMyBladeCS mCosmetic;

	public ByMyBlade(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mDamageBonusBase = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, (isLevelTwo() ? DAMAGE_2 : DAMAGE_1));
		mDamageBonus = mDamageBonusBase * (isEnhanced() ? 1 + ENHANCEMENT_DAMAGE_MULT : 1);
		mAttackSpeedAmplifier = ATTACK_SPEED_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK_SPEED_AMPLIFIER);
		mHasteAmplifier = HASTE_POTENCY + (int) CharmManager.getLevel(mPlayer, CHARM_HASTE_AMPLIFIER);
		mAttackSpeedDuration = CharmManager.getDuration(mPlayer, CHARM_ATTACK_SPEED_DURATION, ATTACK_SPEED_DURATION);
		mEnhancementHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALTH, ENHANCEMENT_HEAL_PERCENT);
		mEnhancementHealElite = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ELITE_HEALTH, ENHANCEMENT_HEAL_PERCENT_ELITE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new ByMyBladeCS());
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && !isOnCooldown() && PlayerUtils.isFallingAttack(mPlayer)
			&& InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mDamageBonus, mInfo.getLinkedSpell(), true);
			mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, mAttackSpeedDuration, mHasteAmplifier, true));

			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(mPlayer, ATTACK_SPEED_SRC, new PercentAttackSpeed(mAttackSpeedDuration,
					mAttackSpeedAmplifier, ATTACK_SPEED_SRC));
			}

			putOnCooldown();

			if (isEnhanced()) {
				// This might be a bit scuffed... but hopefully it feels better this way.
				// As BMB applies first before melee hit, if the enemy survives BMB but dies to melee
				// It doesn't heal the player. So we delay this check by 1 tick.
				cancelOnDeath(new BukkitRunnable() {
					@Override
					public void run() {
						if (enemy.isDead() || !enemy.isValid()) {
							// Heal Player - 5% normal, 15% elite or boss
							PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer)
								* ((EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy)) ? mEnhancementHealElite : mEnhancementHeal));
							mCosmetic.bmbHeal(mPlayer, enemy.getLocation().add(0, 1, 0));
						}
					}
				}.runTaskLater(mPlugin, 1));
			}

			int level = 1;
			if (isLevelTwo()) {
				mCosmetic.bmbDamageLv2(mPlayer, enemy);
				level = 2;
			}
			mCosmetic.bmbDamage(mPlayer.getWorld(), mPlayer, enemy, level);
		}
		return false;
	}

	private static Description<ByMyBlade> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("While holding two swords, performing a critical melee attack deals ")
			.add(a -> a.mDamageBonusBase, DAMAGE_1, false, Ability::isLevelOne)
			.add(" melee damage to the hit enemy and grants Haste ")
			.addPotionAmplifier(a -> HASTE_POTENCY, HASTE_POTENCY)
			.add(" for ")
			.addDuration(a -> a.mAttackSpeedDuration, ATTACK_SPEED_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<ByMyBlade> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamageBonusBase, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and additionally gain ")
			.addPercent(a -> a.mAttackSpeedAmplifier, ATTACK_SPEED_2)
			.add(" attack speed for ")
			.addDuration(a -> a.mAttackSpeedDuration, ATTACK_SPEED_DURATION)
			.add(" seconds.");
	}

	private static Description<ByMyBlade> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("By My Blade does ")
			.addPercent(ENHANCEMENT_DAMAGE_MULT)
			.add(" extra damage. Killing an enemy with this ability heals you for ")
			.addPercent(a -> a.mEnhancementHeal, ENHANCEMENT_HEAL_PERCENT)
			.add(" of your max health which is increased to ")
			.addPercent(a -> a.mEnhancementHealElite, ENHANCEMENT_HEAL_PERCENT_ELITE)
			.add(" if the target was an Elite or Boss mob.");
	}
}
