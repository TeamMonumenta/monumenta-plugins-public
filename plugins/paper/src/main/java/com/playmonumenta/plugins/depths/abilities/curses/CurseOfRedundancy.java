package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CurseOfRedundancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Curse of Redundancy";
	public static final double RECHARGE_REDUCTION = 0.5;
	public static final int DURATION = 5 * 20;
	public static final String RECHARGE_EFFECT = "CurseOfRedundancyRechargeEffect";

	public static final DepthsAbilityInfo<CurseOfRedundancy> INFO =
		new DepthsAbilityInfo<>(CurseOfRedundancy.class, ABILITY_NAME, CurseOfRedundancy::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.BRICK)
			.descriptions(CurseOfRedundancy::getDescription);

	private @Nullable DepthsTree mLastTree = null;

	public CurseOfRedundancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!(event.getAbility() instanceof DepthsAbility ability)) {
			return true;
		}

		DepthsAbilityInfo<?> info = ability.getInfo();
		if (info == Bulwark.INFO || info == DepthsDodging.INFO || info.getDepthsTrigger() == DepthsTrigger.LIFELINE) {
			return true;
		}

		DepthsTree tree = info.getDepthsTree();
		if (tree == null) {
			return true;
		}

		if (tree == mLastTree) {
			mLastTree = null;
			mPlugin.mEffectManager.addEffect(mPlayer, RECHARGE_EFFECT, new AbilityCooldownRechargeRate(DURATION, -RECHARGE_REDUCTION));

			// Stolen effects from multiplicity because I am lazy
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 1);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 0.75f), 4);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 0.5f), 8);
		} else {
			mLastTree = tree;
		}

		return true;
	}

	private static Description<CurseOfRedundancy> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After casting 2 abilities from the same tree in a row, all of your abilities' cooldowns recharge ")
			.addPercent(RECHARGE_REDUCTION)
			.add(" slower for ")
			.addDuration(DURATION)
			.add(" seconds.");
	}
}
