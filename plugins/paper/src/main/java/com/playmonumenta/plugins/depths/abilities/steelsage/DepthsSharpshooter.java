package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class DepthsSharpshooter extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Sharpshooter";
	public static final double[] DAMAGE_PER_STACK = {0.024, 0.028, 0.032, 0.036, 0.040, 0.048};
	private static final int SHARPSHOOTER_DECAY_TIMER = 20 * 4;
	private static final int TWISTED_SHARPSHOOTER_DECAY_TIMER = 20 * 6;
	private static final int MAX_STACKS = 8;

	public static final DepthsAbilityInfo<DepthsSharpshooter> INFO =
		new DepthsAbilityInfo<>(DepthsSharpshooter.class, ABILITY_NAME, DepthsSharpshooter::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(Material.TARGET)
			.descriptions(DepthsSharpshooter::getDescription)
			.singleCharm(false);

	private final int mMaxStacks;
	private final int mDecayTimerLength;
	private final double mDamage;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	public DepthsSharpshooter(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxStacks = MAX_STACKS + (int) CharmManager.getLevel(mPlayer, CharmEffects.SHARPSHOOTER_MAX_STACKS.mEffectName);
		mDecayTimerLength = CharmManager.getDuration(mPlayer, CharmEffects.SHARPSHOOTER_DECAY_TIMER.mEffectName, mRarity >= 6 ? TWISTED_SHARPSHOOTER_DECAY_TIMER : SHARPSHOOTER_DECAY_TIMER);
		mDamage = DAMAGE_PER_STACK[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SHARPSHOOTER_DAMAGE_PER_STACK.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE || event.getType() == DamageType.PROJECTILE_SKILL) {
			event.setDamage(event.getDamage() * (1 + mStacks * mDamage));

			// Critical arrow and mob is actually going to take damage
			if (event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)
				    && (enemy.getNoDamageTicks() <= enemy.getMaximumNoDamageTicks() / 2f || enemy.getLastDamage() < event.getFinalDamage(false))) {
				mTicksToStackDecay = mDecayTimerLength;

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
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = mDecayTimerLength;
				mStacks--;
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	private static Description<DepthsSharpshooter> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsSharpshooter>(color)
			.add("Each enemy hit with a critical projectile gives you a stack of Sharpshooter, up to ")
			.add(a -> a.mMaxStacks, MAX_STACKS)
			.add(". Stacks decay after ")
			.addDuration(a -> a.mDecayTimerLength, rarity == 6 ? TWISTED_SHARPSHOOTER_DECAY_TIMER : SHARPSHOOTER_DECAY_TIMER, false, rarity == 6)
			.add(" seconds of not gaining a stack. Each stack increases your projectile damage by ")
			.addPercent(a -> a.mDamage, DAMAGE_PER_STACK[rarity - 1], false, true)
			.add(".");
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return MAX_STACKS;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

}

