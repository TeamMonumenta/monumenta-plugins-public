package com.playmonumenta.plugins.abilities.alchemist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.AlchemicalAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.PurpleHaze;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

/*
 * Handles giving potions on kills and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements AbilityWithChargesOrStacks {

	public static class AlchemistPotionsDamageEnchantment extends BaseAbilityEnchantment {
		public AlchemistPotionsDamageEnchantment() {
			super("Alchemist Potion Damage", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final int MAX_CHARGE_POTIONS = 6;
	private static final int POTIONS_TIMER_BASE = 2 * 20;
	private static final int POTION_TIMER_HARB = (int) (1.5 * 20);
	private static final int POTIONS_TIMER_TOWN = 1 * 20;

	private static final double DAMAGE_PER_SKILL_POINT = 0.5;
	private static final double DAMAGE_PER_SPEC_POINT = 1;
	private static final String POTION_SCOREBOARD = "StoredPotions";

	public static final String POTION_METADATA_PLAYER_NAME = "HitByAlchemist";

	private List<PotionAbility> mPotionAbilities = new ArrayList<PotionAbility>();
	private double mDamage = 0;
	private int mTimer = 0;
	private int mSlot = 0;
	private int mCharges;
	private int mChargeTime;

	private static ItemStack POTION = null;

	public AlchemistPotions(Plugin plugin, Player player) {
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
		if (player != null) {
			mCharges = ScoreboardUtils.getScoreboardValue(player, POTION_SCOREBOARD);
			mChargeTime = POTIONS_TIMER_BASE;
			new BukkitRunnable() {
				@Override
				public void run() {
					Ability[] classAbilities = new Ability[8];
					Ability[] specializationAbilities = new Ability[6];
					classAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class);
					classAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
					classAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, IronTincture.class);
					classAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);
					classAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, PowerInjection.class);
					classAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, UnstableArrows.class);
					classAbilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, EnfeeblingElixir.class);
					classAbilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, Bezoar.class);
					specializationAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, PurpleHaze.class);
					specializationAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, NightmarishAlchemy.class);
					specializationAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, ScorchedEarth.class);
					specializationAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, WardingRemedy.class);
					specializationAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, InvigoratingOdor.class);
					specializationAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemicalAmalgam.class);

					for (Ability classAbility : classAbilities) {
						if (classAbility != null) {
							mDamage += DAMAGE_PER_SKILL_POINT * classAbility.getAbilityScore();

							if (classAbility instanceof PotionAbility) {
								PotionAbility potionAbility = (PotionAbility) classAbility;
								mPotionAbilities.add(potionAbility);
								mDamage += potionAbility.getDamage();
							}
						}
					}

					for (Ability specializationAbility : specializationAbilities) {
						if (specializationAbility != null) {
							mDamage += DAMAGE_PER_SPEC_POINT * specializationAbility.getAbilityScore();

							if (specializationAbility instanceof PotionAbility) {
								PotionAbility potionAbility = (PotionAbility) specializationAbility;
								mPotionAbilities.add(potionAbility);
								mDamage += potionAbility.getDamage();
							}

							if (specializationAbility instanceof NightmarishAlchemy) {
								mChargeTime = POTION_TIMER_HARB;
							}
						}
					}

				}
			}.runTaskLater(mPlugin, 5);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 5;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && ItemUtils.isAlchemistItem(potion.getItem())) {
			if (consumeCharge()) {
				potion.setMetadata("AlchemistPotion", new FixedMetadataValue(mPlugin, 0));

				if (POTION == null) {
					POTION = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r1/items/alchemists_potion"));
				}
				if (POTION != null) {
					potion.setItem(POTION);
				} else {
					mPlugin.getLogger().severe("Failed to get alchemist's potion from loot table!");
				}
			} else {
				potion.remove();
			}
		}

		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			createAura(potion.getLocation());

			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						apply(entity);
					}
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

	public void apply(LivingEntity mob) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mob, "AlchemistPotionApplying")) {
			if (!mob.hasMetadata(POTION_METADATA_PLAYER_NAME)) {
				mob.setMetadata(POTION_METADATA_PLAYER_NAME, new FixedMetadataValue(mPlugin, new HashSet<String>()));
			}

			Object obj = mob.getMetadata(POTION_METADATA_PLAYER_NAME).get(0).value();
			if (obj instanceof HashSet) {
				((HashSet<String>) obj).add(mPlayer.getName());
			}

			// Apply effects first so stuff like Vulnerability properly stacks
			for (PotionAbility potionAbility : mPotionAbilities) {
				potionAbility.apply(mob);
			}
			double damage = getDamage();
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ALCHEMY, true, mInfo.mLinkedSpell);
		}
	}

	public void applyEffects(LivingEntity mob) {
		//Apply potions effects but no damage
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob);
		}
	}

	public double getDamage() {
		return mDamage + AlchemistPotionsDamageEnchantment.getExtraDamage(mPlayer, AlchemistPotionsDamageEnchantment.class);
	}

	public boolean consumeCharge() {
		if (mCharges > 0) {
			mCharges--;
			ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
			PlayerInventory inventory = mPlayer.getInventory();

			PlayerUtils.callAbilityCastEvent(mPlayer, mInfo.mLinkedSpell);

			AbilityUtils.updateAlchemistItem(inventory.getItemInMainHand(), mCharges);
			mSlot = inventory.getHeldItemSlot();

			ClientModHandler.updateAbility(mPlayer, this);

			return true;
		}
		return false;
	}

	public boolean incrementCharge() {
		if (mCharges < MAX_CHARGE_POTIONS) {
			mCharges++;
			ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
			//update item
			ItemStack item = mPlayer.getInventory().getItem(mSlot);
			if (item != null) {
				AbilityUtils.updateAlchemistItem(item, mCharges);
			}
			ClientModHandler.updateAbility(mPlayer, this);
			return true;
		}
		return false;
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

			if (mCharges < MAX_CHARGE_POTIONS) {
				mOnCooldown = true;
			}

			if (mCharges > MAX_CHARGE_POTIONS) {
				mCharges = MAX_CHARGE_POTIONS;
				ScoreboardUtils.setScoreboardValue(mPlayer, POTION_SCOREBOARD, mCharges);
				//update item

				if (item != null) {
					AbilityUtils.updateAlchemistItem(item, mCharges);
				}
			}

			if (item != null && item.getAmount() > 1 && ItemUtils.isAlchemistItem(item)) {
				item.setAmount(1);
			}
		}
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return MAX_CHARGE_POTIONS;
	}

}
