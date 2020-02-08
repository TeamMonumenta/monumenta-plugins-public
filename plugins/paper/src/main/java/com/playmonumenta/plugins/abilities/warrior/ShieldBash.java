package com.playmonumenta.plugins.abilities.warrior;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Shield Bash: Right click while looking at a mob within 4 blocks
 * to stun it for 1 / 2 seconds (Cooldown: 5 seconds).
 * Move twice / thrice as fast while blocking.
 */

public class ShieldBash extends Ability {

	private static final int SHIELD_BASH_1_STUN = 20 * 1;
	private static final int SHIELD_BASH_2_STUN = 20 * 2;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 5;
	private static final int SHIELD_BASH_RANGE = 4;

	public static class PlayerWithShieldBash {
		public Player mPlayer;
		private boolean mIsShielding = false;
		private boolean mWasShielding = false;

		public PlayerWithShieldBash(Player player) {
			mPlayer = player;
		}

		public void updateStatus() {
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			mWasShielding = mIsShielding;
			// Check for no bows because drawing them counts as raising your hand
			mIsShielding = (mPlayer.isBlocking() || mPlayer.isHandRaised())
			               && (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD)
			               && (offHand.getType() != Material.BOW || mainHand.getType() != Material.BOW);
		}

		public boolean startedShielding() {
			return mIsShielding && !mWasShielding;
		}

		public boolean stoppedShielding() {
			return !mIsShielding && mWasShielding;
		}
	}

	private static BukkitRunnable mShieldTracker;
	private static Map<UUID, PlayerWithShieldBash> mPlayersWithShieldBash = new HashMap<UUID, PlayerWithShieldBash>();

	private int mStunDuration;

	public ShieldBash(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.SHIELD_BASH;
		mInfo.scoreboardId = "ShieldBash";
		mInfo.cooldown = SHIELD_BASH_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mStunDuration = getAbilityScore() == 1 ? SHIELD_BASH_1_STUN : SHIELD_BASH_2_STUN;
		if (player != null) {
			mPlayersWithShieldBash.put(player.getUniqueId(), new PlayerWithShieldBash(player));
		}

		if (mShieldTracker == null || mShieldTracker.isCancelled()) {
			mShieldTracker = new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<Map.Entry<UUID, PlayerWithShieldBash>> iter = mPlayersWithShieldBash.entrySet().iterator();

					while (iter.hasNext()) {
						PlayerWithShieldBash p = iter.next().getValue();

						p.updateStatus();

						if (AbilityManager.getManager().getPlayerAbility(p.mPlayer, ShieldBash.class) == null) {
							iter.remove();
						}
					}
				}
			};
			mShieldTracker.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer.isHandRaised()) {
					Location eyeLoc = mPlayer.getEyeLocation();
					Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), SHIELD_BASH_RANGE);
					ray.mThroughBlocks = false;
					ray.mThroughNonOccluding = false;

					RaycastData data = ray.shootRaycast();

					List<LivingEntity> mobs = data.getEntities();
					if (mobs != null && !mobs.isEmpty()) {
						for (LivingEntity mob : mobs) {
							if (mob.isValid() && !mob.isDead() && EntityUtils.isHostileMob(mob)) {
								Location mobLoc = mob.getEyeLocation();
								mWorld.spawnParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25);
								mWorld.spawnParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25);
								mWorld.spawnParticle(Particle.CLOUD, mobLoc, 5, 0.15, 0.5, 0.15, 0);
								mWorld.playSound(eyeLoc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 1);
								mWorld.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
								EntityUtils.applyStun(mPlugin, mStunDuration, mob);
								putOnCooldown();
								break;
							}
						}
					}
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD;
	}
}
