package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.SagesInsightCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SagesInsight extends Ability implements AbilityWithChargesOrStacks {
	public static final String NAME = "Sage's Insight";
	private static final int DECAY_TIMER = 20 * 4;
	private static final int MAX_STACKS = 8;
	private static final int SPEED_DURATION = 5 * 20;
	private static final int ABILITIES_COUNT_1 = 2;
	private static final int ABILITIES_COUNT_2 = 3;
	private static final String ATTR_NAME = "SagesExtraSpeedAttr";

	public static final String CHARM_STACKS = "Sage's Insight Stack Trigger Threshold";
	public static final String CHARM_DECAY = "Sage's Insight Decay Duration";
	public static final String CHARM_SPEED = "Sage's Insight Speed Amplifier";
	public static final String CHARM_ABILITY = "Sage's Insight Ability Count";

	public static final AbilityInfo<SagesInsight> INFO =
		new AbilityInfo<>(SagesInsight.class, NAME, SagesInsight::new)
			.linkedSpell(ClassAbility.SAGES_INSIGHT)
			.scoreboardId("SagesInsight")
			.shorthandName("SgI")
			.actionBarColor(TextColor.color(222, 219, 36))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Refresh a spell's cooldown after multiple spells damage enemies in succession.")
			.displayItem(Material.ENDER_EYE);

	private final int mResetSize;
	private final int mMaxStacks;
	private final double mSpeed;
	private final int mDecayTimer;
	private final List<ClassAbility> mResets = new ArrayList<>();
	private final Set<ClassAbility> mCastAbilities = new HashSet<>(); // Set of abilities that have been cast but not yet dealt damage. When they deal damage they are removed from the set and add to mStacks
	private final SagesInsightCS mCosmetic;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;


	public SagesInsight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mResetSize = (isLevelOne() ? ABILITIES_COUNT_1 : ABILITIES_COUNT_2) + (int) CharmManager.getLevel(player, CHARM_ABILITY);
		mMaxStacks = (int) CharmManager.getLevel(player, CHARM_STACKS) + MAX_STACKS;
		mSpeed = CharmManager.getLevelPercentDecimal(player, CHARM_SPEED);
		mDecayTimer = CharmManager.getDuration(mPlayer, CHARM_DECAY, DECAY_TIMER);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SagesInsightCS());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = mDecayTimer;
				mStacks--;
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability == null || ability.isFake()) {
			return false;
		}
		mTicksToStackDecay = mDecayTimer;

		// the cast ability and damage ability for elemental arrows are different
		// elemental arrows enhancement has a cooldown whereas the others don't so that's the only one we care about here
		if (ability == ClassAbility.ELEMENTAL_ARROWS_FIRE || ability == ClassAbility.ELEMENTAL_ARROWS_ICE) {
			ability = ClassAbility.ELEMENTAL_ARROWS;
		}


		if (mCastAbilities.remove(ability)) {
			mStacks++;
			if (mStacks >= mMaxStacks) {
				if (mSpeed > 0) {
					mPlugin.mEffectManager.addEffect(mPlayer, "SagesExtraSpeed",
						new PercentSpeed(SPEED_DURATION, mSpeed, ATTR_NAME).deleteOnAbilityUpdate(true));
				}
				mCosmetic.insightTrigger(mPlugin, mPlayer, mResetSize);

				mStacks = 0;
				for (ClassAbility s : mResets) {
					if (s == ClassAbility.MANA_LANCE) {
						// Special Treatment for Mana Lance because of charged abilities.
						Objects.requireNonNull(mPlugin.mAbilityManager.getPlayerAbility(mPlayer, ManaLance.class)).incrementCharge();
					} else if (s == ClassAbility.MAGMA_SHIELD) {
						// Special Treatment for Magma Shield because of charged abilities.
						Objects.requireNonNull(mPlugin.mAbilityManager.getPlayerAbility(mPlayer, MagmaShield.class)).incrementCharge();
					} else {
						mPlugin.mTimers.removeCooldown(mPlayer, s);
					}
				}
				mResets.clear();
			} else {
				mCosmetic.insightStackGain(mPlayer, event);
				showChargesMessage();
			}
			ClientModHandler.updateAbility(mPlayer, this);
		}
		return false; // only used to check that an ability dealt damage, and does not cause more damage instances.
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		ClassAbility cast = event.getSpell();
		mCastAbilities.add(cast);

		mResets.add(cast);
		if (mResets.size() > mResetSize) {
			mResets.remove(0);
		}
		return true;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks - 1; // -1 as "max stacks" is never reached - it immediately resets back to 0
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	private static Description<SagesInsight> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("If an active spell hits an enemy, you gain an Arcane Insight. Insights stack up to ")
			.add(a -> a.mMaxStacks, MAX_STACKS)
			.add(", but decay every ")
			.addDuration(a -> a.mDecayTimer, DECAY_TIMER)
			.add(" seconds of not gaining one. Once ")
			.add(a -> a.mMaxStacks, MAX_STACKS)
			.add(" Insights are revealed, the cooldowns of the previous ")
			.add(a -> a.mResetSize, ABILITIES_COUNT_1, false, Ability::isLevelOne)
			.add(" spells cast are refreshed. This sets your Insights back to 0.");
	}

	private static Description<SagesInsight> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Sage's Insight now refreshes the cooldowns of your previous ")
			.add(a -> a.mResetSize, ABILITIES_COUNT_2, false, Ability::isLevelTwo)
			.add(" spells upon activating.");
	}
}
