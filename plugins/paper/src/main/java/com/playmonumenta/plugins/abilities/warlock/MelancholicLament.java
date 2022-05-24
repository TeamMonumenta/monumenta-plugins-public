package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class MelancholicLament extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.3;
	private static final int COOLDOWN = 20 * 16;
	private static final int RADIUS = 7;
	private static final int CLEANSE_REDUCTION = 20 * 10;

	public static final String CHARM_RADIUS = "Melancholic Lament Radius";
	public static final String CHARM_COOLDOWN = "Melancholic Lament Cooldown";
	public static final String CHARM_WEAKNESS = "Melancholic Lament Weakness Amplifier";
	public static final String CHARM_RECOVERY = "Melancholic Lament Negative Effect Recovery";


	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(235, 235, 224), 1.0f);

	private final double mWeakenEffect;
	private @Nullable JudgementChain mJudgementChain;

	private int mEnhancementBonusDamage;


	public MelancholicLament(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Melancholic Lament");
		mInfo.mLinkedSpell = ClassAbility.MELANCHOLIC_LAMENT;
		mInfo.mScoreboardId = "Melancholic";
		mInfo.mShorthandName = "MLa";
		mInfo.mDescriptions.add("Press the swap key while sneaking and holding a scythe to recite a haunting song, causing all mobs within 7 blocks to target the user and afflicting them with 20% Weaken for 8 seconds. Cooldown: 16s.");
		mInfo.mDescriptions.add("Increase the Weaken to 30% and decrease the duration of all negative potion effects on players in the radius by 10s.");
		mInfo.mDescriptions.add("When cast, also cleanse all potion debuffs. Then, your next non-ailment damage deals +1 damage per debuff cleansed.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.GHAST_TEAR, 1);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mJudgementChain = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, JudgementChain.class);
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isHoe(mainHandItem)) {
			event.setCancelled(true);
			// *TO DO* - Turn into boolean in constructor -or- look at changing trigger entirely
			if (!mPlayer.isSneaking() || (mPlayer.isSneaking() && mJudgementChain != null && mPlayer.getLocation().getPitch() < -50.0)) {
				return;
			}
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}

			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();

			world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.2f);
			world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.4f);
			world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.6f);

			new PartialParticle(Particle.REDSTONE, loc, 300, 8, 8, 8, 0.125, COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 300, 8, 8, 8, 0.125).spawnAsPlayerActive(mPlayer);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
				EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
				EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
			}

			if (isEnhanced()) {
				mEnhancementBonusDamage = 0;
				for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, mPlayer)) {
					PotionEffect effect = mPlayer.getPotionEffect(effectType);
					if (effect != null) {
						mPlayer.removePotionEffect(effectType);
						mEnhancementBonusDamage += 1;

						// mPlayer.sendMessage("Removed " + effectType);
					}
				}
			}

			if (isLevelTwo()) {
				int reductionTime = CLEANSE_REDUCTION + CharmManager.getExtraDuration(mPlayer, CHARM_RECOVERY);
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS), true)) {
					new PartialParticle(Particle.REDSTONE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125, COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
					for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
						PotionEffect effect = player.getPotionEffect(effectType);
						if (effect != null) {
							player.removePotionEffect(effectType);
							if (effect.getDuration() - reductionTime > 0) {
								player.addPotionEffect(new PotionEffect(effectType, effect.getDuration() - reductionTime, effect.getAmplifier()));
							}
						}
					}
					EntityUtils.setWeakenTicks(mPlugin, player, Math.max(0, EntityUtils.getWeakenTicks(mPlugin, player) - reductionTime));
					EntityUtils.setSlowTicks(mPlugin, player, Math.max(0, EntityUtils.getSlowTicks(mPlugin, player) - reductionTime));
				}
			}
			putOnCooldown();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.AILMENT
			&& mEnhancementBonusDamage > 0) {
			event.setDamage(event.getDamage() + mEnhancementBonusDamage);
			mEnhancementBonusDamage = 0;
		}

		return false;
	}
}
