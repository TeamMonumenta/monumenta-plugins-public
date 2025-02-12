package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SharpshooterCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class Sharpshooter extends Ability implements AbilityWithChargesOrStacks {
	private static final double PERCENT_BASE_DAMAGE = 0.2;
	private static final int SHARPSHOOTER_DECAY_TIMER = TICKS_PER_SECOND * 5;
	private static final int MAX_STACKS = 8;
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
			.descriptions(
				String.format("Your projectiles deal %s more damage.",
					StringUtils.multiplierToPercentageWithSign(PERCENT_BASE_DAMAGE)),
				String.format("Each enemy hit with a critical projectile gives you a stack of Sharpshooter, up to " +
					"%s. Stacks decay after %ss of not gaining a stack. Each stack increases the damage " +
					"bonus by an additional %s. Additionally, gain a %s chance to not consume arrows.",
					MAX_STACKS,
					StringUtils.ticksToSeconds(SHARPSHOOTER_DECAY_TIMER),
					StringUtils.multiplierToPercentageWithSign(PERCENT_DAMAGE_PER_STACK),
					StringUtils.multiplierToPercentageWithSign(ARROW_SAVE_CHANCE)),
				String.format("The damage bonus is further increased by %s per block of distance between you and " +
					"the target, up to %s blocks.",
					StringUtils.multiplierToPercentageWithSign(DAMAGE_PER_BLOCK),
					MAX_DISTANCE))
			.simpleDescription("Gain increased projectile damage. Landing shots further increases damage.")
			.displayItem(Material.TARGET);

	private final int mMaxStacks;
	private final int mDecayTime;
	private final double mArrowSaveChance;
	private final SharpshooterCS mCosmetic;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	public Sharpshooter(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mMaxStacks = MAX_STACKS + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mDecayTime = CharmManager.getDuration(mPlayer, CHARM_DECAY, SHARPSHOOTER_DECAY_TIMER);
		mArrowSaveChance = ARROW_SAVE_CHANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RETRIEVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SharpshooterCS());
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		final DamageType type = event.getType();
		final boolean huntingCompanion = event.getAbility() == ClassAbility.HUNTING_COMPANION;

		mCosmetic.hitEffect(mPlayer, enemy);
		if (huntingCompanion || type == DamageType.PROJECTILE || type == DamageType.PROJECTILE_SKILL) {
			double multiplier = 1 + PERCENT_BASE_DAMAGE;
			if (!huntingCompanion) {
				if (isLevelTwo()) {
					multiplier += mStacks * (PERCENT_DAMAGE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STACK_DAMAGE));
				}
				if (isEnhanced()) {
					multiplier += Math.min(enemy.getLocation().distance(mPlayer.getLocation()), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DISTANCE, MAX_DISTANCE)) * DAMAGE_PER_BLOCK;
				}
			} else {
				// half stack bonus for hunting companion
				multiplier += mStacks * (PERCENT_DAMAGE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STACK_DAMAGE)) / 2;
			}

			event.updateDamageWithMultiplier(multiplier);

			if (!huntingCompanion && isLevelTwo() && (enemy.getNoDamageTicks() <= enemy.getMaximumNoDamageTicks() / 2f || enemy.getLastDamage() < event.getDamage())
				&& (type != DamageType.PROJECTILE || (event.getDamager() instanceof final Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)))) {
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
		if (isLevelTwo()) {
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
		} else {
			return Component.text("");
		}
	}
}
