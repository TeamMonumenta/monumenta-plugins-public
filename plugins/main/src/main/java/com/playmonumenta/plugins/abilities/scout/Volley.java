package com.playmonumenta.plugins.abilities.scout;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Volley extends Ability {

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 10;
	private static final double VOLLEY_1_DAMAGE_INCREASE = 0.75;
	private static final double VOLLEY_2_DAMAGE_INCREASE = 1.5;

	public Volley(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.VOLLEY;
		mInfo.scoreboardId = "Volley";
		mInfo.cooldown = VOLLEY_COOLDOWN;

		/*
		 * NOTE! Because Volley has two events - the actual shot event won't trigger by default
		 * when volley is on cooldown. Therefor it needs to bypass the automatic cooldown check
		 * and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (!mPlayer.isSneaking()
		    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			/* This ability is actually on cooldown - event proceeds as normal */
			return true;
		}

		//Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		List<Projectile> projectiles;
		int volley = getAbilityScore();
		int numArrows = (volley == 1) ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;

		// Store PotionData from the original arrow only if it is weakness or slowness
		PotionData tArrowData = null;
		if (arrow instanceof TippedArrow) {
			TippedArrow tArrow = (TippedArrow)arrow;

			tArrowData = tArrow.getBasePotionData();
			if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
				// This arrow isn't weakness or slowness - don't store the potion data
				tArrowData = null;
			}
		}

		if (tArrowData == null) {
			projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.5, 5, Arrow.class);
		} else {
			projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.5, 5, TippedArrow.class);
		}

		for (Projectile proj : projectiles) {
			Arrow _arrow = (Arrow)proj;


			proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));

			// If the base arrow's potion data is still stored, apply it to the new arrows
			if (tArrowData != null && _arrow instanceof TippedArrow) {
				((TippedArrow)_arrow).setBasePotionData(tArrowData);
			}

			_arrow.setCritical(arrow.isCritical());
			_arrow.setFireTicks(arrow.getFireTicks());
			_arrow.setKnockbackStrength(arrow.getKnockbackStrength());

			mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

			//Fire: How stupid of me. I completely forgot we can call Bukkit Events on our OWN
			ProjectileLaunchEvent event = new ProjectileLaunchEvent(_arrow);
			Bukkit.getPluginManager().callEvent(event);
		}

		//  I hate this so much, you don't even know... [Rock]
		Location jankWorkAround = mPlayer.getLocation();
		jankWorkAround.setY(-15);
		arrow.teleport(jankWorkAround);


		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata("Volley")) {
			double damageMultiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_INCREASE : VOLLEY_2_DAMAGE_INCREASE;
			double oldDamage = event.getDamage();
			double extraDamage = 0;
			if (AbilityManager.getManager().getPlayerAbility(mPlayer, Sharpshooter.class) != null) {
				Sharpshooter ss = (Sharpshooter) AbilityManager.getManager().getPlayerAbility(mPlayer, Sharpshooter.class);
				extraDamage += ss.getSharpshot();
			}
			double newDamage = oldDamage + (oldDamage * damageMultiplier);
			event.setDamage(newDamage + extraDamage);
		}
		return true;
	}
}
