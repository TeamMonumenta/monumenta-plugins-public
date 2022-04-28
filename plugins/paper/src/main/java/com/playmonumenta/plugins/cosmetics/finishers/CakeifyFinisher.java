package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CakeifyFinisher {

	public static final String NAME = "Cakeify";

	public static void run(Player p, Entity killedMob, Location loc) {

		throwCakes(0, loc);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks <= 30) {
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EAT, 1.0f, 0.6f + (.03f * mTicks));
				}
				if (mTicks == 10) {
					throwCakes(30, loc);
				} else if (mTicks == 20) {
					throwCakes(60, loc);
				} else if (mTicks == 100) {
					Collection<Item> potentialCakes = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 20.0);
					for (Item i : potentialCakes) {
						if (i.getScoreboardTags().contains(NAME)) {
							i.remove();
						}
					}
					this.cancel();
				}
				mTicks += 5;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 5);

	}

	public static void throwCakes(int offset, Location loc) {
		ItemStack cake = new ItemStack(Material.CAKE);
		for (int i = 0; i < 4; i++) {
			ItemMeta cakeMeta = cake.getItemMeta();
			cakeMeta.displayName(Component.text("Birthday Cake " + offset + i + "!", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
			cake.setItemMeta(cakeMeta);
			Item cakeItem = loc.getWorld().dropItem(loc, cake);
			cakeItem.setPickupDelay(Integer.MAX_VALUE);
			cakeItem.addScoreboardTag(NAME);
			int degrees = offset + (i * 90);
			cakeItem.setVelocity(new Vector(0.2 * FastUtils.sinDeg(degrees), 0.6, 0.2 * FastUtils.cosDeg(degrees)));
		}
	}
}
