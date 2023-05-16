package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AdhesiveTotems extends Ability {

	public static final AbilityInfo<AdhesiveTotems> INFO =
		new AbilityInfo<>(AdhesiveTotems.class, "Adhesive Totems", AdhesiveTotems::new)
			.linkedSpell(ClassAbility.ADHESIVE_TOTEMS)
			.scoreboardId("AdhesiveTotems")
			.shorthandName("AT")
			.descriptions(
				"Hitting a mob with a totem spawning projectile attaches the totem to that mob until the duration expires or the mob dies.",
				String.format("""
					On hit, this now does special effects for each type of totem.
					Flame Totem: Deal an extra pulse of damage if the stuck mob dies.
					Cleansing Totem: Apply %s%% weaken for %s seconds to the stuck mob.
					Lightning Totem: Attacks mobs excluding the stuck mob unless it is the only one.
					Whirlwind Totem: Silence the stuck mob for %s seconds.
					Decayed Totem: Apply Decay %s to the stuck mob for %s seconds.""",
					StringUtils.multiplierToPercentage(CleansingTotem.WEAKNESS_PERCENT),
					StringUtils.ticksToSeconds(CleansingTotem.WEAKNESS_DURATION),
					StringUtils.ticksToSeconds(WhirlwindTotem.SILENCE_DURATION),
					DecayedTotem.DECAY_LEVEL,
					StringUtils.ticksToSeconds(DecayedTotem.DECAY_DURATION)
					))
			.simpleDescription("Makes totems stick to mobs that are hit by the summoning projectile and apply an effect to them at level 2.")
			.displayItem(Material.SLIME_BALL);

	public AdhesiveTotems(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
	}
}
