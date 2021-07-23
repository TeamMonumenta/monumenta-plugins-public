package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class Reflection implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Reflection";
	private static final double REFLECT_PCT_PER_LEVEL = 0.06;
	private static final int RADIUS = 4;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, int level, EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.CUSTOM) {
			double damage = event.getDamage();
			World world = player.getWorld();
			world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.6f);
			world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.4f);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 3, 0.1, 0.1, 0.1, 0.15);
					if (mTicks >= 20) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), RADIUS, player)) {
							EntityUtils.damageEntity(plugin, mob, DelveInfusionUtils.getModifiedLevel(plugin, player, level) * REFLECT_PCT_PER_LEVEL * damage, player, MagicType.HOLY, true);
						}
						world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
						new BukkitRunnable() {
							double mRadius = 0;
							final Location mLoc = player.getLocation();
							@Override
							public void run() {
								mRadius += 0.5;
								for (double j = 0; j < 360; j += 18) {
									double radian1 = Math.toRadians(j);
									mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
									world.spawnParticle(Particle.SOUL_FIRE_FLAME, mLoc, 2, 0, 0, 0, 0.125);
									world.spawnParticle(Particle.END_ROD, mLoc, 2, 0, 0, 0, 0.15);
									mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
								}

								if (mRadius >= RADIUS + 1) {
									this.cancel();
								}
							}

						}.runTaskTimer(plugin, 0, 1);
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}
	}
}
