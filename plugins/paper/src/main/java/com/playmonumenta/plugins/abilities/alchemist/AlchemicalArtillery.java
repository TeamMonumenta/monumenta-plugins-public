package com.playmonumenta.plugins.abilities.alchemist;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Alchemical Artillery: Left click with a bow to prime it with an alchemist potion.
 * Shooting the bow in the next 5 seconds consumes 4 / 3 potions.
 * When the arrow hits an enemy, the potion is appled in a 3 / 5 block radius.
 * Basilisk Poison is also applied if applicable.
 */

public class AlchemicalArtillery extends Ability {
	private static final String ALCHEMICAL_ARTILLERY_METAKEY = "AlchemicalArtilleryArrowGotTheDankPot";
	private static final int ALCHEMICAL_ARTILLERY_1_RADIUS = 3;
	private static final int ALCHEMICAL_ARTILLERY_2_RADIUS = 4;
	private static final int ALCHEMICAL_ARTILLERY_1_COST = 4;
	private static final int ALCHEMICAL_ARTILLERY_2_COST = 3;
	private static final int ALCHEMICAL_ARTILLERY_ACTIVITY_PERIOD = 20 * 5;

	private int mRadius;
	private int mCost;
	private boolean mActive = false;

	public AlchemicalArtillery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Alchemical Artillery");
		mInfo.linkedSpell = Spells.ALCHEMICAL_ARTILLERY;
		mInfo.scoreboardId = "Artillery";
		mInfo.mShorthandName = "AAr";
		mInfo.mDescriptions.add("Left click with a bow without shifting to prime your next arrow shot within 5 seconds. Shooting a primed arrow consumes 4 Alchemist Potions and applies all potion abilities and Basilisk Poison in a 3 block radius of where the arrow lands.");
		mInfo.mDescriptions.add("The cost is reduced to 3 potions and the radius is increased to 4 blocks.");
		mInfo.cooldown = 0;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mRadius = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_RADIUS : ALCHEMICAL_ARTILLERY_2_RADIUS;
		mCost = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_COST : ALCHEMICAL_ARTILLERY_2_COST;
	}

	@Override
	public void cast(Action action) {
		if (!mActive) {
			if (!mPlayer.isSneaking() && InventoryUtils.isBowItem(mPlayer.getInventory().getItemInMainHand())) {
				mActive = true;
				mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 2.5f);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 1, 0.25, 0, 0.25, 0);
						if (mTicks == 3) {
							mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 2.5f);
						}
						if (!mActive || mTicks > ALCHEMICAL_ARTILLERY_ACTIVITY_PERIOD) {
							mActive = false;
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		if (arrow.hasMetadata(ALCHEMICAL_ARTILLERY_METAKEY)) {
			// Must remove metadata or the arrow could bounce and go boom x2
			arrow.removeMetadata(ALCHEMICAL_ARTILLERY_METAKEY, mPlugin);
			Location loc = arrow.getLocation().add(0, 1, 0);
			mWorld.spawnParticle(Particle.SPELL_MOB, loc, 15 * (int) Math.pow(mRadius, 2), mRadius, 0.5, mRadius, 0);
			mWorld.spawnParticle(Particle.FLAME, loc, 3 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.06 * mRadius);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.08 * mRadius);
			mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1);
			mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1);

			BrutalAlchemy ba = (BrutalAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
			GruesomeAlchemy ga = (GruesomeAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class);
			NightmarishAlchemy na = (NightmarishAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, NightmarishAlchemy.class);
			InvigoratingOdor io = (InvigoratingOdor) AbilityManager.getManager().getPlayerAbility(mPlayer, InvigoratingOdor.class);
			BasiliskPoison bp = (BasiliskPoison) AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);

			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);
			int size = mobs.size();
			boolean guaranteedApplicationApplied = false;

			for (LivingEntity mob : mobs) {
				// Gruesome must go first to apply the Vulnerability
				if (ga != null) {
					ga.apply(mob);
				}
				if (ba != null) {
					ba.apply(mob);
				}
				if (na != null) {
					guaranteedApplicationApplied = na.apply(mob, size, guaranteedApplicationApplied);
				}
				if (io != null) {
					io.apply(mob);
				}
				if (bp != null) {
					bp.apply(mob);
				}
			}
			if (io != null) {
				io.createAura(loc, mRadius);
			}
		}
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
		if (mActive) {
			if (AbilityUtils.removeAlchemistPotions(mPlayer, mCost)) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
				arrow.setMetadata(ALCHEMICAL_ARTILLERY_METAKEY, new FixedMetadataValue(mPlugin, mRadius));
				mActive = false;
			}
		}

		return true;
	}

}
