package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class DepthsSharpshooter extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Sharpshooter";
	public static final double[] DAMAGE_PER_STACK = {0.025, 0.032, 0.038, 0.044, 0.050, 0.062};
	private static final int SHARPSHOOTER_DECAY_TIMER = 20 * 4;
	private static final int MAX_STACKS = 8;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	public DepthsSharpshooter(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TARGET;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof AbstractArrow arrow) {
			event.setDamage(event.getDamage() * (1 + mStacks * DAMAGE_PER_STACK[mRarity - 1]));

			// Critical arrow and mob is actually going to take damage
			if ((arrow.isCritical() || arrow instanceof Trident) && (enemy.getNoDamageTicks() <= 10 || enemy.getLastDamage() < event.getDamage()) && !arrow.hasMetadata("RapidFireArrow")) {
				mTicksToStackDecay = SHARPSHOOTER_DECAY_TIMER;

				if (mStacks < MAX_STACKS) {
					mStacks++;
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter Stacks: " + mStacks);
					ClientModHandler.updateAbility(mPlayer, this);
				}
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer == null) {
			return;
		}
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = SHARPSHOOTER_DECAY_TIMER;
				mStacks--;
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter Stacks: " + mStacks);
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	public static void addStacks(Plugin plugin, Player player, int stacks) {
		DepthsSharpshooter ss = AbilityManager.getManager().getPlayerAbility(player, DepthsSharpshooter.class);
		if (ss != null) {
			ss.mStacks = Math.min(MAX_STACKS, ss.mStacks + stacks);
			MessagingUtils.sendActionBarMessage(plugin, player, "Sharpshooter Stacks: " + ss.mStacks);
			ClientModHandler.updateAbility(ss.mPlayer, ss);
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Each enemy hit with a critical arrow or trident gives you a stack of Sharpshooter, up to " + MAX_STACKS + ". Stacks decay after " + SHARPSHOOTER_DECAY_TIMER / 20 + " seconds of not gaining a stack. Each stack increases your arrow and trident damage by " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(DAMAGE_PER_STACK[rarity - 1]) + "%" + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
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

