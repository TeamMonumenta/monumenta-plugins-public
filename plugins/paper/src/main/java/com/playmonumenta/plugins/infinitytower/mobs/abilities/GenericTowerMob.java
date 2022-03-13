package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class GenericTowerMob extends TowerAbility {

	final Mob mBoss;
	private LivingEntity mLastTarget = null;

	public GenericTowerMob(Plugin plugin, String identityTag, Mob boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		mBoss = boss;

		//used a runnable so even if the mob is silenced, this "spell" will still get cast.
		new BukkitRunnable() {

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					cancel();
					return;
				}

				if (mGame.isGameEnded()) {
					TowerFileUtils.warning("Game ended but mob still loaded: " + mBoss.getCustomName() + " Unloading..");
					cancel();
					return;
				}

				if (mBoss.getTarget() != null && mBoss.getTarget().getScoreboardTags().contains(TowerConstants.MOB_TAG_UNTARGETABLE)) {
					mBoss.setTarget(null);
					mLastTarget = null;
				}

				if (mBoss.getTarget() != null && mBoss.getTarget() instanceof Player) {
					mBoss.setTarget(mLastTarget);
				}

				if (mBoss.getTarget() != null && (mBoss.getTarget().isDead() || !mBoss.getTarget().isValid())) {
					mBoss.setTarget(mLastTarget);
				}

				if (mLastTarget != null && ((mIsPlayerMob && mLastTarget.getScoreboardTags().contains(TowerConstants.MOB_TAG_PLAYER_TEAM) || (!mIsPlayerMob && mLastTarget.getScoreboardTags().contains(TowerConstants.MOB_TAG_FLOOR_TEAM))))) {
					mLastTarget = null;
					mBoss.setTarget(null);
				}

				if (mGame.isTurnEnded()) {
					return;
				}

				if (mLastTarget != null) {
					if (!mLastTarget.isValid() || mLastTarget.isDead() || (mIsPlayerMob && mGame.mPlayerMobs.contains(mLastTarget)) || (!mIsPlayerMob && mGame.mFloorMobs.contains(mLastTarget))) {
						mLastTarget = null;
						mBoss.setTarget(null);
					}
				}


				if (mLastTarget == null) {
					List<LivingEntity> targets = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
					targets.removeIf(entity -> entity.getScoreboardTags().contains(TowerConstants.MOB_TAG_UNTARGETABLE));
					Location loc = mBoss.getLocation();
					targets.sort((a, b) -> (int) (loc.distance(a.getLocation()) - loc.distance(b.getLocation())));

					if (targets.size() > 0) {
						mBoss.setTarget(targets.get(0));
						mLastTarget = targets.get(0);
					} else {
						mBoss.setTarget(null);
					}
				} else {
					mBoss.setTarget(mLastTarget);
				}
			}

		}.runTaskTimer(TowerManager.mPlugin, 10, 5);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);

	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player) {
			event.setCancelled(true);
			return;
		}

		if (damagee instanceof Mob target) {
			if (!target.getScoreboardTags().contains(TowerConstants.MOB_TAG)) {
				event.setCancelled(true);
				return;
			}
			if (target != mLastTarget) {
				if ((mIsPlayerMob && target.getScoreboardTags().contains(TowerConstants.MOB_TAG_PLAYER_TEAM)) || (!mIsPlayerMob && target.getScoreboardTags().contains(TowerConstants.MOB_TAG_FLOOR_TEAM))) {
					event.setCancelled(true);
				}
			}
			TowerGameUtils.getFinalDamage(event, mBoss, target, event.getDamage());
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {

		if (source instanceof Player player && player.getScoreboardTags().contains(TowerConstants.PLAYER_TAG)) {
			event.setCancelled(true);
			return;
		}

		if ((source.getScoreboardTags().contains(TowerConstants.MOB_TAG_FLOOR_TEAM) && !mIsPlayerMob) || (mIsPlayerMob && source.getScoreboardTags().contains(TowerConstants.MOB_TAG_PLAYER_TEAM))) {
			event.setCancelled(true);
			return;
		}


		if (damager != source) {
			double realDamage = 0;
			EntityEquipment eq = source.getEquipment();
			if (eq != null) {
				ItemStack mainHand = eq.getItemInMainHand();
				if (mainHand.getType() != Material.AIR) {
					ItemMeta meta = mainHand.getItemMeta();
					if (meta != null && meta.hasAttributeModifiers()) {
						Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.HORSE_JUMP_STRENGTH);
						if (modifiers != null) {
							for (AttributeModifier mod : modifiers) {
								realDamage += mod.getAmount();
							}
							event.setDamage(realDamage);
						}
					}
				}
			}
		}
	}

	@Override
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		event.setCancelled(true);
	}
}
