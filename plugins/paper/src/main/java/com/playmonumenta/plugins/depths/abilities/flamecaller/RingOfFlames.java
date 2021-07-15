package com.playmonumenta.plugins.depths.abilities.flamecaller;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

import net.md_5.bungee.api.ChatColor;

public class RingOfFlames extends DepthsAbility {

	public static final String ABILITY_NAME = "Ring of Flames";
	private static final int COOLDOWN = 18 * 20;
	private static final double[] DAMAGE = {4, 5, 6, 7, 8};
	private static final int DURATION = 10 * 20;
	private static final int EFFECT_DURATION = 4 * 20;

	public RingOfFlames(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.RING_OF_FLAMES;
		mDisplayItem = Material.BLAZE_POWDER;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		world.spawnParticle(Particle.SOUL_FIRE_FLAME, mPlayer.getLocation(), 50, 0.25f, 0.1f, 0.25f, 0.15f);

		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0);
			}
		}.runTaskLater(mPlugin, 5);

		Location tempLoc = loc.clone();
		List<BoundingBox> boxes = new ArrayList<>();

		for (int deg = 0; deg < 360; deg += 5) {
			tempLoc.set(loc.getX() + 4 * FastUtils.cos(deg), loc.getY() + 2, loc.getZ() + 4 * FastUtils.sin(deg));
			boxes.add(BoundingBox.of(tempLoc, 0.5, 3.5, 0.5));
		}

		new BukkitRunnable() {
			private int mTicks = 0;
			private int mDeg = 0;
			private List<LivingEntity> mMobs;
			@Override
			public void run() {
				if (mTicks >= DURATION) {
					this.cancel();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 1, 0.75f);
					return;
				}

				for (int y = -1; y < 4; y++) {
					tempLoc.set(loc.getX() + 4 * FastUtils.cos(mDeg), loc.getY() + y, loc.getZ() + 4 * FastUtils.sin(mDeg));
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, tempLoc, 1, 0, 0, 0, 0);

					tempLoc.set(loc.getX() + 4 * FastUtils.cos(mDeg + 180), loc.getY() + y, loc.getZ() + 4 * FastUtils.sin(mDeg + 180));
					world.spawnParticle(Particle.FLAME, tempLoc, 1, 0, 0, 0, 0);
				}
				mDeg++;
				if (mDeg >= 360) {
					mDeg = 0;
				}


				if (mTicks % 20 == 0) {
					mMobs = EntityUtils.getNearbyMobs(loc, 6);

					int mobsHitThisTick = 0;
					for (BoundingBox box : boxes) {
						for (LivingEntity e : mMobs) {
							if (box.overlaps(e.getBoundingBox())) {
								if (mobsHitThisTick <= 10) {
									world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.8f, 1f);
								}
								world.spawnParticle(Particle.SOUL_FIRE_FLAME, e.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
								EntityUtils.applyFire(mPlugin, EFFECT_DURATION, e, mPlayer);
								EntityUtils.applyBleed(mPlugin, EFFECT_DURATION, 2, e);
								EntityUtils.damageEntity(mPlugin, e, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
								mobsHitThisTick++;
							}
						}
					}
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to summon a ring of flames around you that lasts for " + DURATION / 20 + " seconds. Enemies on the flame perimeter are dealt " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage every second, and they are inflicted with Bleed 1 and set on fire for " + EFFECT_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}
