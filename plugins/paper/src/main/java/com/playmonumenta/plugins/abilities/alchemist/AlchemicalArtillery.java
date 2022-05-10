package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;



public class AlchemicalArtillery extends Ability {
	public static final String ARTILLERY_POTION_TAG = "ArtilleryPotion";
	public static final String ACTIVE_TAG = "AlchArtActive";

	private static final double BOW_DAMAGE_MULTIPLIER = 0.35;

	private boolean mActive;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private boolean mHasTaboo;

	public AlchemicalArtillery(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Alchemical Artillery");
		mInfo.mScoreboardId = "Alchemical";
		mInfo.mShorthandName = "AA";
		mInfo.mDescriptions.add("Swap hands while holding a bow, crossbow, or trident to toggle shooting Alchemist's Potions instead of projectiles. Shooting a potion consumes the potion and applies the damage and any effects that potion would normally apply.");
		mInfo.mDescriptions.add("Potions shot with this ability have 35% of your projectile damage added to their base damage.");
		mInfo.mLinkedSpell = ClassAbility.ALCHEMICAL_ARTILLERY;
		mDisplayItem = new ItemStack(Material.CROSSBOW, 1);

		mActive = player != null && player.getScoreboardTags().contains(ACTIVE_TAG);
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mHasTaboo = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Taboo.class) != null;
		});
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer != null && mAlchemistPotions != null && mActive && (arrow.isCritical() || arrow instanceof Trident) && mAlchemistPotions.decrementCharge()) {
			ThrownPotion pot = mPlayer.getWorld().spawn(arrow.getLocation(), ThrownPotion.class);
			pot.setVelocity(arrow.getVelocity());
			pot.setShooter(mPlayer);
			mAlchemistPotions.setPotionToAlchemistPotion(pot);

			arrow.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(arrow);

			double bownus = 0;
			if (getAbilityScore() > 1) {
				PlayerInventory inv = mPlayer.getInventory();
				bownus = BOW_DAMAGE_MULTIPLIER * ItemStatUtils.getAttributeAmount(inv.getItemInMainHand(), AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
				double offhand = ItemStatUtils.getAttributeAmount(inv.getItemInOffHand(), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.OFFHAND);
				double head = ItemStatUtils.getAttributeAmount(inv.getHelmet(), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.HEAD);
				double shoulders = ItemStatUtils.getAttributeAmount(inv.getChestplate(), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.CHEST);
				double knees = ItemStatUtils.getAttributeAmount(inv.getLeggings(), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.LEGS);
				double andToes = ItemStatUtils.getAttributeAmount(inv.getBoots(), AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.FEET);
				bownus = bownus * (1 + (offhand + head + shoulders + knees + andToes));
			}

			pot.setMetadata(ARTILLERY_POTION_TAG, new FixedMetadataValue(mPlugin, bownus));
		}

		return true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null) {
			return;
		}

		if (mHasTaboo && mActive && mPlayer.isSneaking()) {
			return;
		}

		if (ItemUtils.isBowOrTrident(mPlayer.getInventory().getItemInMainHand())) {
			mActive = ScoreboardUtils.toggleTag(mPlayer, ACTIVE_TAG);
			String active;
			if (mActive) {
				active = "activated";
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1.25f);
			} else {
				active = "deactivated";
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 0.75f);
			}
			mPlayer.sendActionBar(ChatColor.YELLOW + "Alchemical Artillery has been " + active + "!");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.9f, 1.2f);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	public boolean isActive() {
		return mActive;
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
