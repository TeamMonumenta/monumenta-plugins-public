package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Sneak and right-click to trigger a channeled frost ray (range: 12 blocks),
 * firing into the direction you are looking at and dealing 5 / 7 damage per
 * 0.5s, applying Slowness II to affected mobs. The direction of the ray follows
 * the direction you are facing, and it lasts up to 5 / 6 s as long as you do
 * not move. Cooldown: 35 s / 25 s
 *
 * NOTE: Particle effects need flair
 */

public class FrostRay extends Ability {
	private static final int FROST_RAY_1_COOLDOWN = 35 * 20;
	private static final int FROST_RAY_2_COOLDOWN = 25 * 20;
	private static final int FROST_RAY_1_DURATION = 5 * 20;
	private static final int FROST_RAY_2_DURATION = 6 * 20;
	private static final int FROST_RAY_RANGE = 14;
	private static final int FROST_RAY_1_DAMAGE = 5;
	private static final int FROST_RAY_2_DAMAGE = 7;
	private static final int FROST_RAY_SLOWNESS_LEVEL = 1;
	private static final int FROST_RAY_SLOWNESS_DURATION = 2 * 20;
	private static final double FROST_RAY_RADIUS = 0.65;
	private static final Particle.DustOptions FROST_RAY_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1f);

	public FrostRay(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.FROST_RAY;
		mInfo.scoreboardId = "FrostRay";
		mInfo.cooldown = getAbilityScore() == 1 ? FROST_RAY_1_COOLDOWN : FROST_RAY_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.getVelocity().length() <= 0.1 && (InventoryUtils.isWandItem(mHand) || InventoryUtils.isWandItem(oHand));
	}

	@Override
	public boolean cast() {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 30, 0f, 0f, 0f, 0.15f); //Rudimentary effects
		int damage = getAbilityScore() == 1 ? FROST_RAY_1_DAMAGE : FROST_RAY_2_DAMAGE;
		new BukkitRunnable() {
			int t = 0;
			Location castLocation = mPlayer.getLocation().add(0, 1.2, 0);
			int maxDuration = getAbilityScore() == 1 ? FROST_RAY_1_DURATION : FROST_RAY_2_DURATION;

			@Override
			public void run() {
				t++;
				Location location = mPlayer.getLocation().add(0, 1.2, 0);
				Vector increment = location.getDirection();
				if (castLocation.distance(location) > 1 || t >= maxDuration) {
					this.cancel();
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1);
				}
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(location, FROST_RAY_RANGE);
				BoundingBox box = BoundingBox.of(location, FROST_RAY_RADIUS, FROST_RAY_RADIUS, FROST_RAY_RADIUS);
				for (int i = 0; i <= FROST_RAY_RANGE; i++) {
					box.shift(increment);
					Location loc = box.getCenter().toLocation(mWorld);
					mWorld.spawnParticle(Particle.REDSTONE, loc, 4, 0.1, 0.1, 0.1, FROST_RAY_COLOR);
					mWorld.spawnParticle(Particle.SNOWBALL, loc, 2, 0.1, 0.1, 0.1, 0.1);
					if (t % 10 == 0) {
						for (LivingEntity e : mobs) {
							if (e.getBoundingBox().overlaps(box)) {
								EntityUtils.damageEntity(mPlugin, e, damage, mPlayer, MagicType.ICE);
								e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_RAY_SLOWNESS_DURATION, FROST_RAY_SLOWNESS_LEVEL, true, false));
							}
						}
					}
					if (loc.getBlock().getType().isSolid()) {
						mWorld.spawnParticle(Particle.CLOUD, box.getCenter().toLocation(mWorld), 3, 0, 0, 0, 0.125f);
						break;
					}
				}
			}

		}.runTaskTimer(mPlugin, 1, 1);

		putOnCooldown();
		return true;
	}
}
