package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.abilities.alchemist.PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION;
import static com.playmonumenta.plugins.utils.DescriptionUtils.TRIGGER_TEXT;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

/*
 * Handles giving potions and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements AbilityWithChargesOrStacks {

	private static final String GRUESOME_MODE_TAG = "AlchPotionGruesomeMode";
	private static final String GRUESOME_POTION_METAKEY = "GruesomeAlchemistPotion";
	public static final String METADATA_KEY = "PlayerThrowAlchPotionEvent";
	public static final int MAX_CHARGES = 8;
	public static final int POTIONS_TIMER_BASE = (int) (1.5 * 20);
	private static final int POTIONS_TIMER_TOWN = 1 * 20;
	public static final int IFRAME_BETWEEN_POT = 10;
	private static final String POTION_SCOREBOARD = "StoredPotions";

	public static final String CHARM_CHARGES = "Alchemist Potion Charges";
	public static final String CHARM_DAMAGE = "Alchemist Potion Damage";
	public static final String CHARM_RADIUS = "Alchemist Potion Radius";
	public static final String CHARM_RECHARGE_RATE = "Alchemist Potion Recharge Rate";

	private final List<PotionAbility> mPotionAbilities = new ArrayList<>();
	private double mTimer = 0;
	private int mSlot;
	private @Nullable ItemStack mItemInSlot;
	private final int mMaxCharges;
	private int mCharges;
	private int mChargeTime;
	private final IndependentIframeTracker mIframeTracker;
	private final WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;
	private final HashMap<String, Double> mRechargeRateMultipliers;

	public static final AbilityInfo<AlchemistPotions> INFO =
		new AbilityInfo<>(AlchemistPotions.class, "Alchemist Potions", AlchemistPotions::new)
			.hotbarName("A") // Have this as "A" to make it sorted in front of everything (alphabetically)
			.linkedSpell(ClassAbility.ALCHEMIST_POTION)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Alchemist.CLASS_ID)
			.addTrigger(new AbilityTriggerInfo<>("throwOpposite", "throw opposite potion", "Throws a potion of the opposite type, e.g. a gruesome potion if brutal potions are selected.",
				AlchemistPotions::throwOpposite, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false), HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", "Toggles between throwing gruesome or brutal potions.",
				AlchemistPotions::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).enabled(false), HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH);

	public final GruesomeAlchemyCS mCosmetic;
	private final int mLevelZeroDuration;
	private final double mLevelZeroSlownessAmount;
	private final double mLevelZeroVulnerabilityAmount;
	private final double mLevelZeroWeakenAmount;
	private final double mGruesomeDamageMultiplier;
	private final double mBrutalDamageMultiplier;

	private double mRechargeRateCoefficient = 1;
	private @Nullable ProjectileHitEvent mLastHitEvent;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;

	public AlchemistPotions(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mIframeTracker = new IndependentIframeTracker(IFRAME_BETWEEN_POT);

		mChargeTime = POTIONS_TIMER_BASE;
		mMaxCharges = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CHARGES, MAX_CHARGES);
		mCharges = Math.min(ScoreboardUtils.getScoreboardValue(player, POTION_SCOREBOARD).orElse(0), mMaxCharges);
		mLevelZeroDuration = CharmManager.getDuration(mPlayer, GruesomeAlchemy.CHARM_DURATION, GruesomeAlchemy.GRUESOME_ALCHEMY_DURATION);
		mLevelZeroSlownessAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_0_SLOWNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_SLOWNESS);
		mLevelZeroVulnerabilityAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_0_VULNERABILITY_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_VULNERABILITY);
		mLevelZeroWeakenAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_0_WEAKEN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_WEAKEN);
		mGruesomeDamageMultiplier = GruesomeAlchemy.GRUESOME_POTION_DAMAGE_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_DAMAGE_MULTIPLIER);
		mBrutalDamageMultiplier = BrutalAlchemy.BRUTAL_POTION_DAMAGE_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, BrutalAlchemy.CHARM_DAMAGE_MULTIPLIER);
		mPlayerItemStatsMap = new WeakHashMap<>();
		mRechargeRateMultipliers = new HashMap<>();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS());

		// Scan hotbar for alchemical utensil
		PlayerInventory inv = player.getInventory();
		mSlot = 0;
		for (int i = 0; i < 9; i++) {
			ItemStack item = inv.getItem(i);
			if (ItemUtils.isAlchemistItem(item)) {
				mSlot = i;
				mItemInSlot = item;
				mRechargeRateCoefficient = getRechargeRate(item);
				break;
			}
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			List<? extends PotionAbility> potionAbilities = Stream.of(GruesomeAlchemy.class, BrutalAlchemy.class, VolatileReaction.class, TransmutationRing.class, EsotericEnhancements.class, ScorchedEarth.class, EnergizingElixir.class)
				.map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c)).filter(Objects::nonNull).toList();

			mPotionAbilities.addAll(potionAbilities);
			mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
			mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
		});
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion, ProjectileLaunchEvent e) {
		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && mPlayer.getInventory().getHeldItemSlot() == mSlot && ItemUtils.isAlchemistItem(potion.getItem())) {
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
		throwPotion(gruesome, null);
	}

	public void throwPotion(boolean gruesome, @Nullable String customMetaKey) {
		if (decrementCharge()) {
			ThrownPotion thrownPotion = mPlayer.launchProjectile(ThrownPotion.class);
			thrownPotion.setItem(mPlayer.getEquipment().getItemInMainHand());
			setPotionToAlchemistPotion(thrownPotion, gruesome, customMetaKey);
		}
	}

	public boolean toggle() {
		mCosmetic.effectsOnSwap(mPlayer, isGruesomeMode());
		swapMode();
		return true;
	}

	private boolean throwOpposite() {
		if (MetadataUtils.checkOnceInRecentTicks(mPlugin, mPlayer, "GruesomeAlchemy_throwOpposite", 3)) {
			throwPotion(!isGruesomeMode());
			return true;
		}
		return false;
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, with also the damage and if it's gruesome or brutal mode
	 */
	public void setPotionToAlchemistPotion(ThrownPotion potion, boolean gruesome) {
		setPotionToAlchemistPotion(potion, gruesome, null);
	}

	public void setPotionToAlchemistPotion(ThrownPotion potion, boolean gruesome, @Nullable String customMetaKey) {
		mPlayerItemStatsMap.put(potion, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		if (gruesome) {
			potion.setMetadata(GRUESOME_POTION_METAKEY, new FixedMetadataValue(mPlugin, 0));
		}
		if (customMetaKey != null) {
			potion.setMetadata(customMetaKey, new FixedMetadataValue(mPlugin, 0));
		}

		setPotionAlchemistPotionAesthetic(potion, gruesome);
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, ONLY to an aesthetic level
	 */
	public void setPotionAlchemistPotionAesthetic(ThrownPotion potion, boolean gruesome) {
		ItemStack item = potion.getItem();
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
			Vector originalPotionVelocity = potion.getVelocity();
			potion.setVelocity(new Vector(0, 0, 0));

			boolean isGruesome = isGruesome(potion);

			double radius = getRadius(playerItemStats);

			boolean isSpecialPot = false;
			for (PotionAbility potionAbility : mPotionAbilities) {
				isSpecialPot |= potionAbility.createAura(loc, potion, originalPotionVelocity, playerItemStats);
			}

			if (potion.hasMetadata(VolatileReaction.POTION_METAKEY)) {
				return true;
			}
			mCosmetic.effectsOnSplash(mPlayer, loc, isGruesome, radius, isSpecialPot);

			Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
			for (LivingEntity entity : hitbox.getHitMobs()) {
				apply(entity, potion, isGruesome, playerItemStats);
			}

			List<Player> players = PlayerUtils.playersInRange(loc, radius, true);
			players.forEach(player -> applyToPlayer(player, potion, isGruesome));
		}

		return true;
	}

	public void apply(LivingEntity mob, ThrownPotion potion, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mob, "AlchemistPotionApplying") && !mob.isDead()) {
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, getDamage(playerItemStats));

			if (isGruesome) {
				damage *= mGruesomeDamageMultiplier;
			} else {
				damage *= mBrutalDamageMultiplier;
			}

			double finalDamage = damage;
			// Intentionally apply effects after damage.
			mIframeTracker.damage(mob, () -> {
				DamageUtils.damage(
					mPlayer,
					mob,
					new DamageEvent.Metadata(
						DamageType.MAGIC,
						mInfo.getLinkedSpell(),
						playerItemStats
					),
					finalDamage,
					true,
					true,
					false
				);
				// Brutal DoT-stacking effects need to be Iframe-capped.
				if (!isGruesome) {
					applyEffects(mob, false, playerItemStats, true);
				}
			});
			// Gruesome effects don't need to be Iframe-capped.
			// Also, allow Brutal to trigger abilities like Volatile at any time.
			applyEffects(mob, isGruesome, playerItemStats, false);
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
		if (isGruesome && mGruesomeAlchemy != null) {
			applyEffects(mob, true, playerItemStats, mGruesomeAlchemy.getLevel(), true);
		} else if (!isGruesome && mBrutalAlchemy != null) {
			applyEffects(mob, false, playerItemStats, mBrutalAlchemy.getLevel(), true);
		}
	}

	public void applyEffects(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, boolean refreshBrutalDot) {
		if (isGruesome && mGruesomeAlchemy != null) {
			applyEffects(mob, true, playerItemStats, mGruesomeAlchemy.getLevel(), refreshBrutalDot);
		} else if (!isGruesome && mBrutalAlchemy != null) {
			applyEffects(mob, false, playerItemStats, mBrutalAlchemy.getLevel(), refreshBrutalDot);
		}
	}

	public void applyEffects(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level) {
		applyEffects(mob, isGruesome, playerItemStats, level, true);
	}

	public void applyEffects(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level, boolean refreshBrutalDot) {
		// Apply potions effects but no damage
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob, isGruesome, playerItemStats, level, refreshBrutalDot);
		}
	}

	/**
	 * Returns true if it has changed, and false if it has not.
	 */
	private boolean checkAlchemicalUtensilChanged() {
		// Check if the player has an alchemical utensil on the hotbar
		PlayerInventory inventory = mPlayer.getInventory();
		if (ItemUtils.isAlchemistItem(inventory.getItemInMainHand())) {
			if (inventory.getHeldItemSlot() != mSlot) {
				mSlot = inventory.getHeldItemSlot();
				mCharges = 0;
				mRechargeRateCoefficient = getRechargeRate(inventory.getItemInMainHand());
				return true;
			} else {
				ItemStack itemInMainhand = inventory.getItemInMainHand();

				if (!ItemUtils.equalIgnoringDurability(itemInMainhand, mItemInSlot)) {
					mItemInSlot = itemInMainhand;
					mCharges = 0;
					mRechargeRateCoefficient = getRechargeRate(itemInMainhand);
					return true;
				}
				return false;
			}
		}

		ItemStack itemInSlot = inventory.getItem(mSlot);
		if (itemInSlot != null && ItemUtils.isAlchemistItem(itemInSlot)) {
			if (mItemInSlot == null || !ItemUtils.equalIgnoringDurability(itemInSlot, mItemInSlot)) {
				mItemInSlot = itemInSlot;
				mCharges = 0;
				mRechargeRateCoefficient = getRechargeRate(itemInSlot);
				return true;
			}
			return false;
		}

		// Not found in mSlot either, so must look for it anew.
		boolean found = false;
		for (int i = 0; i < 9; i++) {
			ItemStack itemInHotbar = inventory.getItem(i);
			if (ItemUtils.isAlchemistItem(itemInHotbar)) {
				mSlot = i;
				mItemInSlot = itemInHotbar;
				mCharges = 0;
				mRechargeRateCoefficient = getRechargeRate(itemInHotbar);
				found = true;
				break;
			}
		}

		return found;
	}

	public boolean decrementCharge() {
		if (mCharges <= 0) {
			return false;
		}

		if (checkAlchemicalUtensilChanged()) {
			return false;
		}

		mCharges--;
		ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
		PlayerUtils.callAbilityCastEvent(mPlayer, this, ClassAbility.ALCHEMIST_POTION, 0);
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

	public boolean decrementCharges(double charges) {
		int intCharges = (int) charges;
		double doubleCharges = charges - intCharges;
		double doubleChargesTimer = doubleCharges * mChargeTime;

		if (mCharges < intCharges) {
			// Not enough potions to pay the integer price
			return false;
		}

		// mCharges >= intCharges
		if (mCharges == intCharges && mTimer < doubleChargesTimer) {
			return false;
		}

		// mCharges >= intCharges
		if (mTimer >= doubleChargesTimer) {
			for (int i = 0; i < intCharges; i++) {
				if (!decrementCharge()) {
					// Enough charges but cannot remove for other reason such as no bag
					return false;
				}
			}
			mTimer -= doubleChargesTimer;
			return true;
		}

		// mCharges > intCharges, mTimer < doubleChargesTimer
		for (int i = 0; i < intCharges + 1; i++) {
			if (!decrementCharge()) {
				// Enough charges but cannot remove for other reason such as no bag
				return false;
			}
		}
		mTimer += (1 - doubleCharges) * mChargeTime;
		return true;
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
		// Check if the utensil has changed, in which case we immediately use
		// the new utensil's recharge rate stat.
		if (checkAlchemicalUtensilChanged()) {
			mOnCooldown = true;
			mTimer = 0;
			updateAlchemistItem();
		}

		if (mOnCooldown) {
			double coefficient = calculateRechargeRateCoefficient();
			mTimer += 5 * coefficient;
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

	public void addRechargeRateMultiplier(String source, double multiplier) {
		mRechargeRateMultipliers.put(source, multiplier);
	}

	public void removeRechargeRateMultiplier(String source) {
		mRechargeRateMultipliers.remove(source);
	}

	private double calculateRechargeRateCoefficient() {
		return mRechargeRateMultipliers.values().stream().reduce(mRechargeRateCoefficient, (acc, curr) -> acc * curr);
	}

	public void swapMode() {
		boolean gruesome = ScoreboardUtils.toggleTag(mPlayer, GRUESOME_MODE_TAG);
		String mode = gruesome ? "Gruesome" : "Brutal";

		sendActionBarMessage("Alchemist's Potions swapped to " + mode + " mode");
		updateAlchemistItem();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public boolean isGruesomeMode() {
		return mPlayer.getScoreboardTags().contains(GRUESOME_MODE_TAG);
	}

	public boolean isGruesome(ThrownPotion potion) {
		return potion.hasMetadata(GRUESOME_POTION_METAKEY);
	}

	public void increaseChargeTime(int ticks) {
		mChargeTime += ticks;
	}

	public void reduceChargeTime(int ticks) {
		mChargeTime -= ticks;
	}

	public int getChargeTime() {
		return mChargeTime;
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

	public double getRechargeRate(ItemStack itemStack) {
		double baseRate = NBT.get(itemStack, nbt -> {
			ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);
			return ItemStatUtils.getAttributeAmount(attributes, AttributeType.POTION_RECHARGE_RATE, Operation.MULTIPLY, Slot.MAINHAND);
		});
		return CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RECHARGE_RATE, baseRate);
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

		// Also send an update to all nearby players if an alchemist's potion is held
		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
			for (Player otherPlayer : mPlayer.getTrackedBy()) {
				otherPlayer.sendEquipmentChange(mPlayer, EquipmentSlot.HAND, mPlayer.getInventory().getItemInMainHand());
			}
		}
	}

	@Override
	public @Nullable String getMode() {
		return isGruesomeMode() ? "gruesome" : null;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("AP", isGruesomeMode() ? NamedTextColor.RED : TextColor.color(0, 255, 0)))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	public static Description<AlchemistPotions> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Allows using Alchemical Utensils.")
			.addLine("You gain *1* potion every %t, up to").styles(WHITE)
				.statValues(stat(POTIONS_TIMER_BASE))
			.addLine("a maximum of %d potions.")
				.statValues(stat(a -> a.mMaxCharges, MAX_CHARGES))
			.addLine()
			.addLine("*Right Button* to throw *Brutal* potions").styles(TRIGGER_TEXT, Alchemist.BRUTAL_COLOR)
			.addLine("that deal magic damage (s) in an area.")
			.addLine()
			.addLine("*Left Button* to throw *Gruesome* potions").styles(TRIGGER_TEXT, Alchemist.GRUESOME_COLOR)
			.addLine("that deal %p of regular damage, but ")
				.statValues(stat(a -> a.mGruesomeDamageMultiplier, GruesomeAlchemy.GRUESOME_POTION_DAMAGE_MULTIPLIER))
			.addLine("inflict %p *Slowness*, %p *Vulnerability*,").styles(WHITE, WHITE)
				.statValues(stat(a -> a.mLevelZeroSlownessAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_0_SLOWNESS_AMPLIFIER),
					stat(a -> a.mLevelZeroVulnerabilityAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_0_VULNERABILITY_AMPLIFIER))
			.addLine("and %p *Weakness* to mobs for %t.").styles(WHITE)
				.statValues(stat(a -> a.mLevelZeroWeakenAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_0_WEAKEN_AMPLIFIER),
					stat(a -> a.mLevelZeroDuration, GruesomeAlchemy.GRUESOME_ALCHEMY_DURATION));
	}
}
