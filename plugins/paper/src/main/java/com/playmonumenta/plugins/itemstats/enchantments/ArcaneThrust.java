package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ArcaneThrust implements Enchantment {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(24, 216, 224), 1.0f);
	public static final String CHARM_DAMAGE = "Arcane Thrust Damage";
	public static final String CHARM_KNOCKBACK = "Arcane Thrust Knockback";

	@Override
	public String getName() {
		return "Arcane Thrust";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ARCANE_THRUST;
	}

	@Override
	public double getPriorityAmount() {
		return 30;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			if (player.getCooledAttackStrength(0.5f) > 0.9) {
				double damageMult = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, (value / (value + 1)));
				double damage = 1 + (event.getDamage() * damageMult);

				Location loc = player.getEyeLocation();
				BoundingBox box = BoundingBox.of(loc, 0.6, 0.6, 0.6);
				Vector dir = loc.getDirection();
				box.shift(dir);
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), 10, player);
				World world = player.getWorld();
				new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 0, 0, 0, 2).spawnAsPlayerActive(player);

				for (int i = 0; i < 3; i++) {
					box.shift(dir);
					Location bLoc = box.getCenter().toLocation(world);

					new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 1, 0, 0, 0).spawnAsPlayerActive(player);
					new PartialParticle(Particle.REDSTONE, bLoc, 12, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerActive(player);

					Iterator<LivingEntity> iter = mobs.iterator();
					while (iter.hasNext()) {
						LivingEntity mob = iter.next();
						if (box.overlaps(mob.getBoundingBox())) {
							if (mob != enemy) {
								DamageUtils.damage(player, mob, DamageType.MELEE_ENCH, damage);
								float knockback = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_KNOCKBACK, 0.25);
								MovementUtils.knockAway(player.getLocation(), mob, knockback, knockback);
							}
							iter.remove();
						}
					}
				}
				world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
			}
		}
	}
}
