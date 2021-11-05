package com.playmonumenta.plugins.depths.abilities.shadow;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class ChaosDagger extends DepthsAbility {

	public static final String ABILITY_NAME = "Chaos Dagger";
	public static final int COOLDOWN = 22 * 20;
	public static final double[] DAMAGE = {2.0, 2.25, 2.5, 2.75, 3.0, 3.5};
	private static final double VELOCITY = 0.5;
	public static final int STUN_DURATION = 3 * 20;
	public static final int DAMAGE_DURATION = 5 * 20;
	private static final int TARGET_RADIUS = 20;
	private static final int ELITE_RADIUS = 5;

	private Entity mHitMob;

	public ChaosDagger(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.ITEM_FRAME;
		mTree = DepthsTree.SHADOWS;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.CHAOS_DAGGER;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

		event.setCancelled(true);

		if ((!isTimerActive() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand())) &&
				EntityUtils.getNearestMob(mPlayer.getLocation(), 20.0) != null) {
			putOnCooldown();
			mHitMob = null;

			Location loc = mPlayer.getEyeLocation();
			ItemStack itemTincture = new ItemStack(Material.NETHERITE_SWORD);
			ItemUtils.setPlainName(itemTincture, "Chaos Dagger");
			ItemMeta tinctureMeta = itemTincture.getItemMeta();
			tinctureMeta.displayName(Component.text("Chaos Dagger", NamedTextColor.WHITE)
	                .decoration(TextDecoration.ITALIC, false));
			itemTincture.setItemMeta(tinctureMeta);
			World world = mPlayer.getWorld();
			Item tincture = world.dropItem(loc, itemTincture);
			tincture.setPickupDelay(Integer.MAX_VALUE);

			Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
			vel.multiply(VELOCITY);

			tincture.setVelocity(vel);
			tincture.setGlowing(true);
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);

			new BukkitRunnable() {

				int mExpire = 0;
				World mWorld = mPlayer.getWorld();
				LivingEntity mTarget = EntityUtils.getNearestMob(tincture.getLocation(), 20.0);
				Location mLastLocation = null;

				@Override
				public void run() {
					mExpire++;
					if (mExpire >= 10 * 20) {
						tincture.remove();
						// Take the skill off cooldown (by setting to 0)
						mPlugin.mTimers.addCooldown(mPlayer, mInfo.mLinkedSpell, 0);
						this.cancel();
					}
					Location tLoc = tincture.getLocation();

					List<LivingEntity> veryNearbyMobs = EntityUtils.getNearbyMobs(tLoc, ELITE_RADIUS);
					veryNearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
					veryNearbyMobs.removeIf(mob -> !(EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)));
					mTarget = EntityUtils.getNearestMob(tLoc, veryNearbyMobs);

					if (mTarget == null) {
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(tLoc, TARGET_RADIUS);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						mTarget = EntityUtils.getNearestMob(tLoc, nearbyMobs);
					}

					if (mTarget == null) {
						tincture.remove();
						this.cancel();
						return;
					}

					if (mTarget.getBoundingBox().overlaps(tincture.getBoundingBox()) && !mTarget.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, tLoc, 30, 2, 0, 2);
						world.playSound(tLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.15f);
						mHitMob = mTarget;
						if (EntityUtils.isBoss(mTarget) || mTarget.getScoreboardTags().contains("boss_ccimmune")) {
							EntityUtils.applySlow(mPlugin, STUN_DURATION, 0.99f, mTarget);
						} else {
							EntityUtils.applyStun(mPlugin, STUN_DURATION, mTarget);
						}
						mTarget.setGlowing(true);

						new BukkitRunnable() {
							@Override
							public void run() {
								if (mHitMob != null && !(mHitMob instanceof MagmaCube && mHitMob.getName().contains("Gyrhaeddant"))) {
									mHitMob.setGlowing(false);
								}
								mHitMob = null;
								this.cancel();
							}
						}.runTaskLater(mPlugin, DAMAGE_DURATION);

						tincture.remove();
						this.cancel();
					} else {
						loc.getWorld().spawnParticle(Particle.SPELL_WITCH, tLoc, 5, 0.2, 0.2, 0.2, 0.65);

						Vector dir = tLoc.subtract(mTarget.getLocation().toVector().clone().add(new Vector(0, 0.5, 0))).toVector();

						if (dir.length() < 0.001) {
							/* If the direction magnitude is too small, escape, rather than divide by zero / infinity */
							tincture.remove();
							this.cancel();
							return;
						}

						dir = dir.normalize().multiply(VELOCITY * -1.0);

						if (mLastLocation != null && tLoc.distance(mLastLocation) < 0.05) {
							dir.setY(dir.getY() + 1.0);
						}

						tincture.setVelocity(dir);
						mLastLocation = tLoc;

					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void doBonusDamage(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		if (entity == mHitMob) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
			mHitMob = null;
			if (!(entity instanceof MagmaCube && entity.getName().contains("Gyrhaeddant"))) {
				entity.setGlowing(false);
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		doBonusDamage(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		doBonusDamage(event);
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap hands to throw a cursed dagger that stuns an enemy for " + STUN_DURATION / 20 + " seconds (rooting bosses instead). The next instance of any type of damage you deal to this mob within " + DAMAGE_DURATION / 20 + " seconds is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + ". The dagger prioritizes nearby Elites and Bosses but can hit any mob in its path. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}

