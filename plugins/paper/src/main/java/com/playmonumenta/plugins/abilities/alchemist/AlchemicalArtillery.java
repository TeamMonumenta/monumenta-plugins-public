package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;


public class AlchemicalArtillery extends PotionAbility {
	public static final String ARTILLERY_POTION_TAG = "ArtilleryPotion";
	public static final String ACTIVE_TAG = "AlchArtActive";

	private static final double BOW_DAMAGE_MULTIPLIER = 0.35;

	private static final int ENHANCEMENT_EXPLOSION_DELAY = 20;
	private static final int ENHANCEMENT_EXPLOSION_RADIUS = 3;
	private static final float ENHANCEMENT_EXPLOSION_KNOCK_UP = 1.5f;
	private static final double ENHANCEMENT_EXPLOSION_POT_PERCENT_DAMAGE = 0.15;

	public static final String CHARM_MULTIPLIER = "Alchemical Artillery Projectile Damage Multiplier";
	public static final String CHARM_DELAY = "Alchemical Artillery Delay";
	public static final String CHARM_RADIUS = "Alchemical Artillery Radius";
	public static final String CHARM_KNOCKBACK = "Alchemical Artillery Knockback";
	public static final String CHARM_EXPLOSION_MULTIPLIER = "Alchemical Artillery Explosion Damage Multiplier";

	private boolean mActive;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private boolean mHasTaboo;

	public AlchemicalArtillery(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Alchemical Artillery", 0, 0);
		mInfo.mScoreboardId = "Alchemical";
		mInfo.mShorthandName = "AA";
		mInfo.mDescriptions.add("Swap hands while holding a bow, crossbow, or trident to toggle shooting Alchemist's Potions instead of projectiles. Shooting a potion consumes the potion and applies the damage and any effects that potion would normally apply.");
		mInfo.mDescriptions.add("Potions shot with this ability have 35% of your projectile damage added to their base damage.");
		mInfo.mDescriptions.add("1 second after the Artillery lands, cause an explosion that deals 15% of the potion's damage to and knocking up enemies in a 3 block radius.");
		mInfo.mLinkedSpell = ClassAbility.ALCHEMICAL_ARTILLERY;
		mDisplayItem = new ItemStack(Material.CROSSBOW, 1);

		mActive = player != null && player.getScoreboardTags().contains(ACTIVE_TAG);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mHasTaboo = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Taboo.class) != null;
		});
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (mPlayer != null && mAlchemistPotions != null
			&& mActive && EntityUtils.isAbilityTriggeringProjectile(projectile, true) && mAlchemistPotions.decrementCharge()) {
			ThrownPotion pot = mPlayer.getWorld().spawn(projectile.getLocation(), ThrownPotion.class);
			Vector velocity = projectile.getVelocity();
			double speed = velocity.length();
			if (speed > 5) { // fast potions tend to explode in your face, so limit speed to some acceptable value
				velocity = velocity.normalize().multiply(5);
			}
			pot.setVelocity(velocity);
			pot.setShooter(mPlayer);
			mAlchemistPotions.setPotionToAlchemistPotion(pot);

			projectile.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(projectile);

			// give back a normal arrow when a crossbow is shot
			if (mPlayer.getInventory().getItemInMainHand().getType() == Material.CROSSBOW
				    && mPlayer.getGameMode() != GameMode.CREATIVE
					&& projectile instanceof AbstractArrow arrow
				    && arrow.getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
				InventoryUtils.giveItem(mPlayer, new ItemStack(Material.ARROW), true);
			}

			double bownus = 0;
			if (isLevelTwo()) {
				PlayerInventory inv = mPlayer.getInventory();
				bownus = (BOW_DAMAGE_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MULTIPLIER)) * ItemStatUtils.getAttributeAmount(inv.getItemInMainHand(), AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
				double multiply = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
				if (multiply != 0) {
					bownus *= multiply;
				}
			}

			pot.setMetadata(ARTILLERY_POTION_TAG, new FixedMetadataValue(mPlugin, bownus));

			return false;
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

	@Override
	public void createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isEnhanced() && potion.hasMetadata(ARTILLERY_POTION_TAG) && mAlchemistPotions != null) {
			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_EXPLOSION_RADIUS);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				new PartialParticle(Particle.EXPLOSION_LARGE, loc, (int) (4 * radius * radius / 9), radius, radius / 2.0, radius, 0.1).spawnAsPlayerActive(mPlayer);
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
				world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.8f);

				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, radius);
				double damage = (mAlchemistPotions.getDamage() + potion.getMetadata(AlchemicalArtillery.ARTILLERY_POTION_TAG).get(0).asDouble()) * (ENHANCEMENT_EXPLOSION_POT_PERCENT_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_EXPLOSION_MULTIPLIER));
				float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, ENHANCEMENT_EXPLOSION_KNOCK_UP);
				for (LivingEntity mob : mobs) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), damage, true, false, false);
					MovementUtils.knockAway(loc, mob, knockback);
				}

			}, ENHANCEMENT_EXPLOSION_DELAY + CharmManager.getExtraDuration(mPlayer, CHARM_DELAY));
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
