package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class AlchemicalArtillery extends PotionAbility {
	public static final String ARTILLERY_POTION_TAG = "ArtilleryPotion";
	public static final String ACTIVE_TAG = "AlchArtActive";

	private static final double BOW_DAMAGE_MULTIPLIER = 0.25;

	private static final int ENHANCEMENT_EXPLOSION_DELAY = 20;
	private static final int ENHANCEMENT_EXPLOSION_RADIUS = 3;
	private static final float ENHANCEMENT_EXPLOSION_KNOCK_UP = 0.5f;
	private static final double ENHANCEMENT_EXPLOSION_POT_PERCENT_DAMAGE = 0.1;

	public static final String CHARM_MULTIPLIER = "Alchemical Artillery Projectile Damage Multiplier";
	public static final String CHARM_DELAY = "Alchemical Artillery Delay";
	public static final String CHARM_RADIUS = "Alchemical Artillery Radius";
	public static final String CHARM_KNOCKBACK = "Alchemical Artillery Knockback";
	public static final String CHARM_EXPLOSION_MULTIPLIER = "Alchemical Artillery Explosion Damage Multiplier";

	public static final AbilityInfo<AlchemicalArtillery> INFO =
		new AbilityInfo<>(AlchemicalArtillery.class, "Alchemical Artillery", AlchemicalArtillery::new)
			.linkedSpell(ClassAbility.ALCHEMICAL_ARTILLERY)
			.scoreboardId("Alchemical")
			.shorthandName("AA")
			.descriptions(
				"Swap hands while holding a bow, crossbow, or trident to toggle shooting Alchemist's Potions instead of projectiles. Shooting a potion consumes the potion and applies the damage and any effects that potion would normally apply.",
				"Potions shot with this ability have 25% of your projectile damage added to their base damage.",
				"1 second after the Artillery lands, cause an explosion that deals 10% of the potion's damage to and knocking up enemies in a 3 block radius.")
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", AlchemicalArtillery::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.CROSSBOW, 1));

	private boolean mActive;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public AlchemicalArtillery(Plugin plugin, Player player) {
		super(plugin, player, INFO, 0, 0);
		mActive = player != null && player.getScoreboardTags().contains(ACTIVE_TAG);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (mAlchemistPotions == null
			    || !mActive
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			    || !mAlchemistPotions.decrementCharge()) {
			return true;
		}
		// Preferably use the player's real direction for potions over the projectile's direction; only use projectile direction for multishot crossbows
		Vector direction = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);
		if (mPlayer.getInventory().getItemInMainHand().getType() == Material.CROSSBOW
			    && ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), ItemStatUtils.EnchantmentType.MULTISHOT)) {
			direction = projectile.getVelocity().normalize();
		}
		Location location = mPlayer.getEyeLocation().add(direction.clone().multiply(0.2));
		ThrownPotion pot = mPlayer.getWorld().spawn(location, ThrownPotion.class);
		Vector velocity = direction.clone().multiply(Math.min(projectile.getVelocity().length(), 5));
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
			bownus *= multiply;
		}

		pot.setMetadata(ARTILLERY_POTION_TAG, new FixedMetadataValue(mPlugin, bownus));

		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		double recoil = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.RECOIL);
		if (recoil > 0) {
			if (mPlayer.isSneaking()) {
				Material type = item.getType();
				if (mPlayer.getCooldown(type) < 10) {
					mPlayer.setCooldown(type, 10);
				}
			} else if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
				Recoil.applyRecoil(mPlayer, recoil);
			}
		}

		return false;

	}

	public void toggle() {
		mActive = ScoreboardUtils.toggleTag(mPlayer, ACTIVE_TAG);
		String active;
		if (mActive) {
			active = "activated";
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 1, 1.25f);
		} else {
			active = "deactivated";
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 1, 0.75f);
		}
		MessagingUtils.sendActionBarMessage(mPlayer, "Alchemical Artillery has been " + active + "!");
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.9f, 1.2f);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public void createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isEnhanced() && potion.hasMetadata(ARTILLERY_POTION_TAG) && mAlchemistPotions != null) {
			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_EXPLOSION_RADIUS);
			cancelOnDeath(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				new PartialParticle(Particle.EXPLOSION_LARGE, loc, (int) (4 * radius * radius / 9), radius, radius / 2.0, radius, 0.1).spawnAsPlayerActive(mPlayer);
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.2f);
				world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 0.8f);

				List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, radius).getHitMobs();
				double damage = ((mAlchemistPotions == null ? 0 : mAlchemistPotions.getDamage()) + MetadataUtils.getMetadata(potion, AlchemicalArtillery.ARTILLERY_POTION_TAG, 0.0)) * (ENHANCEMENT_EXPLOSION_POT_PERCENT_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_EXPLOSION_MULTIPLIER));
				float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, ENHANCEMENT_EXPLOSION_KNOCK_UP);
				for (LivingEntity mob : mobs) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
					MovementUtils.knockAway(loc, mob, knockback);
				}

			}, CharmManager.getDuration(mPlayer, CHARM_DELAY, ENHANCEMENT_EXPLOSION_DELAY)));
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
