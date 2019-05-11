package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class EnfeeblingElixir extends Ability {
	private static final int ENFEEBLING_COOLDOWN = 20 * 20;
	private static final int ENFEEBLING_DURATION_1 = 6 * 20;
	private static final int ENFEEBLING_DURATION_2 = 9 * 20;
	private static final float ENFEEBLING_KNOCKBACK_1_SPEED = 0.35f;
	private static final float ENFEEBLING_KNOCKBACK_2_SPEED = 0.5f;
	private static final int ENFEEBLING_JUMP_LEVEL = 1;
	private static final int ENFEEBLING_RADIUS = 3;

	public EnfeeblingElixir(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ENFEEBLING_ELIXIR;
		mInfo.scoreboardId = "EnfeeblingElixir";
		mInfo.cooldown = ENFEEBLING_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int enfeeblingElixir = getAbilityScore();
		LivingEntity damagee = (LivingEntity) event.getEntity();
		if (EntityUtils.isHostileMob(damagee)) {
			if (mPlayer.isSneaking()) {
				int duration = (enfeeblingElixir == 1) ? ENFEEBLING_DURATION_1 : ENFEEBLING_DURATION_2;

				float kbSpeed = (enfeeblingElixir == 1) ? ENFEEBLING_KNOCKBACK_1_SPEED : ENFEEBLING_KNOCKBACK_2_SPEED;
				int weaknessLevel = enfeeblingElixir;

				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ENFEEBLING_RADIUS, mPlayer)) {
					MovementUtils.KnockAway(damagee, mob, kbSpeed);
					PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.WEAKNESS, duration, weaknessLevel, true, false));
				}

				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.SPEED, duration, enfeeblingElixir - 1));
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.JUMP, duration, ENFEEBLING_JUMP_LEVEL));

				mWorld.spawnParticle(Particle.SPELL_MOB, damagee.getLocation(), 100, 2, 1.5, 2, 0);
				mWorld.playSound(damagee.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);
				putOnCooldown();
			}
		}
		return true;
	}
}
