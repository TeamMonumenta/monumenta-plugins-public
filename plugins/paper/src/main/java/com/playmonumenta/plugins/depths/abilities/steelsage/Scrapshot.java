package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.List;

public class Scrapshot extends DepthsAbility {
	public static final String ABILITY_NAME = "Scrapshot";
	private static final int COOLDOWN = 10 * 20;
	private static final int[] DAMAGE = {30, 37, 45, 52, 60, 75};
	private static final double[] DISTANCE_MULTIPLIER = {1, 1, 1, 1, 0.9, 0.8, 0.7, 0.6};
	private static final double VELOCITY = 1;
	private static final int RANGE = 8;

	private static final Particle.DustOptions SCRAPSHOT_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);

	public Scrapshot(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.SCRAPSHOT;
		mDisplayItem = Material.NETHERITE_SCRAP;
		mTree = DepthsTree.METALLIC;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.125);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 2);

		Vector velocity = mPlayer.getLocation().getDirection().multiply(-VELOCITY);
		mPlayer.setVelocity(velocity.setY(Math.max(0.1, velocity.getY())));

		for (double y = -0.1; y <= 0.1; y += 0.1) {
			for (int a = -1; a <= 1; a++) {
				//Do not make corners
				if (Math.abs(y) == Math.abs(a * 0.1)) {
					continue;
				}

				double angle = a * Math.toRadians(5);
				Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY() + y, FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
				newDir.normalize();

				box = BoundingBox.of(loc, 0.55, 0.55, 0.55);

				world.spawnParticle(Particle.CLOUD, loc, 0, newDir.getX(), newDir.getY(), newDir.getZ(), 1);

				for (int i = 0; i < RANGE; i++) {
					box.shift(newDir);
					Location bLoc = box.getCenter().toLocation(world);

					world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 3, 0.025, 0.025, 0.025, 0.05);
					world.spawnParticle(Particle.REDSTONE, bLoc, 3, 0.05, 0.05, 0.05, SCRAPSHOT_COLOR);

					if (bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract(newDir.multiply(0.5));
						world.spawnParticle(Particle.SQUID_INK, bLoc, (8 - i) * 2, 0, 0, 0, 0.125);
						world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
						break;
					}
					Iterator<LivingEntity> iter = mobs.iterator();
					while (iter.hasNext()) {
						LivingEntity mob = iter.next();
						if (box.overlaps(mob.getBoundingBox())) {
							DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, DISTANCE_MULTIPLIER[i] * DAMAGE[mRarity - 1], mInfo.mLinkedSpell);

							mob.setVelocity(new Vector(0, 0, 0));
							iter.remove();
							mobs.remove(mob);

							world.spawnParticle(Particle.SQUID_INK, bLoc, (8 - i) * 2, 0, 0, 0, 0.125);
							world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);

							return;
						}
					}
				}
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
	    if (event.getType() == DamageType.MELEE) {
	        cast(Action.LEFT_CLICK_AIR);
	    }
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to fire a blunderbuss shot that goes up to " + RANGE + " blocks, in a cone that deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " projectile damage and knocks you backward. Damage is decreased for every block of distance after the first 4 blocks. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}
