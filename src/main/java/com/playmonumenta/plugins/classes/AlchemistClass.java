package com.playmonumenta.plugins.classes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
    BasiliskPoison - refactored
    Unstable Arrows - refactored
    PowerInjection - refactored
    IronTincture - refactored
    Gruesome Alchemy
    Brutal Alchemy
    Enfeebling Elixir - refactored
*/

public class AlchemistClass extends BaseClass {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final int GRUESOME_ALCHEMY_VULN = 4; //25%
	private static final int GRUESOME_ALCHEMY_SLOW = 2;

	private static final int BRUTAL_ALCHEMY_DAMAGE_1 = 3;
	private static final int BRUTAL_ALCHEMY_DAMAGE_2 = 5;
	private static final int BRUTAL_ALCHEMY_WITHER_1_DURATION = 4 * 20;
	private static final int BRUTAL_ALCHEMY_WITHER_2_DURATION = 6 * 20;

	private World mWorld;

	public AlchemistClass(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	private static ItemStack getAlchemistPotion() {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, 1);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		meta.setColor(Color.WHITE);
		meta.setDisplayName(ChatColor.AQUA + "Alchemist's Potion");
		List<String> lore = Arrays.asList(new String[] {
			ChatColor.GRAY + "A unique potion for Alchemists",
		});
		meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	public static void addAlchemistPotions(Player player, int numAddedPotions) {
		if (numAddedPotions == 0) {
			return;
		}

		Inventory inv = player.getInventory();
		ItemStack firstFoundPotStack = null;
		int potCount = 0;

		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				if (firstFoundPotStack == null) {
					firstFoundPotStack = item;
				}
				potCount += item.getAmount();
			}
		}

		if (potCount < 32) {
			if (firstFoundPotStack != null) {
				firstFoundPotStack.setAmount(firstFoundPotStack.getAmount() + numAddedPotions);
			} else {
				ItemStack newPotions = getAlchemistPotion();
				newPotions.setAmount(numAddedPotions);
				inv.addItem(newPotions);
			}
		}
	}

	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		int brutalAlchemy = ScoreboardUtils.getScoreboardValue(player, "BrutalAlchemy");
		int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");

		if (brutalAlchemy > 0 || gruesomeAlchemy > 0) {
			int newPot = 1;
			if (mRandom.nextDouble() < 0.30) {
				newPot++;
			}

			addAlchemistPotions(player, newPot);
		}
	}

	@Override
	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		ItemStack item = potion.getItem();
		if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.SPELL);
			potion.setMetadata("AlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}
	}

	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
	                                       ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int brutalAlchemy = ScoreboardUtils.getScoreboardValue(player, "BrutalAlchemy");
				if (brutalAlchemy > 0) {
					for (LivingEntity entity : affectedEntities) {
						if (EntityUtils.isHostileMob(entity)) {
							int damage = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_DAMAGE_1 : BRUTAL_ALCHEMY_DAMAGE_2;
							int duration = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_WITHER_1_DURATION : BRUTAL_ALCHEMY_WITHER_2_DURATION;
							EntityUtils.damageEntity(mPlugin, entity, damage, player);
							entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration + 10, 1, false, true));
						}
					}
				}

				int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");
				if (gruesomeAlchemy > 0) {
					for (LivingEntity entity : affectedEntities) {
						if (EntityUtils.isHostileMob(entity)) {
							entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRUESOME_ALCHEMY_DURATION, GRUESOME_ALCHEMY_SLOW, false, true));
							if (gruesomeAlchemy > 1) {
								entity.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, GRUESOME_ALCHEMY_DURATION, GRUESOME_ALCHEMY_VULN, false, true));
							}
						}
					}
				}
			}
		}
		return true;
	}
}
