package com.playmonumenta.plugins.gallery;

import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import com.playmonumenta.plugins.gallery.effects.GalleryReviveTimeEffect;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class GalleryGrave {
	private static final int REANIMATION_DURATION = 20 * 10;
	private static final int GRAVE_DURATION = 20 * 30;

	private final ArmorStand mGrave;
	private final ArmorStand mIndicator;
	private final GalleryPlayer mPlayer;
	private final GalleryGame mGame;

	private final BukkitRunnable mRunnable;

	private GalleryGrave(ArmorStand grave, ArmorStand indicator, GalleryPlayer player, GalleryGame game) {
		game.mGraves.add(this);
		mGrave = grave;
		mIndicator = indicator;
		mPlayer = player;
		mGame = game;

		mGrave.setGlowing(true);
		mRunnable = new BukkitRunnable() {
			int mTimer = 0;
			int mReanimationTimer = 0;
			@Override public void run() {
				if (!mPlayer.isOnline()) {
					cancel();
					removeGrave();
				}

				if (isCancelled()) {
					return;
				}


				Component indicatorName = Component.empty().append(Component.text("[", NamedTextColor.WHITE));

				int greenIndicator = (int) ((double) mReanimationTimer / (double) REANIMATION_DURATION * 20);
				for (int i = 0; i < greenIndicator; i++) {
					indicatorName = indicatorName.append(Component.text("|", NamedTextColor.GREEN));
				}
				int redIndicator = 20 - greenIndicator;
				for (int i = 0; i < redIndicator; i++) {
					indicatorName = indicatorName.append(Component.text("|", NamedTextColor.RED));
				}
				indicatorName = indicatorName.append(Component.text("]", NamedTextColor.WHITE));

				mIndicator.customName(indicatorName);

				//this way of doing this is a bit junk and should be rewritten with a cleaner way...someday..
				int reanimationReduction = 0;
				boolean someoneIsReviving = false;
				for (Player player : mGrave.getWorld().getNearbyPlayers(mGrave.getLocation(), 1.5)) {
					if (player.isSneaking()) {
						someoneIsReviving = true;
						GalleryPlayer gPlayer = game.getGalleryPlayer(player.getUniqueId());
						if (gPlayer != null) {
							GalleryReviveTimeEffect effect = (GalleryReviveTimeEffect) gPlayer.getEffectOfType(GalleryEffectType.REVIVE_TIME);
							if (effect != null) {
								reanimationReduction = effect.getCurrentStacks() * GalleryReviveTimeEffect.REVIVE_TIME_EFFECT_PER_STACK;
							}
						}
						mReanimationTimer += 10;
						break;
					}
				}

				if (mReanimationTimer >= REANIMATION_DURATION - reanimationReduction) {
					respawnPlayer();
				}

				if (someoneIsReviving) {
					return;
				}

				if (mTimer >= GRAVE_DURATION) {
					//Timer is out - player can no longer respawn
					cancel();
					removeGrave();
					mGame.sendMessageToPlayers(mPlayer.getPlayer().getName() + " has fallen, they will be back next round.");
				}
				mReanimationTimer -= 20;
				mTimer += 10;
				if (mReanimationTimer <= 0) {
					mReanimationTimer = 0;
				}

				if (mTimer <= GRAVE_DURATION / 3) {
					ScoreboardUtils.addEntityToTeam(mGrave, "Green");
				} else if (mTimer <= GRAVE_DURATION * 2 / 3) {
					ScoreboardUtils.addEntityToTeam(mGrave, "Orange");
				} else {
					ScoreboardUtils.addEntityToTeam(mGrave, "Red");
				}
			}
		};

		mRunnable.runTaskTimer(GalleryManager.mPlugin, 0, 10);
	}


	public void respawnPlayer() {
		//TODO - sound and particle
		mPlayer.setAlive(true);
		Player player = mPlayer.getPlayer();
		if (player != null) {
			player.teleport(mGrave);
		} else {
			mPlayer.setShouldTeleportWhenJoining(true);
		}
		removeGrave();
	}

	public void removeGrave() {
		mRunnable.cancel();
		mGrave.remove();
		mIndicator.remove();
		mGame.mGraves.remove(this);
	}

	public static GalleryGrave createGrave(GalleryPlayer player, Player bukkitPlayer, Location location, GalleryGame game) {
		ArmorStand grave = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
		grave.setGravity(false);
		grave.setInvulnerable(true);
		ArmorStand indicator = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
		indicator.setGravity(false);
		indicator.teleport(location.clone().add(0, 0.5, 0));
		indicator.setInvulnerable(true);
		indicator.setCustomNameVisible(true);
		indicator.setInvisible(true);
		indicator.addDisabledSlots(EquipmentSlot.values());

		PlayerInventory playerInventory = bukkitPlayer.getInventory();

		ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setPlayerProfile(bukkitPlayer.getPlayerProfile());
		skull.setItemMeta(meta);

		grave.setArms(true);
		grave.setBasePlate(false);
		grave.setItem(EquipmentSlot.HEAD, skull);
		grave.setItem(EquipmentSlot.CHEST, playerInventory.getItem(EquipmentSlot.CHEST));
		grave.setItem(EquipmentSlot.LEGS, playerInventory.getItem(EquipmentSlot.LEGS));
		grave.setItem(EquipmentSlot.FEET, playerInventory.getItem(EquipmentSlot.FEET));
		grave.setItem(EquipmentSlot.HAND, playerInventory.getItem(EquipmentSlot.HAND));
		grave.setItem(EquipmentSlot.OFF_HAND, playerInventory.getItem(EquipmentSlot.OFF_HAND));
		grave.addDisabledSlots(EquipmentSlot.values());
		grave.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
		grave.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.REMOVING_OR_CHANGING);
		grave.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.REMOVING_OR_CHANGING);
		grave.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.REMOVING_OR_CHANGING);
		grave.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.REMOVING_OR_CHANGING);
		grave.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.REMOVING_OR_CHANGING);
		//TODO - set grave position like it is all on the floor
		grave.setCustomNameVisible(true);
		grave.customName(Component.text(bukkitPlayer.getName() + "'s grave"));

		return new GalleryGrave(grave, indicator, player, game);

	}
}
