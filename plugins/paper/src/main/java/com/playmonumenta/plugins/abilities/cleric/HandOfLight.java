package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HandOfLightCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;



public class HandOfLight extends Ability {

	public static final int HEALING_RADIUS = 12;
	public static final int DAMAGE_RADIUS = 6;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int HEALING_1_COOLDOWN = 14 * 20;
	private static final int HEALING_2_COOLDOWN = 10 * 20;
	private static final int FLAT_1 = 2;
	private static final int FLAT_2 = 4;
	private static final double PERCENT_1 = 0.1;
	private static final double PERCENT_2 = 0.2;
	private static final int DAMAGE_PER_1 = 2;
	private static final int DAMAGE_PER_2 = 3;
	private static final int DAMAGE_MAX_1 = 8;
	private static final int DAMAGE_MAX_2 = 9;
	public static final String DAMAGE_MODE_TAG = "ClericHOLDamageMode";

	private int mFlat;
	private double mPercent;
	private int mDamagePer;
	private int mDamageMax;
	private boolean mDamageMode;

	private @Nullable Crusade mCrusade;
	private boolean mHasCleansingRain;
	private boolean mHasLuminousInfusion;

	private final HandOfLightCS mCosmetic;

	public HandOfLight(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Hand of Light");
		mInfo.mLinkedSpell = ClassAbility.HAND_OF_LIGHT;
		mInfo.mScoreboardId = "Healing";
		mInfo.mShorthandName = "HoL";
		mInfo.mDescriptions.add("Right click while holding a weapon or tool to heal all other players in a 12 block range in front of you or within 2 blocks of you for 2 hearts + 10% of their max health and gives them regen 2 for 4 seconds. If holding a shield, the trigger is changed to crouch + right click. Additionally, swap hands while looking up and not sneaking to change to damage mode. In damage mode, instead of healing players, damage all mobs in a 6 block radius in front of you magic damage equal to 2 times the number of undead mobs in the range, up to 8 damage. Cooldown: 14s.");
		mInfo.mDescriptions.add("The healing is improved to 4 hearts + 20% of their max health. In damage mode, deal 3 damage per undead mob, up to 9 damage. Cooldown: 10s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? HEALING_1_COOLDOWN : HEALING_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.PINK_DYE, 1);
		mInfo.mIgnoreCooldown = true;

		mFlat = getAbilityScore() == 1 ? FLAT_1 : FLAT_2;
		mPercent = getAbilityScore() == 1 ? PERCENT_1 : PERCENT_2;
		mDamagePer = getAbilityScore() == 1 ? DAMAGE_PER_1 : DAMAGE_PER_2;
		mDamageMax = getAbilityScore() == 1 ? DAMAGE_MAX_1 : DAMAGE_MAX_2;

		mDamageMode = player != null && player.getScoreboardTags().contains(DAMAGE_MODE_TAG);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HandOfLightCS(), HandOfLightCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);

			mHasCleansingRain = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, CleansingRain.class) != null;
			mHasLuminousInfusion = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, LuminousInfusion.class) != null;
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}

		if (isTimerActive() || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		// If holding a shield, must be sneaking to activate
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offhand = mPlayer.getInventory().getItemInOffHand();
		if (!mPlayer.isSneaking() && (offhand.getType() == Material.SHIELD || mainhand.getType() == Material.SHIELD)) {
			return;
		}

		// Must not match conditions for cleansing rain
		if (mHasCleansingRain && mPlayer.getLocation().getPitch() <= -50) {
			return;
		}

		// Must not match conditions for luminous infusion
		if (mHasLuminousInfusion && mPlayer.getLocation().getPitch() >= 50) {
			return;
		}

		//Cannot be cast with multitool.
		if (mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.MULTITOOL) > 0) {
			return;
		}

		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		World world = mPlayer.getWorld();
		Location userLoc = mPlayer.getLocation();

		if (!mDamageMode) {
			List<Player> nearbyPlayers = PlayerUtils.otherPlayersInRange(mPlayer, HEALING_RADIUS, true);
			nearbyPlayers.removeIf(p -> (playerDir.dot(p.getLocation().toVector().subtract(userLoc.toVector()).setY(0).normalize()) < HEALING_DOT_ANGLE && p.getLocation().distance(userLoc) > 2) || p.getScoreboardTags().contains("disable_class"));
			if (nearbyPlayers.size() > 0) {
				for (Player p : nearbyPlayers) {
					double maxHealth = EntityUtils.getMaxHealth(p);
					PlayerUtils.healPlayer(mPlugin, p, mFlat + mPercent * maxHealth, mPlayer);

					Location loc = p.getLocation();
					mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, 20 * 4, 1, true, true));
					mCosmetic.lightHealEffect(mPlayer, loc, p);
				}

				mCosmetic.lightHealCastEffect(world, userLoc, mPlugin, mPlayer, HEALING_RADIUS, HEALING_DOT_ANGLE);
				putOnCooldown();
			}
		} else {
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(userLoc, DAMAGE_RADIUS);
			nearbyMobs.removeIf(mob -> (playerDir.dot(mob.getLocation().toVector().subtract(userLoc.toVector()).setY(0).normalize()) < HEALING_DOT_ANGLE && mob.getLocation().distance(userLoc) > 2) || mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

			List<LivingEntity> undeadMobs = new ArrayList<>(nearbyMobs);
			undeadMobs.removeIf(mob -> !Crusade.enemyTriggersAbilities(mob, mCrusade));
			int damage = Math.min(undeadMobs.size() * mDamagePer, mDamageMax);

			if (damage > 0) {
				for (LivingEntity mob : nearbyMobs) {
					DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, true);

					Location loc = mob.getLocation();
					mCosmetic.lightDamageEffect(mPlayer, loc, mob);
				}
				mCosmetic.lightDamageCastEffect(world, userLoc, mPlugin, mPlayer, DAMAGE_RADIUS, HEALING_DOT_ANGLE);
				putOnCooldown();
			}
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || mPlayer.isSneaking() || mPlayer.getLocation().getPitch() > -45) {
			return;
		}

		mDamageMode = ScoreboardUtils.toggleTag(mPlayer, DAMAGE_MODE_TAG);
		String mode;
		if (mDamageMode) {
			mode = "damage";
		} else {
			mode = "healing";
		}
		mPlayer.sendActionBar(ChatColor.YELLOW + "Hand of Light has been set to " + mode + " mode!");
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();

		//Must be holding weapon, tool, or shield
		if (ItemUtils.isSomeBow(mainhand) || ItemUtils.isSomePotion(mainhand) || mainhand.getType().isBlock()
			    || mainhand.getType().isEdible() || mainhand.getType() == Material.TRIDENT || mainhand.getType() == Material.COMPASS) {
			return false;
		}

		return true;
	}

	@Override
	public @Nullable String getMode() {
		return mDamageMode ? "damage" : null;
	}
}
