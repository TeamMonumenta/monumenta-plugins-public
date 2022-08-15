package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 6;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 3;

	public static final String CHARM_DAMAGE = "Quickdraw Damage";
	public static final String CHARM_COOLDOWN = "Quickdraw Cooldown";
	public static final String CHARM_PIERCING = "Quickdraw Piercing";

	private int mLeftClicks = 0;
	public Projectile mProjectile = null;

	public Quickdraw(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Quickdraw");
		mInfo.mLinkedSpell = ClassAbility.QUICKDRAW;
		mInfo.mScoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add("Left-clicking with a bow or snowball to instantly fire a fully charged arrow / snowball. Left-clicking twice with a trident will fire a trident. This skill can only apply Recoil once before touching the ground. Cooldown: 6s.");
		mInfo.mDescriptions.add("Cooldown: 3s.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && (ItemUtils.isBowOrTrident(inMainHand) || inMainHand.getType() == Material.SNOWBALL) && !ItemUtils.isShootableItem(inOffHand) && !ItemStatUtils.isShattered(inMainHand)) {
			World world = mPlayer.getWorld();

			if (ItemUtils.isSomeBow(inMainHand)) {
				new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);

				boolean launched = shootArrow(inMainHand, 0);
				if (launched) {
					putOnCooldown();

					if (inMainHand.containsEnchantment(Enchantment.MULTISHOT)) {
						for (int i = 0; i < 2; i++) {
							shootArrow(inMainHand, 2 * i - 1);
						}
					}
				}
			} else if (inMainHand.getType() == Material.SNOWBALL) {
				new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

				world.playSound(mPlayer.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1, 1.4f);

				// Not sure what the purpose of the launched boolean stuff is about,
				// Removed it because I didn't quite understand and it was interfering with putOnCooldown.
				// (Something is replacing the trident)
				shootSnowball(inMainHand, 0);
				putOnCooldown();
			} else if (inMainHand.getType() == Material.TRIDENT) {
				if (mLeftClicks > 0) {
					new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

					world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1.4f);

					// Not sure what the purpose of the launched boolean stuff is about,
					// Removed it because I didn't quite understand and it was interfering with putOnCooldown.
					// (Something is replacing the snowball too)
					shootTrident(inMainHand, 0);

					putOnCooldown();
				} else {
					mLeftClicks += 1;

					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
				}
			}
		}
	}

	private boolean shootArrow(ItemStack inMainHand, int deviation) {
		if (mPlayer == null) {
			return true;
		}

		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			direction.rotateAroundY(deviation * 10.0 * Math.PI / 180);
		}
		Arrow arrow = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), direction, 3.0f, 0, Arrow.class);

		if (ItemStatUtils.getEnchantmentLevel(inMainHand, ItemStatUtils.EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				arrow.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}

		arrow.setShooter(mPlayer);
		arrow.setPierceLevel(inMainHand.getEnchantmentLevel(Enchantment.PIERCING) + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING));
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		if (!eventLaunch.isCancelled()) {
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
		}

		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(arrow);
		ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
		if (map != null) {
			ItemStat projDamageAdd = ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat();
			double damage = map.get(projDamageAdd);
			map.set(projDamageAdd, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage));
		}

		return !eventLaunch.isCancelled();
	}

	private void shootSnowball(ItemStack inMainHand, int deviation) {
		if (mPlayer == null) {
			return;
		}

		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			direction.rotateAroundY(deviation * 10.0 * Math.PI / 180);
		}

		Snowball snowball = mPlayer.getWorld().spawn(mPlayer.getEyeLocation(), Snowball.class);
		mProjectile = snowball;

		snowball.setVelocity(direction.normalize().multiply(3.0f));
		snowball.setShooter(mPlayer);

		if (ItemStatUtils.getEnchantmentLevel(inMainHand, ItemStatUtils.EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				snowball.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(snowball);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		mPlugin.mProjectileEffectTimers.addEntity(snowball, Particle.FIREWORKS_SPARK);

		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(snowball);
		ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
		if (map != null) {
			ItemStat projDamageAdd = ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat();
			double damage = map.get(projDamageAdd);
			map.set(projDamageAdd, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage));
		}
	}


	private void shootTrident(ItemStack inMainHand, int deviation) {
		if (mPlayer == null) {
			return;
		}

		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			direction.rotateAroundY(deviation * 10.0 * Math.PI / 180);
		}
		Trident trident = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), direction, 3.0f, 0, Trident.class);
		mProjectile = trident;

		if (ItemStatUtils.getEnchantmentLevel(inMainHand, ItemStatUtils.EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				trident.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}

		trident.setShooter(mPlayer);
		trident.setPierceLevel(inMainHand.getEnchantmentLevel(Enchantment.PIERCING) + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING));
		trident.setCritical(true);
		trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(trident);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		mPlugin.mProjectileEffectTimers.addEntity(trident, Particle.FIREWORKS_SPARK);

		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(trident);
		ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
		if (map != null) {
			ItemStat projDamageAdd = ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat();
			double damage = map.get(projDamageAdd);
			map.set(projDamageAdd, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
