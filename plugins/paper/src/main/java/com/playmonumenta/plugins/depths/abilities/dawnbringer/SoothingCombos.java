package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SoothingCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Soothing Combos";
	public static final double[] SPEED_PERCENT = {0.1, 0.125, 0.15, 0.175, 0.2, 0.25};
	public static final String SPEED_EFFECT_NAME = "SoothingCombosPercentSpeedEffect";
	public static final int[] DURATION = {40, 50, 60, 70, 80, 120};
	public static final int RANGE = 12;
	public static final int HIT_REQ = 3;

	public static final DepthsAbilityInfo<SoothingCombos> INFO =
		new DepthsAbilityInfo<>(SoothingCombos.class, ABILITY_NAME, SoothingCombos::new, DepthsTree.DAWNBRINGER, DepthsTrigger.COMBO)
			.displayItem(Material.HONEYCOMB)
			.descriptions(SoothingCombos::getDescription)
			.singleCharm(false);

	private final int mHitRequirement;
	private final int mDuration;
	private final double mSpeed;
	private final int mHaste;
	private final double mRange;

	private int mComboCount = 0;

	public SoothingCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHitRequirement = HIT_REQ + (int) CharmManager.getLevel(mPlayer, CharmEffects.SOOTHING_COMBOS_HIT_REQUIREMENT.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.SOOTHING_COMBOS_DURATION.mEffectName, DURATION[mRarity - 1]);
		mSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SOOTHING_COMBOS_SPEED_AMPLIFIER.mEffectName, SPEED_PERCENT[mRarity - 1]);
		mHaste = (int) CharmManager.getLevel(mPlayer, CharmEffects.SOOTHING_COMBOS_HASTE_LEVEL.mEffectName) + mRarity == 6 ? 1 : 0;
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SOOTHING_COMBOS_RANGE.mEffectName, RANGE);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;
			if (mComboCount >= mHitRequirement) {
				mComboCount = 0;
				PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, mDuration, mHaste, false, true);

				List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), mRange, true);

				for (Player p : players) {
					p.addPotionEffect(hasteEffect);
					mPlugin.mEffectManager.addEffect(p, SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeed, SPEED_EFFECT_NAME));
					new PartialParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
					mPlayer.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
				}

				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
				new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
			}
			return true;
		}
		return false;
	}

	private static Description<SoothingCombos> getDescription(int rarity, TextColor color) {
		TextComponent haste = rarity == 6 ? Component.text("II", color) : Component.text("I");
		return new DescriptionBuilder<SoothingCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQ, true)
			.add(" melee strikes, apply ")
			.addPercent(a -> a.mSpeed, SPEED_PERCENT[rarity - 1], false, true)
			.add(" speed and Haste ")
			.add(haste)
			.add(" for ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds to players within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks, including the user.");
	}


}

