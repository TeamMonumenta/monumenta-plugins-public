package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.GearDamageIncrease;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.HemorrhageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class Bloodlust implements Infusion {
	private static final int DISTANCE_SQUARED = 8 * 8; // 8 blocks
	private static final String BLOODLUST_EFFECT = "BloodlustDamage";
	private static final int MAX_STACKS = 10;
	private static final double DAMAGE_BONUS = 0.005;
	private static final int DURATION = Constants.TICKS_PER_SECOND * 5;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeProjectileAndMagicTypes();

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.BLOODLUST;
	}

	@Override
	public String getName() {
		return "Redacted Delve Infusion";
	}

	@Override
	public void onHemorrhage(Plugin plugin, Player player, double value, HemorrhageEvent event) {
		if (event.getMob().getLocation().distanceSquared(player.getLocation()) <= DISTANCE_SQUARED) {
			applyBloodlust(plugin, player, value);
		}
	}

	private void applyBloodlust(Plugin plugin, Player player, double level) {
		NavigableSet<Effect> effectNavigableSet = plugin.mEffectManager.getEffects(player, BLOODLUST_EFFECT);
		double currentBloodlust = 0;
		if (effectNavigableSet != null) {
			currentBloodlust = effectNavigableSet.last().getMagnitude();
			// Reset the duration of the previous stacks so that
			// they don't decay in the background, and instead
			// reappear once the greater magnitude stack effect runs out.
			int durationMultiplier = Math.min(MAX_STACKS, effectNavigableSet.size() + 1);
			for (Effect effect : effectNavigableSet) {
				effect.setDuration(durationMultiplier * DURATION);
				durationMultiplier--;
			}
		}

		double damage = Math.min(currentBloodlust + (DAMAGE_BONUS * level), MAX_STACKS * DAMAGE_BONUS * level);
		plugin.mEffectManager.addEffect(player, BLOODLUST_EFFECT, new GearDamageIncrease(DURATION, damage)
				.damageTypes(AFFECTED_DAMAGE_TYPES));

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.5f, 0.7f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 0.75F, 0.5F);
	}
}
