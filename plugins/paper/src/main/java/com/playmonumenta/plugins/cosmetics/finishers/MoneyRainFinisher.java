package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
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

public class MoneyRainFinisher implements EliteFinisher {

	public static final String NAME = "Money Rain";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		loc.setY(loc.getY() + killedMob.getHeight() / 2);
		boolean funny = Math.random() < 0.02;

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				int amount = 1;
				if (mTicks <= 30) {
					loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.6f + (.03f * mTicks));
					throwMoney(loc, funny, amount);
				} else if (mTicks == 33) {
					amount = 25;
					throwMoney(loc, funny, amount);
					EntityUtils.fireworkAnimation(loc, List.of(Color.YELLOW), FireworkEffect.Type.BURST, 1);
				} else if (mTicks == 150) {
					Collection<Item> potentialMoney = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 20.0);
					for (Item i : potentialMoney) {
						if (i.getScoreboardTags().contains(NAME)) {
							i.remove();
						}
					}
					this.cancel();
				}
				mTicks += 3;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 3);

	}

	public static void throwMoney(Location loc, boolean funny, int amount) {
		ItemStack money = null;
		if (funny) {
			money = DisplayEntityUtils.generateRPItem(Material.KELP, "Tlaxan Currency?");
		}
		for (int i = 0; i < amount; i++) {
			if (!funny) {
				int dice = FastUtils.randomIntInRange(1, 3);
				switch (dice) {
					case 1 -> {
						money = DisplayEntityUtils.generateRPItem(Material.SUNFLOWER, "Hyperexperience");
					}
					case 2 -> {
						money = DisplayEntityUtils.generateRPItem(Material.NETHER_STAR, "Hyper Crystalline Shard");
					}
					case 3 -> {
						money = DisplayEntityUtils.generateRPItem(Material.FIREWORK_STAR, "Hyperchromatic Archos Ring");
					}
					default -> {
					}
				}
			}
			Item moneyItem = loc.getWorld().dropItem(loc, money);
			moneyItem.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			moneyItem.setPickupDelay(Integer.MAX_VALUE);
			moneyItem.addScoreboardTag(NAME);
			int degrees = FastUtils.randomIntInRange(0, 360);
			moneyItem.setVelocity(new Vector(0.2 * FastUtils.sinDeg(degrees), FastUtils.randomFloatInRange(0.3f, 0.9f), 0.2 * FastUtils.cosDeg(degrees)));
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.EMERALD;
	}

}
