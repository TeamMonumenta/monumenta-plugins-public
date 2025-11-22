package com.playmonumenta.plugins.cosmetics.punches;

import com.playmonumenta.networkchat.RemotePlayerListener;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RobberyPunch implements PlayerPunch {
	// This punch uses code from the Money Rain elite finisher

	public static final String NAME = "Robbery";

	@Override
	public void run(Player bully, Player victim) {
		Location loc = victim.getLocation();
		World world = victim.getWorld();
		boolean funny = Math.random() < 0.02;

		loc.setY(loc.getY() + victim.getHeight() / 2);

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

		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1).spawnAsPlayerActive(bully);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(bully);

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);

		victim.setVelocity(new Vector(0, 2.5, 0));
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
					case 1 -> money = DisplayEntityUtils.generateRPItem(Material.SUNFLOWER, "Hyperexperience");
					case 2 ->
						money = DisplayEntityUtils.generateRPItem(Material.NETHER_STAR, "Hyper Crystalline Shard");
					case 3 ->
						money = DisplayEntityUtils.generateRPItem(Material.FIREWORK_STAR, "Hyperchromatic Archos Ring");
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
	public void broadcastPunchMessage(Player bully, Player victim, List<Player> playersInWorld, boolean isRemotePunch) {
		for (Player player : playersInWorld) {
			player.sendMessage(
				RemotePlayerListener.getPlayerComponent(bully.getUniqueId())
					.append(Component.text((isRemotePunch ? " remotely robbed " : " robbed "), NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
					.append(RemotePlayerListener.getPlayerComponent(victim.getUniqueId()))
					.append(Component.text(" and punched them into the sky!", NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
			);
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.EMERALD;
	}
}
