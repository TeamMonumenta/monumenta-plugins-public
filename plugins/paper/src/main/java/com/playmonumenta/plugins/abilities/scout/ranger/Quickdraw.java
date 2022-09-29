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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
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
import org.bukkit.util.Vector;

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 6;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 3;

	public static final String CHARM_DAMAGE = "Quickdraw Damage";
	public static final String CHARM_COOLDOWN = "Quickdraw Cooldown";
	public static final String CHARM_PIERCING = "Quickdraw Piercing";

	public @Nullable Projectile mProjectile;

	public Quickdraw(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Quickdraw");
		mInfo.mLinkedSpell = ClassAbility.QUICKDRAW;
		mInfo.mScoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add(String.format("Left-clicking with a projectile weapon instantly fires that projectile, fully charged. This skill can only apply Recoil once before touching the ground. Cooldown: %ds.", QUICKDRAW_1_COOLDOWN / 20));
		mInfo.mDescriptions.add(String.format("Cooldown: %ds.", QUICKDRAW_2_COOLDOWN / 20));
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
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && ItemUtils.isProjectileWeapon(inMainHand)) {
			World world = mPlayer.getWorld();

			new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);

			boolean launched = shootProjectile(inMainHand, 0);
			if (launched) {
				putOnCooldown();

				if (ItemStatUtils.getEnchantmentLevel(inMainHand, ItemStatUtils.EnchantmentType.MULTISHOT) > 0) {
					for (int i = 0; i < 2; i++) {
						shootProjectile(inMainHand, 2 * i - 1);
					}
				}
			}
		}
	}

	private boolean shootProjectile(ItemStack inMainHand, int deviation) {
		if (mPlayer == null) {
			return false;
		}

		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			direction.rotateAroundY(deviation * 10.0 * Math.PI / 180);
		}

		World world = mPlayer.getWorld();
		Location eyeLoc = mPlayer.getEyeLocation();
		Projectile proj;
		switch (inMainHand.getType()) {
			case BOW, CROSSBOW -> proj = world.spawnArrow(eyeLoc, direction, 3.0f, 0, Arrow.class);
			case TRIDENT -> proj = world.spawnArrow(eyeLoc, direction, 3.0f, 0, Trident.class);
			case SNOWBALL -> {
				proj = world.spawn(eyeLoc, Snowball.class);
				proj.setVelocity(direction.normalize().multiply(3.0f));
			}
			default -> {
				// How did we get here?
				return false;
			}
		}
		mProjectile = proj;

		if (ItemStatUtils.getEnchantmentLevel(inMainHand, ItemStatUtils.EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				proj.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}

		proj.setShooter(mPlayer);
		if (proj instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(inMainHand.getEnchantmentLevel(Enchantment.PIERCING) + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING));
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(proj);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		if (!eventLaunch.isCancelled()) {
			mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.FIREWORKS_SPARK);
		}

		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(proj);
		if (stats != null) {
			ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
			if (map != null) {
				ItemStat projDamageAdd = ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat();
				double damage = map.get(projDamageAdd);
				map.set(projDamageAdd, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage));
			}
		}

		return !eventLaunch.isCancelled();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

	public boolean isQuickDraw(Projectile projectile) {
		return projectile == mProjectile;
	}
}
