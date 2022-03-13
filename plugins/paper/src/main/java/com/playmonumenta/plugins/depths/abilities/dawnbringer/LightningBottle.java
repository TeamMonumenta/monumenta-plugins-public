package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class LightningBottle extends DepthsAbility {
	public static final String ABILITY_NAME = "Lightning Bottle";
	public static final String POTION_NAME = ABILITY_NAME;
	public static final String POTION_META_DATA = "LightningBottle";
	public static final double[] DAMAGE = {6, 7.5, 9, 10.5, 12, 15};
	public static final double[] VULNERABILITY = {0.1, 0.125, 0.15, 0.175, 0.2, 0.25};
	public static final double SLOWNESS = 0.2;
	public static final int MAX_STACK = 12;
	public static final int KILLS_PER = 2;
	public static final int DURATION = 3 * 20;
	public static final int DEATH_RADIUS = 32;

	private WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	private int mCount = 0;

	public LightningBottle(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.BREWING_STAND;
		mInfo.mLinkedSpell = ClassAbility.LIGHTNING_BOTTLE;
		mTree = DepthsTree.SUNLIGHT;
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && InventoryUtils.testForItemWithName(potion.getItem(), POTION_NAME)) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.SPELL);
			mPlayerItemStatsMap.put(potion, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		}

		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(potion);
		if (mPlayer != null && playerItemStats != null) {
			for (LivingEntity entity : affectedEntities) {
				if (EntityUtils.isHostileMob(entity)) {
					DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), DAMAGE[mRarity - 1], false, true, false);

					EntityUtils.applyVulnerability(mPlugin, DURATION, VULNERABILITY[mRarity - 1], entity);
					EntityUtils.applySlow(mPlugin, DURATION, SLOWNESS, entity);
				}
			}
		}

		return true;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlayer == null) {
			return;
		}
		mCount++;
		if (mCount >= KILLS_PER) {
			mCount = 0;

			Inventory inv = mPlayer.getInventory();
			ItemStack firstFoundPotStack = null;
			int potCount = 0;

			for (ItemStack item : inv.getContents()) {
				if (item != null && InventoryUtils.testForItemWithName(item, POTION_NAME)) {
					if (firstFoundPotStack == null) {
						firstFoundPotStack = item;
					}
					potCount += item.getAmount();
				}
			}

			if (potCount < MAX_STACK) {
				if (firstFoundPotStack != null) {
					firstFoundPotStack.setAmount(firstFoundPotStack.getAmount() + 1);
				} else {
					ItemStack newPotions = getLightningBottle();
					newPotions.setAmount(1);
					inv.addItem(newPotions);
				}
			}
		}
	}

	@Override
	public double entityDeathRadius() {
		return DEATH_RADIUS;
	}

	public ItemStack getLightningBottle() {
		ItemStack itemStack = new ItemStack(Material.SPLASH_POTION, 1);
		PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

		potionMeta.setBasePotionData(new PotionData(PotionType.MUNDANE));
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // Hide "No Effects" vanilla potion effect lore
		potionMeta.setColor(Color.YELLOW);
		String plainName = POTION_NAME;
		potionMeta.setDisplayName(ChatColor.AQUA + plainName); // OG Alchemist's Potion item name colour of &b

		List<String> loreList = Arrays.asList(
			ChatColor.DARK_GRAY + "A unique potion used by Dawnbringers." // Standard Monumenta lore text colour of &8
		);
		potionMeta.setLore(loreList);

		itemStack.setItemMeta(potionMeta);
		ItemUtils.setPlainTag(itemStack); // Support for resource pack textures like with other items & mechanisms
		return itemStack;
	}

	@Override
	public String getDescription(int rarity) {
		return "For every " + KILLS_PER + " mobs that die within " + DEATH_RADIUS + " blocks of you, you gain a lightning bottle, which stack up to " + MAX_STACK + ". Throwing a lightning bottle deals " + DepthsUtils.getRarityColor(rarity) + (float)DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage and applies " + (int) DepthsUtils.roundPercent(SLOWNESS) + "% slowness and " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(VULNERABILITY[rarity - 1]) + "%" + ChatColor.WHITE + " vulnerability for " + DURATION / 20 + " seconds.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}
}
