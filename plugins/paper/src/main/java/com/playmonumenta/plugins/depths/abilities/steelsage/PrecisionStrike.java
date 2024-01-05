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
import com.playmonumenta.plugins.effects.PrecisionStrikeDamage;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.Nullable;

public class PrecisionStrike extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Precision Strike";
	public static final double[] DAMAGE = {3.5, 4.0, 4.5, 5, 5.5, 7};
	public static final double DISTANCE = 6;
	public static final int MAX_STACKS = 3;
	public static final String SOURCE = "PrecisionStrikeDamageEffect";

	public static final DepthsAbilityInfo<PrecisionStrike> INFO =
		new DepthsAbilityInfo<>(PrecisionStrike.class, ABILITY_NAME, PrecisionStrike::new, DepthsTree.STEELSAGE, DepthsTrigger.SPAWNER)
			.displayItem(Material.IRON_TRAPDOOR)
			.descriptions(PrecisionStrike::getDescription)
			.singleCharm(false);

	private final int mMaxStacks;
	private final double mDamage;
	private final double mDistance;

	public PrecisionStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxStacks = MAX_STACKS + (int) CharmManager.getLevel(mPlayer, CharmEffects.PRECISION_STRIKE_MAX_STACKS.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PRECISION_STRIKE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDistance = CharmManager.getRadius(mPlayer, CharmEffects.PRECISION_STRIKE_RANGE.mEffectName, DISTANCE);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int rarity) {
		int maxStacks = MAX_STACKS + (int) CharmManager.getLevel(player, CharmEffects.PRECISION_STRIKE_MAX_STACKS.mEffectName);
		double damage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.PRECISION_STRIKE_DAMAGE.mEffectName, DAMAGE[rarity - 1]);
		double distance = CharmManager.getRadius(player, CharmEffects.PRECISION_STRIKE_RANGE.mEffectName, DISTANCE);
		onSpawnerBreak(plugin, player, maxStacks, damage, distance);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int maxStacks, double damage, double distance) {
		int stacks;
		PrecisionStrikeDamage effect = getActiveEffect(plugin, player);
		if (effect != null) {
			int oldStacks = effect.getStacks();
			if (oldStacks >= maxStacks) {
				return;
			}
			stacks = Math.min(effect.getStacks() + 1, maxStacks);
			effect.setDuration(1);
		} else {
			stacks = 1;
		}
		plugin.mEffectManager.addEffect(player, SOURCE, new PrecisionStrikeDamage(72000, stacks, damage, distance * distance));
		player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.6f, 0.5f * (float) Math.pow(1.5, stacks - 1));
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlugin, mPlayer, mMaxStacks, mDamage, mDistance);
		}
		return true;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public int getCharges() {
		PrecisionStrikeDamage effect = getActiveEffect(Plugin.getInstance(), mPlayer);
		if (effect != null) {
			return effect.getStacks();
		}
		return 0;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	public static @Nullable PrecisionStrikeDamage getActiveEffect(Plugin plugin, Player player) {
		return plugin.mEffectManager.getActiveEffect(player, PrecisionStrikeDamage.class);
	}

	private static Description<PrecisionStrike> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<PrecisionStrike>(color)
			.add("Breaking a spawner adds ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to your next attack against a mob that is more than ")
			.add(a -> a.mDistance, DISTANCE, true)
			.add(" blocks away. Stacks up to ")
			.add(a -> a.mMaxStacks, MAX_STACKS)
			.add(" times.");
	}
}
