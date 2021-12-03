package com.playmonumenta.plugins.depths.abilities.shadow;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class DummyDecoy extends DepthsAbility {

	public static final String ABILITY_NAME = "Dummy Decoy";

	public static final int COOLDOWN = 25 * 20;
	public static final String DUMMY_NAME = "AlluringShadow";
	public static final int[] HEALTH = {30, 35, 40, 45, 50, 60};
	public static final double[] STUN_SECONDS = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5};
	public static final int MAX_TICKS = 4 * 20;
	public static final int AGGRO_RADIUS = 8;
	public static final int STUN_RADIUS = 4;
	public static final String META_DATA_TAG = "DummyDecoyArrow";

	public DummyDecoy(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.ARMOR_STAND;
		mTree = DepthsTree.SHADOWS;
		mInfo.mLinkedSpell = ClassAbility.DUMMY_DECOY;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	public void execute() {

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1, 1.4f);

		Arrow arrow = mPlayer.launchProjectile(Arrow.class);

		arrow.setPierceLevel(0);
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(2.0));
		arrow.setMetadata(META_DATA_TAG, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SPELL_WITCH);
		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
		Bukkit.getPluginManager().callEvent(eventLaunch);
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				if (arrow == null || mT > MAX_TICKS) {
					arrow.remove();
					this.cancel();
				}

				if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
					spawnDecoy(arrow, arrow.getLocation());
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (proj instanceof AbstractArrow && proj.hasMetadata(META_DATA_TAG)) {
			spawnDecoy((AbstractArrow) proj, le.getLocation());
		}

		return true;
	}

	public void spawnDecoy(AbstractArrow arrow, Location loc) {

		LivingEntity e = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, DUMMY_NAME);
		e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH[mRarity - 1]);
		e.setHealth(HEALTH[mRarity - 1]);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (e == null || e.getHealth() <= 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(e.getLocation(), STUN_RADIUS)) {
						EntityUtils.applyStun(mPlugin, (int)STUN_SECONDS[mRarity - 1] * 20, mob);
					}
					this.cancel();
				}

				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, AGGRO_RADIUS);
				for (LivingEntity le : mobs) {
					if (!le.getScoreboardTags().contains("Boss")) {
						Mob mob = (Mob) le;
						mob.setTarget(e);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		arrow.remove();
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return true;
		}

		if (mPlayer.isSneaking()) {
			arrow.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			mInfo.mCooldown = (int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer));
			putOnCooldown();
			execute();
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting a bow while sneaking fires a cursed arrow. When the arrow lands, it spawns a dummy decoy at the location with " + DepthsUtils.getRarityColor(rarity) + HEALTH[rarity - 1] + ChatColor.WHITE + " health that lasts for up to " + MAX_TICKS / 20 + " seconds. The decoy aggros mobs within " + AGGRO_RADIUS + " blocks on a regular interval. On death, the decoy explodes, stunning mobs in a " + STUN_RADIUS + " block radius for " + DepthsUtils.getRarityColor(rarity) + STUN_SECONDS[rarity - 1] + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}

