package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class ArcaneThrust implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Arcane Thrust";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(24, 216, 224), 1.0f);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (PlayerUtils.isNonFallingAttack(player, event.getEntity())) {
			double damage = 1 + (event.getDamage() * ((double)level / (level + 1)));

			Location loc = player.getEyeLocation();
			BoundingBox box = BoundingBox.of(loc, 0.6, 0.6, 0.6);
			Vector dir = loc.getDirection();
			box.shift(dir);
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), 10, player);
			World world = player.getWorld();
			world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 0, 0, 0, 2);

			for (int i = 0; i < 3; i++) {
				box.shift(dir);
				Location bLoc = box.getCenter().toLocation(world);

				world.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 1, 0, 0, 0);
				world.spawnParticle(Particle.REDSTONE, bLoc, 12, 0.4, 0.4, 0.4, COLOR);

				Iterator<LivingEntity> iter = mobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (box.overlaps(mob.getBoundingBox())) {
						if (mob != target) {
							EntityUtils.damageEntity(plugin, mob, damage, player, MagicType.ENCHANTMENT);
							MovementUtils.knockAway(player.getLocation(), mob, 0.25f, 0.25f);
						}
						iter.remove();
						mobs.remove(mob);
					}
				}
			}
			world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		}
	}
}
