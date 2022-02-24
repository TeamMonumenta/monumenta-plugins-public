package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HauntingShades extends Ability {

	private static final String ATTR_NAME = "HauntingShadesHealing";

	private static final int COOLDOWN = 10 * 20;
	private static final int SHADES_DURATION = 7 * 20;
	private static final double VULN = 0.1;
	private static final double HEAL_PERCENT = 0.025;
	private static final int EFFECT_LEVEL = 0;
	private static final int EFFECT_DURATION = 20 * 1;
	private static final int RANGE = 10;
	private static final int AOE_RANGE = 6;
	private static final double HITBOX_LENGTH = 0.55;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public HauntingShades(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Haunting Shades");
		mInfo.mLinkedSpell = ClassAbility.HAUNTING_SHADES;
		mInfo.mScoreboardId = "HauntingShades";
		mInfo.mShorthandName = "HS";
		mInfo.mDescriptions.add("Press the swap key while not sneaking with a scythe to conjure a Shade at the target block or mob location. Mobs within 6 blocks of a Shade are afflicted with 10% Vulnerability. A Shade fades back into darkness after 7 seconds. Cooldown: 10s.");
		mInfo.mDescriptions.add("Players within 6 blocks of the shade are given strength 1 and gain a custom healing effect that regenerates 2.5% of max health every second for 1 second. Effects do not stack with other Tenebrists.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.ALL;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		return ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isHoe(mainHandItem)) {
			event.setCancelled(true);
			// TODO - Turn into boolean in constructor -or- look at changing trigger entirely
			if (isTimerActive() || mPlayer.isSneaking()) {
				return;
			}
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.65f);

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, RANGE));

			for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);

				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075);
				world.spawnParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR);

				Iterator<LivingEntity> iter = nearbyMobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							placeShade(bLoc);
							return;
						}
					}
				}

				if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					placeShade(bLoc);
					return;
				}

				box.shift(shift);
			}
			placeShade(box.getCenter().toLocation(world));
		}
	}

	private void placeShade(Location bLoc) {
		if (mPlayer == null) {
			return;
		}
		World world = mPlayer.getWorld();
		bLoc.setDirection(mPlayer.getLocation().toVector().subtract(bLoc.toVector()).normalize());
		ArmorStand stand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, "HauntingShade");
		if (stand == null) {
			return;
		}
		stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		stand.setGravity(false);
		stand.setCanMove(false);
		stand.setSilent(true);
		stand.setCustomName("Haunting Shade");
		stand.setCustomNameVisible(false);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				Location l = bLoc;
				mT++;
				if (mT % 5 == 0) {
					List<Player> affectedPlayers = PlayerUtils.playersInRange(l, AOE_RANGE, true);
				    Set<LivingEntity> affectedMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(l, AOE_RANGE));
					if (isLevelTwo()) {
						for (Player p : affectedPlayers) {
							double maxHealth = EntityUtils.getMaxHealth(p);
							mPlugin.mEffectManager.addEffect(p, ATTR_NAME, new CustomRegeneration(EFFECT_DURATION, maxHealth * HEAL_PERCENT, mPlayer, mPlugin));

							mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, EFFECT_DURATION, EFFECT_LEVEL, true, false));
						}
					}

				    for (LivingEntity m : affectedMobs) {
						EntityUtils.applyVulnerability(mPlugin, EFFECT_DURATION, VULN, m);
				    }
				}

				if (mT % 10 == 0) {
					new BukkitRunnable() {
						double mRadius = 0;
						final Location mLoc = l;
						@Override
						public void run() {
							mRadius += 1.25;
							for (double j = 0; j < 360; j += 30) {
								double radian1 = Math.toRadians(j);
								mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
								world.spawnParticle(Particle.REDSTONE, mLoc, 3, 0.2, 0.2, 0.2, 0.1, COLOR);
								world.spawnParticle(Particle.SMOKE_NORMAL, mLoc, 1, 0, 0, 0, 0.15);
								mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
							}
							if (mRadius >= AOE_RANGE + 1) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}

				if (mT % 20 == 0) {
					world.playSound(l, Sound.ENTITY_BLAZE_HURT, 0.3f, 0.5f);
				}

				if (mT >= SHADES_DURATION || mPlayer.isDead() || !mPlayer.isValid()) {
					stand.remove();
					world.spawnParticle(Particle.REDSTONE, bLoc, 45, 0.2, 1.1, 0.2, 0.1, COLOR);
					world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 40, 0.3, 1.1, 0.3, 0.15);
					world.playSound(bLoc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
