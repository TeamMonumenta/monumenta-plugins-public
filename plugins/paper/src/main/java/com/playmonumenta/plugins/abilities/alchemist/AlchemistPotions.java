package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeAlchemyCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/*
 * Handles giving potions and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements AbilityWithChargesOrStacks {

	public static final String METADATA_KEY = "PlayerThrowAlchPotionEvent";
	public static final int MAX_CHARGES = 8;
	public static final int POTIONS_TIMER_BASE = 2 * 20;
	private static final int POTIONS_TIMER_TOWN = 1 * 20;

	private static final int IFRAME_BETWEEN_POT = 10;
	public static final double DAMAGE_PER_SKILL_POINT = 0.5;
	public static final double DAMAGE_PER_SPEC_POINT = 2;
	// Alchemist.java assumes that these two constants are equal - update the description there if you change these values to be different.
	public static final double DAMAGE_PER_ENHANCEMENT = 1.5;
	private static final String POTION_SCOREBOARD = "StoredPotions";
	private static final double RADIUS = 4;

	public static final String CHARM_CHARGES = "Alchemist Potion Charges";
	public static final String CHARM_DAMAGE = "Alchemist Potion Damage";
	public static final String CHARM_RADIUS = "Alchemist Potion Radius";

	private final List<PotionAbility> mPotionAbilities = new ArrayList<>();
	private double mDamage = 0;
	private double mRadius = 0;
	private int mTimer = 0;
	private int mSlot;
	private final int mMaxCharges;
	private int mCharges;
	private int mChargeTime;
	private final WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;
	private final Map<UUID, Integer> mMobsIframeMap;

	private boolean mGruesomeMode;

	private static @Nullable ItemStack GRUESOME_POTION = null;
	private static @Nullable ItemStack BRUTAL_POTION = null;

	public static final AbilityInfo<AlchemistPotions> INFO =
		new AbilityInfo<>(AlchemistPotions.class, null, AlchemistPotions::new)
			.linkedSpell(ClassAbility.ALCHEMIST_POTION)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Alchemist.CLASS_ID);

	private final GruesomeAlchemyCS mCosmetic;

	private @Nullable ProjectileHitEvent mLastHitEvent;

	public AlchemistPotions(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		/*
		 * Run this stuff 5 ticks later. As of now, the AbilityManager takes a tick
		 * to initialize everything, and the PotionAbility classes take a tick to
		 * initialize their damage values, but just give a few extra ticks for slight
		 * future-proofing.
		 */
		mChargeTime = POTIONS_TIMER_BASE;
		mMaxCharges = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CHARGES, MAX_CHARGES);
		mCharges = Math.min(ScoreboardUtils.getScoreboardValue(player, POTION_SCOREBOARD).orElse(0), mMaxCharges);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);

		mPlayerItemStatsMap = new WeakHashMap<>();
		mMobsIframeMap = new HashMap<>();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS(), GruesomeAlchemyCS.SKIN_LIST);

		// Scan hotbar for alch potion
		PlayerInventory inv = player.getInventory();
		mSlot = 0;
		for (int i = 0; i < 9; i++) {
			if (ItemUtils.isAlchemistItem(inv.getItem(i))) {
				mSlot = i;
				break;
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				Ability[] classAbilities = new Ability[8];
				Ability[] specAbilities = new Ability[6];
				classAbilities[0] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				classAbilities[1] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
				classAbilities[3] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, EmpoweringOdor.class);
				classAbilities[2] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, IronTincture.class);
				classAbilities[4] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemicalArtillery.class);
				classAbilities[5] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, UnstableAmalgam.class);
				classAbilities[6] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, EnergizingElixir.class);
				classAbilities[7] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Bezoar.class);
				specAbilities[0] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Taboo.class);
				specAbilities[1] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, EsotericEnhancements.class);
				specAbilities[2] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, ScorchedEarth.class);
				specAbilities[3] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, WardingRemedy.class);
				specAbilities[4] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, TransmutationRing.class);
				specAbilities[5] = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Panacea.class);

				for (Ability classAbility : classAbilities) {
					if (classAbility != null) {
						int abilityScore = classAbility.isLevelTwo() ? 2 : 1;
						mDamage += DAMAGE_PER_SKILL_POINT * abilityScore;

						if (ServerProperties.getAbilityEnhancementsEnabled() && classAbility.isEnhanced()) {
							mDamage += DAMAGE_PER_ENHANCEMENT;
						}

						if (classAbility instanceof PotionAbility potionAbility) {
							mPotionAbilities.add(potionAbility);
							mDamage += potionAbility.getDamage();
						}

						if (classAbility instanceof EmpoweringOdor odor && odor.isLevelTwo()) {
							mChargeTime -= EmpoweringOdor.POTION_RECHARGE_TIME_REDUCTION_2;
						}
					}
				}

				if (ServerProperties.getClassSpecializationsEnabled()) {
					for (Ability specAbility : specAbilities) {
						if (specAbility != null) {
							int abilityScore = specAbility.isLevelTwo() ? 2 : 1;
							mDamage += DAMAGE_PER_SPEC_POINT * abilityScore;

							if (specAbility instanceof PotionAbility potionAbility) {
								mPotionAbilities.add(potionAbility);
								mDamage += potionAbility.getDamage();
							}
						}
					}
				}

				mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamage);
			}
		}.runTaskLater(mPlugin, 5);

		// Always switch back to the default mode when refreshing class
		mGruesomeMode = false;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && ItemUtils.isAlchemistItem(potion.getItem())) {
			mPlayer.setMetadata(METADATA_KEY, new FixedMetadataValue(mPlugin, mPlayer.getTicksLived()));
			if (decrementCharge()) {
				setPotionToAlchemistPotion(potion);
			} else {
				potion.remove();
			}
		}

		return true;
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, with also the damage and if it's gruesome or brutal mode
	 */
	public void setPotionToAlchemistPotion(ThrownPotion potion) {
		mPlayerItemStatsMap.put(potion, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		if (mGruesomeMode) {
			potion.setMetadata("GruesomeAlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}

		setPotionAlchemistPotionAesthetic(potion);
	}

	/**
	 * This function will set the given ThrownPotion potion to an Alchemist Potion, ONLY to an aesthetic level
	 */
	public void setPotionAlchemistPotionAesthetic(ThrownPotion potion) {
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
		if (mGruesomeMode) {
			item = GRUESOME_POTION.clone();
		} else {
			item = BRUTAL_POTION.clone();
		}
		item.editMeta(m -> {
			((PotionMeta) m).setColor(mCosmetic.splashColor(mGruesomeMode));
		});
		potion.setItem(item);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		// Prevent the potion from splashing on players, summons, and other non-hostile mobs
		// Most importantly, this prevents the potion from instantly splashing on the throwing player with certain combinations of projectile speed and throw direction when using Alchemical Artillery.
		if (proj instanceof ThrownPotion potion
			    && mPlayerItemStatsMap.containsKey(potion)
			    && event.getHitEntity() != null
			    && !EntityUtils.isHostileMob(event.getHitEntity())) {
			event.setCancelled(true);
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

			createAura(loc, potion, playerItemStats);

			boolean isGruesome = isGruesome(potion);

			double radius = getPotionRadius();
			Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
			for (LivingEntity entity : hitbox.getHitMobs()) {
				apply(entity, potion, isGruesome, playerItemStats);
			}

			mCosmetic.particlesOnSplash(mPlayer, loc, isGruesome);

			List<Player> players = PlayerUtils.playersInRange(loc, radius, true);
			players.remove(mPlayer);
			players.forEach(player -> applyToPlayer(player, potion, isGruesome));
		}

		return true;
	}

	public void createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.createAura(loc, potion, playerItemStats);
		}
	}

	public void apply(LivingEntity mob, ThrownPotion potion, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mob, "AlchemistPotionApplying") && !mob.isDead()) {
			double damage = mDamage;

			if (isGruesome) {
				damage *= GruesomeAlchemy.GRUESOME_POTION_DAMAGE_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_DAMAGE);
			}

			if (potion.hasMetadata(AlchemicalArtillery.ARTILLERY_POTION_TAG)) {
				damage += potion.getMetadata(AlchemicalArtillery.ARTILLERY_POTION_TAG).get(0).asDouble();
			}

			mMobsIframeMap.values().removeIf(tick -> tick + IFRAME_BETWEEN_POT < Bukkit.getServer().getCurrentTick());

			if (mMobsIframeMap.containsKey(mob.getUniqueId())) {
				applyEffects(mob, isGruesome, playerItemStats);
				return;
			}

			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);
			mMobsIframeMap.put(mob.getUniqueId(), Bukkit.getServer().getCurrentTick());

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
		//Apply potions effects but no damage
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob, isGruesome, playerItemStats);
		}
	}

	public boolean decrementCharge() {
		if (mCharges > 0) {
			mCharges--;
			ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
			PlayerInventory inventory = mPlayer.getInventory();

			PlayerUtils.callAbilityCastEvent(mPlayer, ClassAbility.ALCHEMIST_POTION);

			if (ItemUtils.isAlchemistItem(inventory.getItemInMainHand())) {
				updateAlchemistItem(inventory.getItemInMainHand(), mCharges);
				mSlot = inventory.getHeldItemSlot();
			} else if (ItemUtils.isAlchemistItem(inventory.getItem(mSlot))) {
				updateAlchemistItem(inventory.getItem(mSlot), mCharges);
			} else {
				boolean found = false;
				for (int i = 0; i < 9; i++) {
					if (ItemUtils.isAlchemistItem(inventory.getItem(i))) {
						mSlot = i;
						updateAlchemistItem(inventory.getItem(i), mCharges);
						found = true;
						break;
					}
				}
				if (!found) {
					// Cannot find alch bag
					return false;
				}
			}


			ClientModHandler.updateAbility(mPlayer, this);

			return true;
		}
		return false;
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
			//update item
			ItemStack item = mPlayer.getInventory().getItem(mSlot);
			if (item != null) {
				updateAlchemistItem(item, mCharges);
			}
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
		if (twoHertz) {
			ItemStack item = mPlayer.getInventory().getItem(mSlot);

			if (mOnCooldown) {
				mTimer += 10;
				if (mTimer >= mChargeTime || (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.RESIST_5) && mTimer >= POTIONS_TIMER_TOWN)) {
					mTimer = 0;
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
				//update item

				if (item != null) {
					updateAlchemistItem(item, mCharges);
				}
				ClientModHandler.updateAbility(mPlayer, this);
			}

			if (item != null && item.getAmount() > 1 && ItemUtils.isAlchemistItem(item)) {
				item.setAmount(1);
			}
		}
	}

	public void swapMode(float brewPitch) {
		mGruesomeMode = !mGruesomeMode;
		String mode = mGruesomeMode ? "Gruesome" : "Brutal";

		mPlayer.sendActionBar(ChatColor.YELLOW + "Alchemist's Potions swapped to " + mode + " mode");
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.9f, brewPitch);
		updateAlchemistItem(mPlayer.getInventory().getItem(mSlot), mCharges);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public boolean isGruesomeMode() {
		return mGruesomeMode;
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

	public double getDamage() {
		return mDamage;
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	public double getPotionRadius() {
		return mRadius;
	}

	private boolean updateAlchemistItem(@Nullable ItemStack item, int count) {
		if (item == null) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();

		if (item.getType() == Material.SPLASH_POTION && meta instanceof PotionMeta potionMeta) {
			double ratio = ((double) count) / mMaxCharges;
			int color = (int) (ratio * 255);
			if (mGruesomeMode) {
				potionMeta.setColor(Color.fromRGB(color, 0, 0));
			} else {
				potionMeta.setColor(Color.fromRGB(0, color, 0));
			}
			item.setItemMeta(potionMeta);
		}

		if (meta.hasDisplayName() && meta.getDisplayName().contains("Alchemist's Bag")) {
			meta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Alchemist's Bag (" + count + ")");
			item.setItemMeta(meta);
			ItemUtils.setPlainTag(item);
			return true;
		}

		return false;
	}

	@Override
	public @Nullable String getMode() {
		return mGruesomeMode ? "gruesome" : null;
	}

	public boolean isAlchemistPotion(ThrownPotion potion) {
		return BRUTAL_POTION != null && ItemUtils.getPlainNameIfExists(BRUTAL_POTION).equals(ItemUtils.getPlainNameIfExists(potion.getItem()));
	}
}
