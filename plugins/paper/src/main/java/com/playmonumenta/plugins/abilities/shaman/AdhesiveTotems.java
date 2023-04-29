package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.itemstats.enchantments.Decay;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AdhesiveTotems extends Ability {
	private static final double WEAKNESS_PERCENT = 0.4;
	private static final int WEAKNESS_DURATION = 8 * 20;
	private static final int SILENCE_DURATION = 8 * 20;
	private static final int DECAY_LEVEL = 5;
	private static final int DECAY_DURATION = 5 * 20;


	public static final AbilityInfo<AdhesiveTotems> INFO =
		new AbilityInfo<>(AdhesiveTotems.class, "Adhesive Totems", AdhesiveTotems::new)
			.linkedSpell(ClassAbility.ADHESIVE_TOTEMS)
			.scoreboardId("AdhesiveTotems")
			.shorthandName("AT")
			.descriptions(
				"Hitting a mob with a totem spawning projectile attaches the totem to that mob, making the totem follow the mob until the duration expires or the mob dies.",
				"On hit, this now does special effects for each type of totem.\nFlame Totem: Deal an extra pulse of damage if the stuck mob dies." +
				"\nCleansing Totem: Apply a 40% slow for 8 seconds to the stuck mob.\nLightning Totem: Attacks mobs excluding the stuck mob unless it is the only one." +
				"\nWhirlwind Totem: Apply a 8 second silence to the stuck mob.\nDecayed Totem: Apply Decay 5 to the stuck mob for 5 seconds.")
			.simpleDescription("Makes totems stick to mobs that are hit by the summoning projectile and apply an effect to them at level 2.")
			.displayItem(Material.SLIME_BALL);

	public AdhesiveTotems(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
	}

	public static void onTotemHitMob(Plugin plugin, Player player, LivingEntity hitMob, ClassAbility ability) {
		switch (ability) {
			case CLEANSING_TOTEM -> EntityUtils.applySlow(plugin, WEAKNESS_DURATION, WEAKNESS_PERCENT, hitMob);
			case WHIRLWIND_TOTEM -> EntityUtils.applySilence(plugin, SILENCE_DURATION, hitMob);
			case DECAYED_TOTEM -> Decay.apply(plugin, hitMob, DECAY_DURATION, DECAY_LEVEL, player);
			default -> {
				//fall out
			}
		}
	}
}
