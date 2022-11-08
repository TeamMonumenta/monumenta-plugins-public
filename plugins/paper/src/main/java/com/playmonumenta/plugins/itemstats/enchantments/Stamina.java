package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Stamina implements Enchantment {

	private static final String STAMINA_EFFECT = "StaminaDamage";
	private static final double DAMAGE_BONUS = 0.025;
	private static final double DAMAGE_CAP = 0.1;
	private static final int DURATION = 6 * 20;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(241, 190, 84), 0.75f);
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH,
		DamageEvent.DamageType.MELEE_SKILL,
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.PROJECTILE_SKILL
	);

	@Override
	public String getName() {
		return "Stamina";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STAMINA;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null) {
			applyStamina(plugin, player, value);
		}
	}

	private void applyStamina(Plugin plugin, Player player, double level) {
		NavigableSet<Effect> s = plugin.mEffectManager.getEffects(player, STAMINA_EFFECT);
		if (s != null && s.last().getDuration() > DURATION - 20) {
			// attacked within 1s, do not run anything/refresh effect
			return;
		}
		double currStamina = 0;
		if (s != null) {
			currStamina = s.last().getMagnitude();
		}
		double damage = Math.min(currStamina + (DAMAGE_BONUS * level), DAMAGE_CAP * level);
		plugin.mEffectManager.addEffect(player, STAMINA_EFFECT, new PercentDamageDealt(DURATION, damage, AFFECTED_DAMAGE_TYPES));

		player.getWorld().playSound(
			player.getLocation(),
			Sound.BLOCK_LANTERN_BREAK,
			SoundCategory.PLAYERS,
			0.5f,
			0.7f
		);

		double widthDelta = PartialParticle.getWidthDelta(player);
		double doubleWidthDelta = widthDelta * 2;
		double heightDelta = PartialParticle.getHeightDelta(player);

		new PartialParticle(
			Particle.REDSTONE,
			LocationUtils.getHeightLocation(player, 0.8),
			8,
			doubleWidthDelta,
			heightDelta / 2,
			doubleWidthDelta,
			1,
			COLOR
		).spawnAsEnemy();
	}

}
