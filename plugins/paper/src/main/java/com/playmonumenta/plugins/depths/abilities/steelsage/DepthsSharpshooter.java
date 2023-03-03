package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public class DepthsSharpshooter extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Sharpshooter";
	public static final double[] DAMAGE_PER_STACK = {0.025, 0.032, 0.038, 0.044, 0.050, 0.07};
	private static final int SHARPSHOOTER_DECAY_TIMER = 20 * 4;
	private static final int TWISTED_SHARPSHOOTER_DECAY_TIMER = 20 * 6;
	private static final int MAX_STACKS = 8;

	public static final DepthsAbilityInfo<DepthsSharpshooter> INFO =
		new DepthsAbilityInfo<>(DepthsSharpshooter.class, ABILITY_NAME, DepthsSharpshooter::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.TARGET))
			.descriptions(DepthsSharpshooter::getDescription);

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;
	private int mDecayTimerLength;

	public DepthsSharpshooter(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDecayTimerLength = mRarity >= 6 ? TWISTED_SHARPSHOOTER_DECAY_TIMER : SHARPSHOOTER_DECAY_TIMER;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE) {
			event.setDamage(event.getDamage() * (1 + mStacks * DAMAGE_PER_STACK[mRarity - 1]));

			// Critical arrow and mob is actually going to take damage
			if (event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)
				    && (enemy.getNoDamageTicks() <= enemy.getMaximumNoDamageTicks() / 2f || enemy.getLastDamage() < event.getFinalDamage(false))
				    && !projectile.hasMetadata("RapidFireArrow")) {
				mTicksToStackDecay = mDecayTimerLength;

				if (mStacks < MAX_STACKS) {
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

	public static void addStacks(Player player, int stacks) {
		DepthsSharpshooter ss = AbilityManager.getManager().getPlayerAbility(player, DepthsSharpshooter.class);
		if (ss != null) {
			ss.mStacks = Math.min(MAX_STACKS, ss.mStacks + stacks);
			ss.showChargesMessage();
			ClientModHandler.updateAbility(ss.mPlayer, ss);
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component decay = rarity == 6 ? Component.text(TWISTED_SHARPSHOOTER_DECAY_TIMER / 20, color) : Component.text(SHARPSHOOTER_DECAY_TIMER);
		return Component.text("Each enemy hit with a critical projectile gives you a stack of Sharpshooter, up to " + MAX_STACKS + ". Stacks decay after ")
			.append(decay)
			.append(Component.text(" seconds of not gaining a stack. Each stack increases your projectile damage by "))
			.append(Component.text(StringUtils.multiplierToPercentage(DAMAGE_PER_STACK[rarity - 1]) + "%", color))
			.append(Component.text("."));
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return MAX_STACKS;
	}

}

