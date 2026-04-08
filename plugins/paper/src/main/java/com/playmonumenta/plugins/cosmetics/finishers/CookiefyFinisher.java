package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CookiefyFinisher implements EliteFinisher {

	public static final String NAME = "Cookiefy";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		boolean funny = FastUtils.randomFloatInRange(0.0f, 1.0f) < 0.02;
		throwCookies(0, funny, loc);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks <= 30) {
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 0.6f + (.03f * mTicks));
				}
				if (mTicks == 10) {
					throwCookies(30, funny, loc);
				} else if (mTicks == 20) {
					throwCookies(60, funny, loc);
				} else if (mTicks == 100) {
					Collection<Item> potentialCookies = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 20.0);
					for (Item i : potentialCookies) {
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

	public static void throwCookies(int offset, boolean funny, Location loc) {
		ItemStack cookie = null;
		for (int i = 0; i < 4; i++) {
			if (!funny) {
				cookie = DisplayEntityUtils.generateRPItem(Material.COOKIE, "Christmas Cookie!");
			}
			if (funny) {
				int dice = FastUtils.randomIntInRange(1, 2);
				switch (dice) {
					case 1 -> cookie = DisplayEntityUtils.generateRPItem(Material.COOKIE, "Chewed Cookie");
					case 2 -> cookie = DisplayEntityUtils.generateRPItem(Material.COOKIE, "Congrats");
				}
			}
			Item cookieItem = loc.getWorld().dropItem(loc, cookie);
			cookieItem.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			cookieItem.setPickupDelay(Integer.MAX_VALUE);
			cookieItem.addScoreboardTag(NAME);
			int degrees = offset + (i * 90);
			cookieItem.setVelocity(new Vector(0.2 * FastUtils.sinDeg(degrees), 0.6, 0.2 * FastUtils.cosDeg(degrees)));
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.COOKIE;
	}

}
