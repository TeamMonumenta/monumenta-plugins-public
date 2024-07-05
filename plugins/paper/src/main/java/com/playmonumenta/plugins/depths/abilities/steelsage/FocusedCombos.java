package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class FocusedCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Focused Combos";
	public static final double[] DAMAGE = {0.30, 0.35, 0.40, 0.45, 0.50, 0.80};
	public static final double BLEED_AMOUNT = 0.2;
	public static final int BLEED_DURATION = 20 * 3;
	public static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<FocusedCombos> INFO =
		new DepthsAbilityInfo<>(FocusedCombos.class, ABILITY_NAME, FocusedCombos::new, DepthsTree.STEELSAGE, DepthsTrigger.COMBO)
			.displayItem(Material.SPECTRAL_ARROW)
			.descriptions(FocusedCombos::getDescription)
			.singleCharm(false);

	private final int mBleedDuration;
	private final double mBleedAmount;
	private final double mDamage;

	public FocusedCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.FOCUSED_COMBOS_HIT_REQUIREMENT.mEffectName);
		mBleedDuration = CharmManager.getDuration(mPlayer, CharmEffects.FOCUSED_COMBOS_BLEED_DURATION.mEffectName, BLEED_DURATION);
		mBleedAmount = BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FOCUSED_COMBOS_BLEED_AMPLIFIER.mEffectName);
		mDamage = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FOCUSED_COMBOS_DAMAGE_MULTIPLIER.mEffectName);
	}

	@Override
	public boolean triggersCombos(DamageEvent event) {
		return event.getDamager() instanceof Projectile proj && event.getType() == DamageType.PROJECTILE && EntityUtils.isAbilityTriggeringProjectile(proj, true);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		EntityUtils.applyBleed(mPlugin, mBleedDuration, mBleedAmount, enemy);
		event.updateDamageWithMultiplier(1 + mDamage);

		Location playerLoc = mPlayer.getLocation();
		mPlayer.playSound(playerLoc, Sound.BLOCK_WEEPING_VINES_BREAK, SoundCategory.PLAYERS, 2, 0.8f);
		mPlayer.playSound(playerLoc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4f, 1.75f);
	}

	private static Description<FocusedCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FocusedCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" critical projectile shots, deal ")
			.addPercent(a -> a.mDamage, DAMAGE[rarity - 1], false, true)
			.add(" more damage and apply ")
			.addPercent(a -> a.mBleedAmount, BLEED_AMOUNT)
			.add(" Bleed for ")
			.addDuration(a -> a.mBleedDuration, BLEED_DURATION)
			.add(" seconds.");
	}

}
