package com.playmonumenta.plugins.gallery;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.cosmetics.poses.GravePose;
import com.playmonumenta.plugins.cosmetics.poses.GravePoses;
import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import com.playmonumenta.plugins.gallery.effects.GalleryReviveTimeEffect;
import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
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

			@Override
			public void run() {
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
					Player player = mPlayer.getPlayer();
					if (player != null) {
						mGame.sendMessageToPlayers(player.getName() + " has fallen, they will be back next round.");
					}
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

	public GalleryPlayer getPlayer() {
		return mPlayer;
	}

	public void respawnPlayer() {
		//TODO - sound and particle
		mPlayer.setAlive(true);
		Player player = mPlayer.getPlayer();
		if (player != null) {
			player.teleport(mGrave);
			mGame.sendMessageToPlayers(player.getName() + " has been revived!", mPlayer);
			mPlayer.sendMessage("You have been revived!");
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
		ArmorStand lIndicator = location.getWorld().spawn(location.clone().add(0, 1, 0), ArmorStand.class, indicator -> {
			indicator.setMarker(true);
			indicator.setGravity(false);
			indicator.setInvulnerable(true);
			indicator.setCustomNameVisible(true);
			indicator.setInvisible(true);
			indicator.addDisabledSlots(EquipmentSlot.values());
			indicator.addScoreboardTag(GraveManager.DISABLE_INTERACTION_TAG);
		});

		ArmorStand lGrave = location.getWorld().spawn(location, ArmorStand.class, g -> {
			g.setInvulnerable(true);
			g.setDisabledSlots(EquipmentSlot.values());
			g.setCollidable(false);
			g.setBasePlate(false);
			g.customName(Component.text(bukkitPlayer.getName() + "'s Grave", NamedTextColor.RED));
			g.setCustomNameVisible(true);
			g.setGlowing(true);
			g.setArms(true);
			g.setGravity(false);
			ScoreboardUtils.addEntityToTeam(g, "GraveGreen", NamedTextColor.GREEN);
			g.addScoreboardTag(GraveManager.DISABLE_INTERACTION_TAG);
			GravePose mGravePose = GravePoses.getEquippedGravePose(bukkitPlayer);
			g.setHeadPose(mGravePose.getHeadAngle(false));
			g.setBodyPose(mGravePose.getBodyAngle(false));
			g.setLeftArmPose(mGravePose.getLeftArmAngle(false));
			g.setRightArmPose(mGravePose.getRightArmAngle(false));
			g.setLeftLegPose(mGravePose.getLeftLegAngle(false));
			g.setRightLegPose(mGravePose.getRightLegAngle(false));
			VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(bukkitPlayer);
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack item;
				if (slot != EquipmentSlot.HEAD) {
					item = ItemUtils.clone(bukkitPlayer.getInventory().getItem(slot).clone());
					if (ItemUtils.isNullOrAir(item)) {
						continue;
					}
					VanityManager.applyVanity(item, vanityData, slot, false);
					// usb: remove stats from item before adding to armorstand in case of dupe
					// this should happen after applying vanity
					ItemUpdateHelper.removeStats(item);
				} else {
					item = new ItemStack(Material.PLAYER_HEAD);
					if (item.getItemMeta() instanceof SkullMeta skullMeta) {
						skullMeta.setOwningPlayer(bukkitPlayer);
						item.setItemMeta(skullMeta);
					}
				}
				g.setItem(slot, item);
			}
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();
			meta.setPlayerProfile(bukkitPlayer.getPlayerProfile());
			skull.setItemMeta(meta);
			g.setItem(EquipmentSlot.HEAD, skull);
		});

		return new GalleryGrave(lGrave, lIndicator, player, game);

	}
}
