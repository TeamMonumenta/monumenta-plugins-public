package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BlasphemousAura extends Ability {

	private static final int BLASPHEMY_RADIUS = 3;
	private static final float BLASPHEMY_KNOCKBACK_SPEED = 0.3f;
	private static final int BLASPHEMY_1_VULN_LEVEL = 3;
	private static final int BLASPHEMY_2_VULN_LEVEL = 5;
	private static final int BLASPHEMY_VULN_DURATION = 6 * 20;

	public BlasphemousAura(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "BlasphemousAura";
	}

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity damagee = event.getDamaged();
		int amp = getAbilityScore() == 1 ? 2 : 4;
		PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.UNLUCK, 20 * 5, amp, false, true));
		if (getAbilityScore() > 1) {
			int affected = 0;
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 16, mPlayer)) {
				if (mob.hasPotionEffect(PotionEffectType.UNLUCK)) {
					affected++;
				}
			}

			if (affected >= 5) {
				for (Player player : PlayerUtils.getNearbyPlayers(mPlayer, 16, true)) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
					                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 20,
					                                                  0, true, false));
				}
			}
		}
	}

}
