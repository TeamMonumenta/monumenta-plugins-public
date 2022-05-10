package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Handles giving potions and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements AbilityWithChargesOrStacks {

	private static final int MAX_CHARGES = 8;
	private static final int POTIONS_TIMER_BASE = 2 * 20;
	private static final int POTIONS_TIMER_TOWN = 1 * 20;

	private static final int IFRAME_BETWEEN_POT = 10;
	private static final double DAMAGE_PER_SKILL_POINT = 0.5;
	private static final double DAMAGE_PER_SPEC_POINT = 2.5;
	private static final String POTION_SCOREBOARD = "StoredPotions";
	private static final double RADIUS = 4;


	private List<PotionAbility> mPotionAbilities = new ArrayList<PotionAbility>();
	private double mDamage = 0;
	private int mTimer = 0;
	private int mSlot;
	private int mMaxCharges;
	private int mCharges;
	private int mChargeTime;
	private WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;
	private Map<UUID, Integer> mMobsIframeMap;

	private boolean mGruesomeMode;

	private static @Nullable ItemStack GRUESOME_POTION = null;
	private static @Nullable ItemStack BRUTAL_POTION = null;

	public AlchemistPotions(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
		mInfo.mLinkedSpell = ClassAbility.ALCHEMIST_POTION;

		if (player == null) {
			/* This is a reference ability, not one actually tied to a player */
			return;
		}

		/*
		 * Run this stuff 5 ticks later. As of now, the AbilityManager takes a tick
		 * to initialize everything, and the PotionAbility classes take a tick to
		 * initialize their damage values, but just give a few extra ticks for slight
		 * future-proofing.
		 */
		mCharges = ScoreboardUtils.getScoreboardValue(player, POTION_SCOREBOARD).orElse(0);
		mChargeTime = POTIONS_TIMER_BASE;
		mMaxCharges = MAX_CHARGES;

		mPlayerItemStatsMap = new WeakHashMap<>();
		mMobsIframeMap = new HashMap<>();

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
			}
		}.runTaskLater(mPlugin, 5);

		// Always switch back to the default mode when refreshing class
		mGruesomeMode = false;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 5;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && ItemUtils.isAlchemistItem(potion.getItem())) {
			if (decrementCharge()) {
				setPotionToAlchemistPotion(potion);
			} else {
				potion.remove();
			}
		}

		return true;
	}

	public void setPotionToAlchemistPotion(ThrownPotion potion) {
		if (mPlayer == null) {
			return;
		}

		mPlayerItemStatsMap.put(potion, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		if (mGruesomeMode) {
			potion.setMetadata("GruesomeAlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}

		if (BRUTAL_POTION == null || GRUESOME_POTION == null) {
			ItemStack basePotion = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString("epic:r1/items/alchemists_potion"));
			if (basePotion == null) {
				mPlugin.getLogger().severe("Failed to get alchemist's potion from loot table!");
				return;
			}

			BRUTAL_POTION = basePotion.clone();
			PotionMeta meta = (PotionMeta) BRUTAL_POTION.getItemMeta();
			meta.setColor(Color.BLACK);
			BRUTAL_POTION.setItemMeta(meta);

			GRUESOME_POTION = basePotion.clone();
			PotionMeta meta2 = (PotionMeta) GRUESOME_POTION.getItemMeta();
			meta2.setColor(Color.FUCHSIA);
			GRUESOME_POTION.setItemMeta(meta2);
		}

		if (mGruesomeMode && GRUESOME_POTION != null) {
			potion.setItem(GRUESOME_POTION);
		} else if (!mGruesomeMode && BRUTAL_POTION != null) {
			potion.setItem(BRUTAL_POTION);
		} else {
			mPlugin.getLogger().severe("Failed to get alchemist's potion from loot table!");
		}
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(potion);
		if (playerItemStats != null) {
			Location loc = potion.getLocation();

			createAura(loc);

			boolean isGruesome = isGruesome(potion);

			for (LivingEntity entity : EntityUtils.getNearbyMobs(loc, RADIUS)) {
				if (EntityUtils.isHostileMob(entity)) {
					apply(entity, potion, isGruesome, playerItemStats);
				}

				if (entity instanceof Player player && player != mPlayer) {
					applyToPlayer(player, potion, isGruesome);
				}
			}
		}

		return true;
	}

	public void createAura(Location loc) {
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.createAura(loc);
		}
	}

	public void createAura(Location loc, double radius) {
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.createAura(loc, radius);
		}
	}

	public void apply(LivingEntity mob, ThrownPotion potion, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mPlayer != null && MetadataUtils.checkOnceThisTick(mPlugin, mob, "AlchemistPotionApplying") && !mob.isDead()) {
			double damage = mDamage;

			if (isGruesome) {
				damage *= GruesomeAlchemy.GRUESOME_POTION_DAMAGE_MULTIPLIER;
			}

			if (potion.hasMetadata(AlchemicalArtillery.ARTILLERY_POTION_TAG)) {
				damage += potion.getMetadata(AlchemicalArtillery.ARTILLERY_POTION_TAG).get(0).asDouble();
			}

			mMobsIframeMap.values().removeIf(tick -> tick + IFRAME_BETWEEN_POT < mPlayer.getTicksLived());

			if (mMobsIframeMap.containsKey(mob.getUniqueId())) {
				applyEffects(mob, isGruesome);
				return;
			}

			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), damage, true, true, false);
			mMobsIframeMap.put(mob.getUniqueId(), mPlayer.getTicksLived());

			// Intentionally apply effects after damage
			applyEffects(mob, isGruesome);
		}
	}

	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {
		if (mPlayer != null && MetadataUtils.checkOnceThisTick(mPlugin, player, "AlchemistPotionApplying")) {
			for (PotionAbility potionAbility : mPotionAbilities) {
				potionAbility.applyToPlayer(player, potion, isGruesome);
			}
		}
	}

	public void applyEffects(LivingEntity mob, boolean isGruesome) {
		//Apply potions effects but no damage
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob, isGruesome);
		}
	}

	public boolean decrementCharge() {
		if (mPlayer != null && mCharges > 0) {
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

	public boolean incrementCharge() {
		if (mPlayer != null && mCharges < mMaxCharges) {
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

	private boolean mOnCooldown = false;

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (twoHertz && mPlayer != null) {
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

	public void swapMode() {
		if (mPlayer == null) {
			return;
		}

		mGruesomeMode = !mGruesomeMode;
		String mode = "";
		if (mGruesomeMode) {
			mode = "Gruesome";
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1.25f);
		} else {
			mode = "Brutal";
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0.75f);
		}

		mPlayer.sendActionBar(ChatColor.YELLOW + "Alchemist's Potions swapped to " + mode + " mode");
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.9f, 1);
		updateAlchemistItem(mPlayer.getInventory().getItem(mSlot), mCharges);
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
}
