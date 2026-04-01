package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DivineJusticeCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.CritScaling;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.DARK_GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class DivineJustice extends Ability implements AbilityWithChargesOrStacks, AbilityWithDuration {
	public static final String NAME = "Divine Justice";
	public static final ClassAbility ABILITY = ClassAbility.DIVINE_JUSTICE;

	public static final int DAMAGE_1 = 2;
	public static final int DAMAGE_2 = 3;
	public static final double[] DAMAGE_MULTIPLIER_1 = {0.15, 0.2, 0.25};
	public static final double[] DAMAGE_MULTIPLIER_2 = {0.3, 0.4, 0.45};
	public static final double HEALING_MULTIPLIER_OWN = 0.05;
	public static final double HEALING_MULTIPLIER_OTHER = 0.05;
	public static final int RADIUS = 12;
	public static final double ENHANCEMENT_BONUS_DAMAGE = 0.2;
	public static final int ENHANCEMENT_BONUS_DAMAGE_DURATION = 2 * 20;
	public static final int ENHANCEMENT_COMBO_TIMER = 5 * 20;
	public static final String CHARM_DAMAGE = "Divine Justice Damage";
	public static final String CHARM_SELF = "Divine Justice Self Heal";
	public static final String CHARM_ALLY = "Divine Justice Ally Heal";
	public static final String CHARM_HEAL_RADIUS = "Divine Justice Ally Heal Radius";
	public static final String CHARM_ENHANCE_DAMAGE = "Divine Justice Enhancement Damage Bonus";
	public static final String CHARM_ENHANCE_PRIME_DURATION = "Divine Justice Enhancement Prime Duration";
	public static final String CHARM_ENHANCE_COMBO_TIMER = "Divine Justice Enhancement Combo Timer";

	public static final AbilityInfo<DivineJustice> INFO =
		new AbilityInfo<>(DivineJustice.class, NAME, DivineJustice::new)
			.linkedSpell(ABILITY)
			.scoreboardId("DivineJustice")
			.shorthandName("DJ")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal extra damage on critical melee or projectile attacks against Heretics, and heal yourself and nearby players when killing them.")
			.displayItem(Material.IRON_SWORD);

	public final DivineJusticeCS mCosmetic;

	private final double mDamage;
	private final double mPercentDamage;
	private final double mSelfHeal;
	private final double mAllyHeal;
	private final double mRadius;
	private final double mEnhanceDamage;
	private final int mEnhanceDuration;
	private int mEnhanceRemainingDuration = 0;
	private int mComboNumber = 0;
	private final int mComboTimer;
	private @Nullable BukkitRunnable mComboRunnable = null;
	private boolean mEnhanceIsReady = false;
	private @Nullable BukkitRunnable mReadyRunnable = null;


	public DivineJustice(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mPercentDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, AbilityUtils.getRegionScaled(player, isLevelOne() ? DAMAGE_MULTIPLIER_1 : DAMAGE_MULTIPLIER_2));
		mSelfHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF, HEALING_MULTIPLIER_OWN);
		mAllyHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, HEALING_MULTIPLIER_OTHER);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_HEAL_RADIUS, RADIUS);
		mEnhanceDamage = ENHANCEMENT_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_DAMAGE);
		mEnhanceDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_PRIME_DURATION, ENHANCEMENT_BONUS_DAMAGE_DURATION);
		mComboTimer = CharmManager.getDuration(player, CHARM_ENHANCE_COMBO_TIMER, ENHANCEMENT_COMBO_TIMER);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new DivineJusticeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mEnhanceIsReady) {
			ClassAbility ability = event.getAbility();
			if (ability != null && ability != ABILITY && ability != ClassAbility.ETHEREAL_ASCENSION && !ability.isFake()) {
				event.updateDamageWithMultiplier(1 + mEnhanceDamage);
			}
		}

		/* Prevent DJ from triggering if the enemy is not affected by Crusade */
		if (!Crusade.enemyTriggersAbilities(enemy)) {
			return false;
		}

		/* DamageEvents do not reliably store whether an event is a crit or not since Cumbersome modifies the crit boolean */
		final boolean isMeleeCrit = (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer));
		final boolean isAscensionOrb = event.getAbility() != null && event.getAbility() == ClassAbility.ETHEREAL_ASCENSION;
		if (isMeleeCrit || isAscensionOrb || (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile
			&& EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			&& MetadataUtils.checkOnceThisTick(mPlugin, enemy, "DivineJustice" + mPlayer.getName()))) { // for Multishot projectiles, we only want to trigger DJ on mobs once, not 3 times

			if (!isAscensionOrb) {
				mCosmetic.justiceOnDamage(mPlayer, enemy, mPlayer.getWorld(), enemy.getLocation(), PartialParticle.getWidthDelta(enemy) * 1.5, mComboNumber, isEnhanced());
			}

			if (mComboNumber == 0 || mComboRunnable != null) {
				if (mComboRunnable != null) {
					mComboRunnable.cancel();
				}
				mComboRunnable = new BukkitRunnable() {
					@Override
					public void run() {
						mComboNumber = 0;
						mComboRunnable = null;
						ClientModHandler.updateAbility(mPlayer, ABILITY);
					}
				};
				mComboRunnable.runTaskLater(mPlugin, mComboTimer);
			}
			mComboNumber++;

			if (mComboNumber >= 3) {
				mComboNumber = 0;

				if (isEnhanced()) {
					mEnhanceIsReady = true;
					mEnhanceRemainingDuration = mEnhanceDuration;
					if (mReadyRunnable != null) {
						mReadyRunnable.cancel();
					}
					mReadyRunnable = new BukkitRunnable() {
						@Override
						public void run() {
							mEnhanceRemainingDuration--;
							if (mEnhanceRemainingDuration <= 0) {
								mEnhanceIsReady = false;
								ClientModHandler.updateAbility(mPlayer, ABILITY);
								this.cancel();
							}
						}
					};
					mReadyRunnable.runTaskTimer(mPlugin, 0, 1);
				}

				if (mComboRunnable != null) {
					mComboRunnable.cancel();
					mComboRunnable = null;
				}
			}
			ClientModHandler.updateAbility(mPlayer, this);
			if (isAscensionOrb) {
				// Only trigger once in a tick per orb
				return true;
			}
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, calculateDamage(event, isMeleeCrit), mInfo.getLinkedSpell(), true, false);
		}
		return false; // keep the ability open for more Multishot crits this tick
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent entityDeathEvent, boolean dropsLoot) {
		if (Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity()) && isLevelTwo()) {
			int duration1 = 20;
			@Nullable Effect djEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, "DivineJusticeSelfHealing");
			if (djEffect != null && djEffect.getDuration() > 0) {
				duration1 = Math.min(duration1 + djEffect.getDuration(), 3 * 20);
			}
			mPlugin.mEffectManager.addEffect(mPlayer, "DivineJusticeSelfHealing", new CustomRegeneration(duration1, EntityUtils.getMaxHealth(mPlayer) * mSelfHeal * 5 / 20, 5, mPlayer, true, mPlugin));

			final List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true);
			players.forEach(otherPlayer -> {
				int duration2 = 20;
				@Nullable Effect djEffect2 = mPlugin.mEffectManager.getActiveEffect(otherPlayer, "DivineJusticeAllyHealing");
				if (djEffect2 != null && djEffect2.getDuration() > 0) {
					duration2 = Math.min(duration2 + djEffect2.getDuration(), 3 * 20);
				}
				mPlugin.mEffectManager.addEffect(otherPlayer, "DivineJusticeAllyHealing", new CustomRegeneration(duration2, EntityUtils.getMaxHealth(otherPlayer) * mAllyHeal * 5 / 20, 5, mPlayer, true, mPlugin));
			});

			players.add(mPlayer);
			mCosmetic.justiceKill(mPlayer, entityDeathEvent.getEntity().getLocation());
			mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchSelf());
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchOther()), 2);
		}
	}

	public double calculateDamage(final DamageEvent event, final boolean isMeleeCrit) {
		final boolean weaponHasCumbersome = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.CUMBERSOME);
		return mDamage + event.getFlatDamage() * (event.getType() == DamageType.MELEE ? mPlugin.mItemStatManager.getAttributeAmount(mPlayer, AttributeType.ATTACK_DAMAGE_MULTIPLY) * (isMeleeCrit && !weaponHasCumbersome ? CritScaling.CRIT_BONUS : 1.0) : mPlugin.mItemStatManager.getAttributeAmount(mPlayer, AttributeType.PROJECTILE_DAMAGE_MULTIPLY)) *
			Math.max(mPercentDamage, 0.0);
	}

	private static Description<DivineJustice> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Critical attacks and projectiles deal")
			.addLine("bonus magic damage (s) to *Heretics*.").styles(Cleric.HERETIC_COLOR)
			.addLine()
			.addStat("Bonus Damage: +%d1 + %p1R (s)")
				.statValues(
					stat(a -> a.mDamage, DAMAGE_1),
					perRegion(a -> a.mPercentDamage, DAMAGE_MULTIPLIER_1[0], DAMAGE_MULTIPLIER_1[1], DAMAGE_MULTIPLIER_1[2]))
			.tab().addLine("(of the attack's damage)")
			.addDashedLine();
	}

	private static Description<DivineJustice> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Divine Justice*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Bonus Damage: +%d1 + %p1 -> +%d2 + %p2R (s)")
				.statValues(
					stat(DAMAGE_1),
					perRegion(DAMAGE_MULTIPLIER_1[0], DAMAGE_MULTIPLIER_1[1], DAMAGE_MULTIPLIER_1[2]),
					stat(a -> a.mDamage, DAMAGE_2),
					perRegion(a -> a.mPercentDamage, DAMAGE_MULTIPLIER_2[0], DAMAGE_MULTIPLIER_2[1], DAMAGE_MULTIPLIER_2[2]))
			.addLine()
			.addLine("Killing a *Heretic* heals yourself").styles(Cleric.HERETIC_COLOR, UNDERLINED)
			.addLine("and other nearby players over time.")
			.addLine("(Duration stacks per kill, up to 3s)")
			.addLine()
			// Don't want to display redundant lines for the majority of cases!
			.addIfElse((a, p) -> a == null || a.mSelfHeal == a.mAllyHeal,
				desc -> desc.addStat("Healing: %p HP over 1s")
					.statValues(stat(a -> a.mSelfHeal, HEALING_MULTIPLIER_OWN)),
				desc -> desc.addStat("Self Healing: %p HP over 1s")
					.statValues(stat(a -> a.mSelfHeal, HEALING_MULTIPLIER_OWN))
					.addStat("Ally Healing: %p HP over 1s")
					.statValues(stat(a -> a.mAllyHeal, HEALING_MULTIPLIER_OTHER)))

			.addStat("Healing Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addDashedLine();
	}

	private static Description<DivineJustice> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("After triggering *Divine Justice* or damaging a mob with an").styles(UNDERLINED)
			.addLine("*Ethereal Ascension* orb *3* times within %t of each other, deal").styles(UNDERLINED, WHITE)
				.statValues(stat(a -> a.mComboTimer, ENHANCEMENT_COMBO_TIMER))
			.addLine("increased damage with your other abilities for a short time.")
			.addLine("*(Doesn't boost Divine Justice's or Ethereal Ascension's*").styles(DARK_GREY)
			.addLine("*damage.)*").styles(DARK_GREY)
			.addLine()
			.addStat("Effect: +%p Ability Damage for %t")
				.statValues(
					stat(a -> a.mEnhanceDamage, ENHANCEMENT_BONUS_DAMAGE),
					stat(a -> a.mEnhanceDuration, ENHANCEMENT_BONUS_DAMAGE_DURATION))
			.addDashedLine();
	}

	@Override
	public int getCharges() {
		return isEnhanced() ? mComboNumber : 0;
	}

	@Override
	public int getMaxCharges() {
		return isEnhanced() ? 3 : 0;
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? mEnhanceDuration : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return isEnhanced() ? mEnhanceRemainingDuration : 0;
	}
}
