package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Poultrification implements Enchantment {

	private static final double CHANCE_PER_LEVEL = 0.01;


	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.POULTRIFICATION;
	}

	@Override
	public String getName() {
		return "Poultrification";
	}

	@Override
	public void onDamage(final Plugin plugin, final Player player, final double level, final DamageEvent event,
						 final LivingEntity target) {
		if (!EntityUtils.isBoss(target) && FastUtils.RANDOM.nextDouble() < (CHANCE_PER_LEVEL * level)) {
			final Location loc = target.getLocation();
			final Component name = Component.text(target.getName());
			final double health = target.getHealth();
			final double max_health = EntityUtils.getMaxHealth(target);

			Chicken chicken = loc.getWorld().spawn(loc, Chicken.class);
			chicken.customName(name);
			EntityUtils.setMaxHealthAndHealth(chicken, max_health);
			chicken.setHealth(health);
			chicken.setVelocity(new Vector(
				FastUtils.RANDOM.nextDouble(-0.5, 0.5),
				FastUtils.RANDOM.nextDouble(0.3, 0.5),
				FastUtils.RANDOM.nextDouble(-0.5, 0.5))
			);
			target.setHealth(0);

			loc.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.HOSTILE, 1f, 1.8f);
			loc.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_DEATH, SoundCategory.HOSTILE, 1f, 1.65f);
			loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1f, 1.88f);

			new PartialParticle(Particle.SPELL, chicken.getEyeLocation(), 35)
				.delta(0.2, 0.2, 0.2)
				.extraRange(0.4, 1.4).spawnAsPlayerActive(player);
		}
	}
}
