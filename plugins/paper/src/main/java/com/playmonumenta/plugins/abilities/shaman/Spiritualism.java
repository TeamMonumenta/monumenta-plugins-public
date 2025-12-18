package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.SpiritualismCS;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Spiritualism extends Ability {

	private static final int BONUS_BUFF_DURATION = 20; // 1s application
	private static final String BONUS_HEALING_SOURCE = "SpiritualismBonusHealing";
	private static final double BASE_HEALING_L1 = 0.1;
	private static final double BASE_HEALING_L2 = 0.2;
	private static final double TOTEM_HEALING = 0.05;
	private static final double TOTEM_DMG_BOOST_L1 = 0.1;
	private static final double TOTEM_DMG_BOOST_L2 = 0.2;
	private static final int TOTEM_COOLDOWN_REFUND_CAP = 2 * 20;
	private static final double TOTEM_COOLDOWN_REFUND_PERCENT = 0.15;
	private static final List<ClassAbility> REDUCIBLE_ABILITIES = List.of(
		ClassAbility.TOTEMIC_CONSECRATION,
		ClassAbility.CHAIN_LIGHTNING,
		ClassAbility.DEVASTATION,
		ClassAbility.EARTHEN_TREMOR,
		ClassAbility.IGNITION_DRIVE
	);

	public static final String CHARM_HEALING = "Spiritualism Base Healing";
	public static final String CHARM_BONUS_HEALING = "Spiritualism Bonus Healing";
	public static final String CHARM_DAMAGE_BOOST = "Spiritualism Totem Damage Boost";
	public static final String CHARM_COOLDOWN_REFUND_PERCENT = "Spiritualism Cooldown Refund Amplifier";
	public static final String CHARM_COOLDOWN_REFUND_CAP = "Spiritualism Cooldown Refund Cap";

	public static final AbilityInfo<Spiritualism> INFO =
		new AbilityInfo<>(Spiritualism.class, "Spiritualism", Spiritualism::new)
			.scoreboardId("Spiritualism")
			.shorthandName("Spi")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Increase your healing rate and empower your totems.")
			.displayItem(Material.GOLDEN_CHESTPLATE);

	private final double mHealing;
	private final double mBonusHealing;
	private final double mTotemDamageBoost;
	private final int mNonTotemCooldownRefundCap;
	private final double mNonTotemCooldownRefundPercent;
	private final SpiritualismCS mCosmetic;

	private @Nullable ShamanPassiveManager mShamanPassiveManager;

	public Spiritualism(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHealing = (isLevelOne() ? BASE_HEALING_L1 : BASE_HEALING_L2) + CharmManager.getLevelPercentDecimal(player, CHARM_HEALING);
		mBonusHealing = TOTEM_HEALING + CharmManager.getLevelPercentDecimal(player, CHARM_BONUS_HEALING);
		mTotemDamageBoost = (isLevelOne() ? TOTEM_DMG_BOOST_L1 : TOTEM_DMG_BOOST_L2) + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE_BOOST);
		mNonTotemCooldownRefundCap = CharmManager.getDuration(mPlayer, CHARM_COOLDOWN_REFUND_CAP, TOTEM_COOLDOWN_REFUND_CAP);
		mNonTotemCooldownRefundPercent = TOTEM_COOLDOWN_REFUND_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_COOLDOWN_REFUND_PERCENT);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SpiritualismCS());

		Bukkit.getScheduler().runTask(plugin, () -> mShamanPassiveManager = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, ShamanPassiveManager.class));
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * (1 + mHealing));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (!twoHertz || mPlayer == null || mPlayer.isDead() || mShamanPassiveManager == null) { // only check twice per second
			return;
		}

		List<TotemAbility> totemAbilites = mShamanPassiveManager.getTotemAbilities();
		List<Player> playersInTotemRanges = mShamanPassiveManager.getPlayersInTotemRanges();
		if (playersInTotemRanges == null || playersInTotemRanges.isEmpty()) { // no players in totem range, reset multipliers
			for (TotemAbility totemAbility : totemAbilites) {
				totemAbility.mSpiritualismMultiplier = 1;
			}
			return;
		}

		// At this point there must be at least 1 player in a totem range, so apply the damage boost to each totem
		for (TotemAbility totemAbility : totemAbilites) {
			if (!totemAbility.getPlayersInRange().isEmpty()) {
				totemAbility.mSpiritualismMultiplier = 1 + mTotemDamageBoost;
			} else {
				totemAbility.mSpiritualismMultiplier = 1;
			}
		}

		for (Player player : playersInTotemRanges) {
			if (player.equals(mPlayer)) {
				continue;
			}
			mPlugin.mEffectManager.addEffect(player, BONUS_HEALING_SOURCE, new PercentHeal(BONUS_BUFF_DURATION, mBonusHealing).deleteOnAbilityUpdate(true).displaysTime(false));
		}
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!isEnhanced() || mShamanPassiveManager == null) {
			return true;
		}

		List<Player> playersInTotemRange = mShamanPassiveManager.getPlayersInTotemRanges();
		if (playersInTotemRange == null || playersInTotemRange.isEmpty()) {
			return true;
		}

		if (playersInTotemRange.contains(mPlayer)) {
			ClassAbility ability = event.getSpell();
			if (REDUCIBLE_ABILITIES.contains(ability)) {
				Ability actualAbility = mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbility(ability);
				if (actualAbility == null) {
					return true;
				}
				int cooldownPercent = (int) (mNonTotemCooldownRefundPercent * actualAbility.getModifiedCooldown());
				mPlugin.mTimers.updateCooldown(mPlayer, ability, Math.min(mNonTotemCooldownRefundCap, cooldownPercent));
				mCosmetic.onCooldownRefresh(mPlayer);
			}
		}

		return true;
	}

	private static Description<Spiritualism> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Gain increased healing. Other players")
			.addLine("inside a *Totem's* area gain increased").styles(Shaman.TOTEM_COLOR)
			.addLine("healing as well.")
			.addLine()
			.addStat("Self Effect: +%p1 Healing")
				.statValues(stat(a -> a.mHealing, BASE_HEALING_L1))
			.addStat("Ally Effect: +%p Healing")
				.statValues(stat(a -> a.mBonusHealing, TOTEM_HEALING))
			.addLine()
			.addLine("*Totems* deal increased damage while").styles(Shaman.TOTEM_COLOR)
			.addLine("a player is within their area.")
			.addLine()
			.addStat("Damage Boost: +%p1 (s)")
				.statValues(stat(a -> a.mTotemDamageBoost, TOTEM_DMG_BOOST_L1))
			.addDashedLine();
	}

	private static Description<Spiritualism> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Spiritualism*'s healing boost").styles(UNDERLINED)
			.addLine("and *Totem* damage boost.").styles(Shaman.TOTEM_COLOR)
			.addLine()
			.addStatComparison("Self Effect: +%p1 -> +%p2 Healing")
				.statValues(stat(BASE_HEALING_L1), stat(a -> a.mHealing, BASE_HEALING_L2))
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (s)")
				.statValues(stat(TOTEM_DMG_BOOST_L1), stat(a -> a.mTotemDamageBoost, TOTEM_DMG_BOOST_L2))
			.addDashedLine();
	}

	private static Description<Spiritualism> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Casting a non-*Totem* ability while inside").styles(Shaman.TOTEM_COLOR)
			.addLine("a *Totem's* area reduces the cooldown").styles(Shaman.TOTEM_COLOR)
			.addLine("of that ability.")
			.addLine()
			.addStat("Cooldown Reduction: %p (max %t)")
				.statValues(stat(a -> a.mNonTotemCooldownRefundPercent, TOTEM_COOLDOWN_REFUND_PERCENT),
					stat(a -> a.mNonTotemCooldownRefundCap, TOTEM_COOLDOWN_REFUND_CAP))
			.addDashedLine();
	}
}
