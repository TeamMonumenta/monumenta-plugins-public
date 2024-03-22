package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.IndependentIframeTracker;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeAlchemyCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/*
 * Handles giving potions and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements AbilityWithChargesOrStacks {

	private static final String GRUESOME_MODE_TAG = "AlchPotionGruesomeMode";
	public static final String METADATA_KEY = "PlayerThrowAlchPotionEvent";
	public static final int MAX_CHARGES = 8;
	public static final int POTIONS_TIMER_BASE = 2 * 20;
	private static final int POTIONS_TIMER_TOWN = 1 * 20;
	private static final int IFRAME_BETWEEN_POT = 10;
	private static final String POTION_SCOREBOARD = "StoredPotions";

	public static final String CHARM_CHARGES = "Alchemist Potion Charges";
	public static final String CHARM_DAMAGE = "Alchemist Potion Damage";
	public static final String CHARM_RADIUS = "Alchemist Potion Radius";

	private final List<PotionAbility> mPotionAbilities = new ArrayList<>();
	private double mTimer = 0;
	private int mSlot;
	private final int mMaxCharges;
	private int mCharges;
	private int mChargeTime;
	private final IndependentIframeTracker mIframeTracker;
	private final WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	private static @Nullable ItemStack GRUESOME_POTION = null;
	private static @Nullable ItemStack BRUTAL_POTION = null;

	public static final AbilityInfo<AlchemistPotions> INFO =
			new AbilityInfo<>(AlchemistPotions.class, null, AlchemistPotions::new)
					.linkedSpell(ClassAbility.ALCHEMIST_POTION)
					.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Alchemist.CLASS_ID);

	public final GruesomeAlchemyCS mCosmetic;
	private boolean mHasGruesomeAlchemy = false;

	private @Nullable ProjectileHitEvent mLastHitEvent;

	public AlchemistPotions(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mIframeTracker = new IndependentIframeTracker(IFRAME_BETWEEN_POT);

		mChargeTime = POTIONS_TIMER_BASE;
		mMaxCharges = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CHARGES, MAX_CHARGES);
		mCharges = Math.min(ScoreboardUtils.getScoreboardValue(player, POTION_SCOREBOARD).orElse(0), mMaxCharges);

		mPlayerItemStatsMap = new WeakHashMap<>();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS());

		// Scan hotbar for alch potion
		PlayerInventory inv = player.getInventory();
		mSlot = 0;
		for (int i = 0; i < 9; i++) {
			if (ItemUtils.isAlchemistItem(inv.getItem(i))) {
				mSlot = i;
				break;
			}
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			List<? extends PotionAbility> potionAbilities = Stream.of(GruesomeAlchemy.class, BrutalAlchemy.class, EmpoweringOdor.class, TransmutationRing.class, EsotericEnhancements.class, ScorchedEarth.class)
				.map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c)).filter(Objects::nonNull).toList();

			for (PotionAbility potionAbility : potionAbilities) {
				mPotionAbilities.add(potionAbility);

				if (potionAbility instanceof GruesomeAlchemy) {
					mHasGruesomeAlchemy = true;
				} else if (potionAbility instanceof EmpoweringOdor odor && odor.isLevelTwo()) {
					mChargeTime -= EmpoweringOdor.POTION_RECHARGE_TIME_REDUCTION_2;
				}
			}
		});
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && ItemUtils.isAlchemistItem(potion.getItem())) {
			mPlayer.setMetadata(METADATA_KEY, new FixedMetadataValue(mPlugin, mPlayer.getTicksLived()));
			if (decrementCharge()) {
				setPotionToAlchemistPotion(potion, isGruesomeMode());
				for (PotionAbility potionAbility : mPotionAbilities) {
					potionAbility.alchemistPotionThrown(potion);
				}
			} else {
				potion.remove();
			}
		}

		return true;
	}

	public void throwPotion(boolean gruesome) {
		if (decrementCharge()) {
			ThrownPotion thrownPotion = mPlayer.launchProjectile(ThrownPotion.class);
			setPotionToAlchemistPotion(thrownPotion, gruesome);
		}
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, with also the damage and if it's gruesome or brutal mode
	 */
	public void setPotionToAlchemistPotion(ThrownPotion potion, boolean gruesome) {
		mPlayerItemStatsMap.put(potion, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		if (gruesome) {
			potion.setMetadata("GruesomeAlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}

		setPotionAlchemistPotionAesthetic(potion, gruesome);
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, ONLY to an aesthetic level
	 */
	public void setPotionAlchemistPotionAesthetic(ThrownPotion potion, boolean gruesome) {
		if (BRUTAL_POTION == null || GRUESOME_POTION == null) {
			ItemStack basePotion = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString("epic:r1/items/alchemists_potion"));
			if (basePotion == null) {
				mPlugin.getLogger().severe("Failed to get alchemist's potion from loot table!");
				return;
			}

			BRUTAL_POTION = basePotion.clone();
			GRUESOME_POTION = basePotion.clone();
		}

		ItemStack item;
		if (gruesome) {
			item = GRUESOME_POTION.clone();
		} else {
			item = BRUTAL_POTION.clone();
		}
		item.editMeta(m -> {
			((PotionMeta) m).setColor(mCosmetic.splashColor(gruesome));
		});
		potion.setItem(item);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		// Prevent the potion from splashing on players, summons, and other non-hostile mobs
		// Most importantly, this prevents the potion from instantly splashing on the throwing player with certain combinations of projectile speed and throw direction.
		if (proj instanceof ThrownPotion potion
			&& mPlayerItemStatsMap.containsKey(potion)
			&& event.getHitEntity() != null) {
			if (!EntityUtils.isHostileMob(event.getHitEntity())) {
				event.setCancelled(true);
			}
		}
		mLastHitEvent = event;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(potion);
		if (playerItemStats != null) {
			// Get the real splash location of the potion, which is ahead of where it currently is.
			// While a PotionSplashEvent is also a ProjectileHitEvent, it does not have all the data of that event, so cannot be relied upon.
			// Thus, we store the last ProjectileHitEvent and use that if possible.
			Location loc = mLastHitEvent != null && mLastHitEvent.getEntity() == potion ? EntityUtils.getProjectileHitLocation(mLastHitEvent) : potion.getLocation();

			// Sometimes, the potion just randomly splashes several blocks away from where it actually lands. Force it to splash at the correct location.
			potion.teleport(loc.clone().add(0, 0.25, 0));
			potion.setVelocity(new Vector(0, 0, 0));

			boolean isGruesome = isGruesome(potion);

			double radius = getRadius(playerItemStats);

			boolean isSpecialPot = false;
			for (PotionAbility potionAbility : mPotionAbilities) {
				isSpecialPot |= potionAbility.createAura(loc, potion, playerItemStats);
			}

			mCosmetic.effectsOnSplash(mPlayer, loc, isGruesome, radius, isSpecialPot);

			Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
			for (LivingEntity entity : hitbox.getHitMobs()) {
				apply(entity, potion, isGruesome, playerItemStats);
			}

			List<Player> players = PlayerUtils.playersInRange(loc, radius, true);
			players.remove(mPlayer);
			players.forEach(player -> applyToPlayer(player, potion, isGruesome));
		}

		return true;
	}

	public void apply(LivingEntity mob, ThrownPotion potion, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mob, "AlchemistPotionApplying") && !mob.isDead()) {
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, getDamage(playerItemStats));

			if (isGruesome) {
				damage *= GruesomeAlchemy.GRUESOME_POTION_DAMAGE_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_DAMAGE);
			}

			double finalDamage = damage;
			mIframeTracker.damage(mob, () -> DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), finalDamage, true, true, false));

			// Intentionally apply effects after damage
			applyEffects(mob, isGruesome, playerItemStats);
		}
	}

	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, "AlchemistPotionApplying")) {
			for (PotionAbility potionAbility : mPotionAbilities) {
				potionAbility.applyToPlayer(player, potion, isGruesome);
			}
		}
	}

	public void applyEffects(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		// Apply potions effects but no damage
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob, isGruesome, playerItemStats);
		}
	}

	public boolean decrementCharge() {
		if (mCharges <= 0) {
			return false;
		}

		// Check if the player has an alchemical utensil on the hotbar
		PlayerInventory inventory = mPlayer.getInventory();
		if (ItemUtils.isAlchemistItem(inventory.getItemInMainHand())) {
			mSlot = inventory.getHeldItemSlot();
		} else if (!ItemUtils.isAlchemistItem(inventory.getItem(mSlot))) {
			boolean found = false;
			for (int i = 0; i < 9; i++) {
				if (ItemUtils.isAlchemistItem(inventory.getItem(i))) {
					mSlot = i;
					found = true;
					break;
				}
			}
			if (!found) {
				// Cannot find alch bag
				return false;
			}
		}

		mCharges--;
		ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);

		PlayerUtils.callAbilityCastEvent(mPlayer, this, ClassAbility.ALCHEMIST_POTION);

		updateAlchemistItem();

		ClientModHandler.updateAbility(mPlayer, this);

		return true;
	}

	public boolean decrementCharges(int charges) {
		if (mCharges >= charges) {
			for (int i = 0; i < charges; i++) {
				if (!decrementCharge()) {
					//Enough charges but cannot remove for other reason such as no bag
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean incrementCharge() {
		if (mCharges < mMaxCharges) {
			mCharges++;
			ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
			updateAlchemistItem();
			ClientModHandler.updateAbility(mPlayer, this);
			return true;
		}
		return false;
	}

	public void incrementCharges(int charges) {
		for (int i = 0; i < charges; i++) {
			incrementCharge();
		}
	}

	private boolean mOnCooldown = false;

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {

		if (mOnCooldown) {
			mTimer += 5;
			int chargeTime = ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.RESIST_5) ? Math.min(POTIONS_TIMER_TOWN, mChargeTime) : mChargeTime;
			if (mTimer >= chargeTime) {
				mTimer -= chargeTime;
				incrementCharge();
				mOnCooldown = false;
			}
		}

		if (mCharges < mMaxCharges) {
			mOnCooldown = true;
		}

		if (mCharges > mMaxCharges) {
			mCharges = mMaxCharges;
			ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
			// Update item

			updateAlchemistItem();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		// Old code to prevent Sacred Provisions from duplicating alch pots.
		// That bug has since been fixed, but this is left just in case something similar happens again.
		ItemStack item = mPlayer.getInventory().getItem(mSlot);
		if (item != null && item.getAmount() > 1 && ItemUtils.isAlchemistItem(item)) {
			item.setAmount(1);
		}
	}

	public void swapMode() {
		boolean gruesome = ScoreboardUtils.toggleTag(mPlayer, GRUESOME_MODE_TAG);
		String mode = gruesome ? "Gruesome" : "Brutal";

		sendActionBarMessage("Alchemist's Potions swapped to " + mode + " mode");
		updateAlchemistItem();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public boolean isGruesomeMode() {
		return mHasGruesomeAlchemy && mPlayer.getScoreboardTags().contains(GRUESOME_MODE_TAG);
	}

	private boolean isGruesome(ThrownPotion potion) {
		return potion.hasMetadata("GruesomeAlchemistPotion");
	}

	public void increaseChargeTime(int ticks) {
		mChargeTime += ticks;
	}

	public void reduceChargeTime(int ticks) {
		mChargeTime -= ticks;
	}

	/**
	 * Modifies the time until the next potion is ready, in ticks. Add positive values to make it ready faster, and negative to make it slower.
	 */
	public void modifyCurrentPotionTimer(double ticks) {
		if (mOnCooldown) {
			mTimer += ticks;
		}
	}

	public double getDamage() {
		return getDamage(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer));
	}

	public double getDamage(ItemStatManager.PlayerItemStats playerItemStats) {
		return playerItemStats.getItemStats().get(AttributeType.POTION_DAMAGE.getItemStat());
	}

	public double getRadius() {
		return getRadius(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer));
	}

	public double getRadius(ItemStatManager.PlayerItemStats playerItemStats) {
		return CharmManager.getRadius(mPlayer, CHARM_RADIUS, playerItemStats.getItemStats().get(AttributeType.POTION_RADIUS.getItemStat()));
	}

	public double getSpeed() {
		return getSpeed(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer));
	}

	public double getSpeed(ItemStatManager.PlayerItemStats playerItemStats) {
		return playerItemStats.getItemStats().get(AttributeType.PROJECTILE_SPEED.getItemStat());
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	private void updateAlchemistItem() {
		// Display is handled virtually, just need to update the player's inventory to show changes
		mPlayer.updateInventory();
	}

	@Override
	public @Nullable String getMode() {
		return isGruesomeMode() ? "gruesome" : null;
	}

	public boolean isAlchemistPotion(ThrownPotion potion) {
		return BRUTAL_POTION != null && ItemUtils.getPlainNameIfExists(BRUTAL_POTION).equals(ItemUtils.getPlainNameIfExists(potion.getItem()));
	}
}
