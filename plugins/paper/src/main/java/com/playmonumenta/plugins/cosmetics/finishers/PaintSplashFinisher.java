package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class PaintSplashFinisher implements EliteFinisher {
	public static final String NAME = "Paint Splash";

	private static final Particle.DustOptions[][] PARTICLE_SETS = {
		{
			//set 1
			new Particle.DustOptions(Color.fromRGB(193, 66, 68), 5.0f),
			new Particle.DustOptions(Color.fromRGB(71, 159, 177), 5.0f),
			new Particle.DustOptions(Color.fromRGB(170, 199, 75), 5.0f)
		},
		{
			//set 2
			new Particle.DustOptions(Color.fromRGB(84, 170, 212), 5.0f),
			new Particle.DustOptions(Color.fromRGB(230, 168, 58), 5.0f),
			new Particle.DustOptions(Color.fromRGB(212, 86, 38), 5.0f)
		},
		{
			//set 3
			new Particle.DustOptions(Color.fromRGB(189, 87, 146), 5.0f),
			new Particle.DustOptions(Color.fromRGB(247, 208, 76), 5.0f),
			new Particle.DustOptions(Color.fromRGB(111, 124, 188), 5.0f)
		}
	};

	private static final ItemStack[][] CONCRETE_BASE_ITEM = {
		{
			new ItemStack(Material.RED_CONCRETE),
			new ItemStack(Material.CYAN_CONCRETE),
			new ItemStack(Material.GREEN_CONCRETE)
		},
		{
			new ItemStack(Material.BLUE_CONCRETE),
			new ItemStack(Material.YELLOW_CONCRETE),
			new ItemStack(Material.ORANGE_CONCRETE)
		},
		{
			new ItemStack(Material.PINK_CONCRETE),
			new ItemStack(Material.YELLOW_CONCRETE),
			new ItemStack(Material.LIGHT_BLUE_CONCRETE)
		}
	};


	@Override
	public void run(Player mPlayer, Entity killedMob, Location loc) {
		int mDegreeRand = FastUtils.randomIntInRange(0, 120);
		int mRand = FastUtils.randomIntInRange(0, 2);

		Item[] mConcretes = new Item[3];
		for (int i = 0; i < 3; i++) {
			mConcretes[i] = throwConcrete(120 * i + mDegreeRand, loc, CONCRETE_BASE_ITEM[mRand][i]);
		}
		new BukkitRunnable() {
			int mTicks = 0;


			@Override
			public void run() {
				if (mTicks == 0) {
					loc.getWorld().playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 1f, 1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_JUMP, SoundCategory.PLAYERS, 1f, 1f);
				}
				if (mRand == 0) {
					new PartialParticle(Particle.REDSTONE, mConcretes[0].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[0][0]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[1].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[0][1]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[2].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[0][2]).spawnAsPlayerActive(mPlayer);
				} else if (mRand == 1) {
					new PartialParticle(Particle.REDSTONE, mConcretes[0].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[1][0]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[1].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[1][1]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[2].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[1][2]).spawnAsPlayerActive(mPlayer);
				} else {
					new PartialParticle(Particle.REDSTONE, mConcretes[0].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[2][0]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[1].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[2][1]).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mConcretes[2].getLocation(), 15, 0.5, 0.5, 0.5, PARTICLE_SETS[2][2]).spawnAsPlayerActive(mPlayer);
				}
				if (mTicks > 100 || (mTicks > 50 && mConcretes[0].isOnGround() && mConcretes[1].isOnGround() && mConcretes[2].isOnGround())) {
					Collection<Item> potentialConcrete = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 20.0);
					for (Item i : potentialConcrete) {
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


	@Override
	public Material getDisplayItem() {
		return Material.ORANGE_CONCRETE;
	}


	public static Item throwConcrete(int degrees, Location loc, ItemStack concrete) {
		ItemMeta meta = concrete.getItemMeta();
		meta.displayName(Component.text("Concrete " + degrees, NamedTextColor.WHITE)
			                 .decoration(TextDecoration.ITALIC, false));
		concrete.setItemMeta(meta);
		Item concreteItem = loc.getWorld().dropItem(loc, concrete);
		concreteItem.setPickupDelay(Integer.MAX_VALUE);
		concreteItem.addScoreboardTag(NAME);
		concreteItem.setVelocity(new Vector(0.2 * FastUtils.sinDeg(degrees), 0.6, 0.2 * FastUtils.cosDeg(degrees)));

		return concreteItem;
	}


}
