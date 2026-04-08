package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SharpshooterCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Sharpshooter extends Ability implements AbilityWithChargesOrStacks {
	private static final double PERCENT_BASE_DAMAGE = 0.15;
	private static final double PERCENT_BASE_DAMAGE_L2 = 0.2;
	private static final int SHARPSHOOTER_DECAY_TIMER = TICKS_PER_SECOND * 5;
	private static final int MAX_STACKS = 4;
	private static final int MAX_STACKS_2 = 8;
	private static final double PERCENT_DAMAGE_PER_STACK = 0.03;
	private static final double DAMAGE_PER_BLOCK = 0.015;
	private static final double MAX_DISTANCE = 16;
	private static final double ARROW_SAVE_CHANCE = 0.2;

	public static final String CHARM_STACK_DAMAGE = "Sharpshooter Stack Damage";
	public static final String CHARM_STACKS = "Sharpshooter Max Stacks";
	public static final String CHARM_RETRIEVAL = "Sharpshooter Arrow Save Chance";
	public static final String CHARM_DECAY = "Sharpshooter Stack Decay Time";
	public static final String CHARM_DISTANCE = "Sharpshooter Enhancement Max Distance";

	public static final AbilityInfo<Sharpshooter> INFO =
		new AbilityInfo<>(Sharpshooter.class, "Sharpshooter", Sharpshooter::new)
			.scoreboardId("Sharpshooter")
			.shorthandName("Ss")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Gain increased projectile damage. Landing shots further increases damage.")
			.displayItem(Material.TARGET);

	private final int mMaxStacks;
	private final int mDecayTime;
	private final double mDamagePerStack;
	private final double mArrowSaveChance;
	private final double mMaxDistance;
	private final SharpshooterCS mCosmetic;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	public Sharpshooter(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mMaxStacks = (isLevelTwo() ? MAX_STACKS_2 : MAX_STACKS) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mDecayTime = CharmManager.getDuration(mPlayer, CHARM_DECAY, SHARPSHOOTER_DECAY_TIMER);
		mDamagePerStack = PERCENT_DAMAGE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STACK_DAMAGE);
		mArrowSaveChance = ARROW_SAVE_CHANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RETRIEVAL);
		mMaxDistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DISTANCE, MAX_DISTANCE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SharpshooterCS());
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		final boolean huntingCompanion = event.getAbility() == ClassAbility.HUNTING_COMPANION;
		final DamageEvent.DamageType type = event.getType();

		mCosmetic.hitEffect(mPlayer, enemy);
		if (huntingCompanion || DamageEvent.DamageType.getAllProjectileTypes().contains(type)) {
			double multiplier = 1 + (isLevelTwo() ? PERCENT_BASE_DAMAGE_L2 : PERCENT_BASE_DAMAGE);
			if (!huntingCompanion) {
				multiplier += mStacks * mDamagePerStack;
				if (isEnhanced()) {
					multiplier += Math.min(enemy.getLocation().distance(mPlayer.getLocation()), mMaxDistance) * DAMAGE_PER_BLOCK;
				}
			} else {
				// half stack bonus for hunting companion
				multiplier += mStacks * mDamagePerStack / 2;
			}

			event.updateDamageWithMultiplier(multiplier);

			if (!huntingCompanion
				&& ((event.getDamager() instanceof final Projectile projectile
				&& EntityUtils.isAbilityTriggeringProjectile(projectile, true))
				|| type == DamageEvent.DamageType.PROJECTILE_SKILL)) {
				mTicksToStackDecay = mDecayTime;

				if (mStacks < mMaxStacks) {
					mStacks++;
					showChargesMessage();
					ClientModHandler.updateAbility(mPlayer, this);
				}
			}
		}

		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = mDecayTime;
				mStacks--;
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public boolean playerConsumeArrowEvent() {
		if (isLevelTwo() && FastUtils.RANDOM.nextDouble() < mArrowSaveChance) {
			mCosmetic.arrowSave(mPlayer);
			return false;
		}
		return true;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();
		final int charges = getCharges();
		final int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges,
			(charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	private static Description<Sharpshooter> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Deal increased projectile damage.")
			.addLine()
			.addLine("Hitting a mob with a critical projectile")
			.addLine("gives you *1* stack of *Sharpshooter*,").styles(WHITE, UNDERLINED)
			.addLine("which decays after %t of not gaining any.")
				.statValues(stat(a -> a.mDecayTime, SHARPSHOOTER_DECAY_TIMER))
			.addLine()
			.addLine("Each stack increases the projectile")
			.addLine("damage boost even further.")
			.addLine()
			.addStat("Damage Boost: +%p1 (p), +%p per stack")
				.statValues(stat(a -> PERCENT_BASE_DAMAGE, PERCENT_BASE_DAMAGE), stat(a -> a.mDamagePerStack, PERCENT_DAMAGE_PER_STACK))
			.addStat("Max Stacks: %d1")
				.statValues(stat(a -> a.mMaxStacks, MAX_STACKS))
			.addDashedLine();
	}

	private static Description<Sharpshooter> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Sharpshooter*'s projectile").styles(UNDERLINED)
			.addLine("damage boost even further, and")
			.addLine("increase its maximum stacks.")
			.addLine()
			.addLine("Passively gain a %p chance not to")
				.statValues(stat(a -> a.mArrowSaveChance, ARROW_SAVE_CHANCE))
			.addLine("consume arrows.")
			.addLine()
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (p)")
				.statValues(stat(PERCENT_BASE_DAMAGE), stat(a -> PERCENT_BASE_DAMAGE_L2, PERCENT_BASE_DAMAGE_L2))
			.addStatComparison("Max Stacks: %d1 -> %d2")
				.statValues(stat(MAX_STACKS), stat(a -> a.mMaxStacks, MAX_STACKS_2))
			.addDashedLine();
	}

	private static Description<Sharpshooter> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Sharpshooter*'s damage boost").styles(UNDERLINED)
			.addLine("even further for each block between you")
			.addLine("and the target.")
			.addLine()
			.addStat("Damage Boost: +%p (p) per block")
				.statValues(stat(DAMAGE_PER_BLOCK))
			.tab().addLine("(max %d blocks)")
				.statValues(stat(a -> a.mMaxDistance, MAX_DISTANCE))
			.addDashedLine();
	}
}
