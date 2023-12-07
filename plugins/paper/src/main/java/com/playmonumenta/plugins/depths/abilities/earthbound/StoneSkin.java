package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class StoneSkin extends DepthsAbility {

	public static final String ABILITY_NAME = "Stone Skin";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "StoneSkinPercentDamageReceivedEffect";
	private static final double[] PERCENT_DAMAGE_RECEIVED = {0.15, 0.18, 0.22, 0.26, 0.30, 0.38};
	private static final String KNOCKBACK_RESISTANCE_EFFECT_NAME = "StoneSkinKnockbackResistanceEffect";
	private static final double[] KNOCKBACK_RESISTANCE = {0.4, 0.5, 0.6, 0.7, 0.8, 1.0};
	private static final int DURATION = 20 * 5;
	private static final int COOLDOWN = 20 * 12;

	public static final String CHARM_COOLDOWN = "Stone Skin Cooldown";

	public static final DepthsAbilityInfo<StoneSkin> INFO =
		new DepthsAbilityInfo<>(StoneSkin.class, ABILITY_NAME, StoneSkin::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.STONE_SKIN)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", StoneSkin::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.POLISHED_ANDESITE)
			.descriptions(StoneSkin::getDescription);

	private final double mDamageReduction;
	private final double mKnockbackResistance;
	private final int mDuration;

	public StoneSkin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageReduction = PERCENT_DAMAGE_RECEIVED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.STONE_SKIN_RESISTANCE_AMPLIFIER.mEffectName);
		mKnockbackResistance = KNOCKBACK_RESISTANCE[mRarity - 1] + CharmManager.getLevel(mPlayer, CharmEffects.STONE_SKIN_KNOCKBACK_RESISTANCE.mEffectName) / 10;
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.STONE_SKIN_DURATION.mEffectName, DURATION);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(mDuration, -mDamageReduction));
		mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESISTANCE_EFFECT_NAME, new PercentKnockbackResist(mDuration, mKnockbackResistance, KNOCKBACK_RESISTANCE_EFFECT_NAME));

		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.2, 0, 0.2, 0.25).spawnAsPlayerBuff(mPlayer);
		new PartialParticle(Particle.BLOCK_DUST, loc, 20, 0.2, 0, 0.2, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsPlayerBuff(mPlayer);
		loc = loc.add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.25f, 1.35f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.25f, 1.1f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 35, 0.4, 0.4, 0.4, 0.25).spawnAsPlayerBuff(mPlayer);
	}


	private static Description<StoneSkin> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<StoneSkin>(color)
			.add("Right click while sneaking to gain ")
			.addPercent(a -> a.mDamageReduction, PERCENT_DAMAGE_RECEIVED[rarity - 1], false, true)
			.add(" resistance and ")
			.add(a -> a.mKnockbackResistance * 10, KNOCKBACK_RESISTANCE[rarity - 1] * 10, false, null, true)
			.add(" knockback resistance for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}
