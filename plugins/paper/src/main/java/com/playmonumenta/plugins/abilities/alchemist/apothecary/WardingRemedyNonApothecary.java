package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;

public class WardingRemedyNonApothecary extends Ability {

	private static final int WARDING_REMEDY_RANGE = 12;
	private static final double WARDING_REMEDY_1_DAMAGE_MULTIPLIER = 1.05;
	private static final double WARDING_REMEDY_2_DAMAGE_MULTIPLIER = 1.1;
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);

	public WardingRemedyNonApothecary(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		int level = getWardingRemedyLevel();
		if (level > 0 && AbsorptionUtils.getAbsorption(mPlayer) > 0) {
			double multiplier = level == 1 ? WARDING_REMEDY_1_DAMAGE_MULTIPLIER : WARDING_REMEDY_2_DAMAGE_MULTIPLIER;
			Location loc = event.getDamagee().getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SPELL_WITCH, loc, 8, 0.3, 0.5, 0.3);
			world.spawnParticle(Particle.REDSTONE, loc, 10, 0.4, 0.5, 0.4, APOTHECARY_DARK_COLOR);
			event.setDamage(event.getDamage() * multiplier);
		}
	}

	private int getWardingRemedyLevel() {
		int level = 0;

		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), WARDING_REMEDY_RANGE, true)) {
			Ability wr = AbilityManager.getManager().getPlayerAbility(player, WardingRemedy.class);
			if (wr != null) {
				int score = wr.getAbilityScore();
				if (score == 2) {
					return score;
				} else if (score == 1) {
					level = score;
				}
			}
		}

		return level;
	}

}
