package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CrackedIdol extends DepthsAbility {
	public static final String ABILITY_NAME = "Cracked Idol";

	private static final String IDOL_DAMAGE_EFFECT_NAME = "CrackedIdolDamageEffect";
	private static final String IDOL_RESISTANCE_EFFECT_NAME = "CrackedIdolResistanceEffect";
	private static final String IDOL_SPEED_EFFECT_NAME = "CrackedIdolSpeedEffect";

	public static final DepthsAbilityInfo<CrackedIdol> INFO =
		new DepthsAbilityInfo<>(CrackedIdol.class, ABILITY_NAME, CrackedIdol::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.EMERALD)
			.descriptions(CrackedIdol::getDescription);

	public CrackedIdol(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CrackedIdol> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Gain +10% damage, resistance, speed, and ability rarity odds. The next time you die, permanently lose these effects.");
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			EffectManager effectManager = Plugin.getInstance().mEffectManager;
			effectManager.addEffect(mPlayer, IDOL_DAMAGE_EFFECT_NAME, new PercentDamageDealt(80, 0.1).displaysTime(false));
			effectManager.addEffect(mPlayer, IDOL_RESISTANCE_EFFECT_NAME, new PercentDamageReceived(80, -0.1).displaysTime(false));
			effectManager.addEffect(mPlayer, IDOL_SPEED_EFFECT_NAME, new PercentSpeed(80, 0.1, IDOL_SPEED_EFFECT_NAME).displaysTime(false));
		}
	}
}
