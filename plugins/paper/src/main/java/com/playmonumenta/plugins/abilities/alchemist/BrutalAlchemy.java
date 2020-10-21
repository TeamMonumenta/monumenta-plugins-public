package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BrutalAlchemy extends PotionAbility {
	private static final int BRUTAL_ALCHEMY_1_DAMAGE = 1;
	private static final int BRUTAL_ALCHEMY_2_DAMAGE = 2;
	private static final int BRUTAL_ALCHEMY_DURATION = 20 * 8;
	private static final int BRUTAL_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 2;
	private static final int BRUTAL_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 4;

	private final int mVulnerabilityAmplifier;

	public BrutalAlchemy(Plugin plugin, Player player) {
		super(plugin, player, "Brutal Alchemy", BRUTAL_ALCHEMY_1_DAMAGE, BRUTAL_ALCHEMY_2_DAMAGE);
		mInfo.mLinkedSpell = Spells.BRUTAL_ALCHEMY;
		mInfo.mScoreboardId = "BrutalAlchemy";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Your Alchemist's Potions deal 1 damage and 15% Vulnerability for 8 seconds.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now deal 2 damage and 25% Vulnerability.");

		mVulnerabilityAmplifier = getAbilityScore() == 1 ? BRUTAL_ALCHEMY_1_VULNERABILITY_AMPLIFIER : BRUTAL_ALCHEMY_2_VULNERABILITY_AMPLIFIER;
	}

	@Override
	public void apply(LivingEntity mob) {
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, BRUTAL_ALCHEMY_DURATION, mVulnerabilityAmplifier, false, true));
	}

}
