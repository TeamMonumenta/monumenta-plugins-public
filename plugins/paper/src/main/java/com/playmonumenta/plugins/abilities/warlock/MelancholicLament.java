package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.UmbralWail;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;



public class MelancholicLament extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.3;
	private static final int COOLDOWN = 20 * 16;
	private static final int RADIUS = 7;
	private static final int CLEANSE_REDUCTION = 20 * 10;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(235, 235, 224), 1.0f);

	private final double mWeakenEffect;
	private JudgementChain mJudgementChain;
	private DarkPact mDarkPact;
	private UmbralWail mUmbral;


	public MelancholicLament(Plugin plugin, Player player) {
		super(plugin, player, "Melancholic Lament");
		mInfo.mLinkedSpell = ClassAbility.MELANCHOLIC_LAMENT;
		mInfo.mScoreboardId = "Melancholic";
		mInfo.mShorthandName = "MLa";
		mInfo.mDescriptions.add("Press the swap key while sneaking to recite a haunting song, causing all mobs within 7 blocks to target the user and afflicting them with 20% Weaken for 8 seconds. Cooldown: 16s");
		mInfo.mDescriptions.add("Increase the Weaken to 30% and decrease the duration of all negative potion effects on players in the radius by 10s.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2;
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mJudgementChain = AbilityManager.getManager().getPlayerAbility(mPlayer, JudgementChain.class);
				mDarkPact = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
				mUmbral = AbilityManager.getManager().getPlayerAbility(mPlayer, UmbralWail.class);
			}
		});
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
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

			world.spawnParticle(Particle.REDSTONE, loc, 300, 8, 8, 8, 0.125, COLOR);
			world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 300, 8, 8, 8, 0.125);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
				EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
				EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
			}

			if (getAbilityScore() > 1) {
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
					world.spawnParticle(Particle.REDSTONE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125, COLOR);
					world.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125);
					for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
						PotionEffect effect = player.getPotionEffect(effectType);
						if (effect != null) {
							player.removePotionEffect(effectType);
							if (effect.getDuration() - CLEANSE_REDUCTION > 0) {
								player.addPotionEffect(new PotionEffect(effectType, effect.getDuration() - CLEANSE_REDUCTION, effect.getAmplifier()));
							}
						}
					}
				}
			}
			putOnCooldown();
		}
	}
}