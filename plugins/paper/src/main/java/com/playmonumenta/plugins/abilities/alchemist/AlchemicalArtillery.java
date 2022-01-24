package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.md_5.bungee.api.ChatColor;



public class AlchemicalArtillery extends Ability {
	public static final String ARTILLERY_POTION_TAG = "ArtilleryPotion";

	private static final double BOW_DAMAGE_MULTIPLIER = 0.25;

	private boolean mActive;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public AlchemicalArtillery(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Alchemical Artillery");
		mInfo.mScoreboardId = "Alchemical";
		mInfo.mShorthandName = "AA";
		mInfo.mDescriptions.add("Swap hands while holding a bow, crossbow, or trident to toggle shooting Alchemist's Potions instead of projectiles. Shooting a potion consumes the potion and applies the damage and any effects that potion would normally apply.");
		mInfo.mDescriptions.add("Potions shot with this ability have 25% of your projectile damage added to their base damage.");
		mInfo.mLinkedSpell = ClassAbility.ALCHEMICAL_ARTILLERY;
		mDisplayItem = new ItemStack(Material.CROSSBOW, 1);

		mActive = false;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer != null && mAlchemistPotions != null && mActive && arrow.isCritical() && mAlchemistPotions.decrementCharge()) {
			ThrownPotion pot = mPlayer.getWorld().spawn(arrow.getLocation(), ThrownPotion.class);
			pot.setVelocity(arrow.getVelocity());
			pot.setShooter(mPlayer);
			mAlchemistPotions.setPotionToAlchemistPotion(pot);

			double bownus = 0;
			if (getAbilityScore() > 1) {
				bownus = BOW_DAMAGE_MULTIPLIER * ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(), AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
				double offhand = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItem(45), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.OFFHAND);
				double head = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItem(5), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.HEAD);
				double shoulders = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItem(6), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.CHEST);
				double knees = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItem(7), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.LEGS);
				double andToes = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItem(8), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.FEET);
				bownus = bownus * (1 + (offhand + head + shoulders + knees + andToes));
			}

			pot.setMetadata(ARTILLERY_POTION_TAG, new FixedMetadataValue(mPlugin, bownus));
			arrow.remove();
		}

		return true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer != null && ItemUtils.isBowOrTrident(mPlayer.getInventory().getItemInMainHand())) {
			mActive = !mActive;
			String active = "";
			if (mActive) {
				active = "activated";
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1.25f);
			} else {
				active = "deactivated";
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 0.75f);
			}
			mPlayer.sendActionBar(ChatColor.YELLOW + "Alchemical Artillery has been " + active + "!");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.9f, 1.2f);
		}
	}
}
