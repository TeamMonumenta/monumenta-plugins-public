package com.playmonumenta.plugins.abilities.cleric.hierophant;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Hallowed Beam: Level 1 – Firing a fully-drawn bow while sneaking,
 * if pointed directly at a non-boss undead, will instantly deal 42 damage
 * to the undead instead of consuming the arrow. Cooldown: 20s. Level 2 -
 * The targeted undead explodes, dealing 22 damage to undead within a
 * 5-block radius, and giving slowness 4 to all enemies.
 */
public class HallowedBeam extends Ability {

	private static final double HALLOWED_DAMAGE_DIRECT = 42;
	private static final double HALLOWED_DAMAGE_EXPLOSION = 22;

	public HallowedBeam(Plugin plugin, Player player) {
		super(plugin, player, "Hallowed Beam");
		mInfo.mScoreboardId = "HallowedBeam";
		mInfo.mShorthandName = "HB";
		mInfo.mDescriptions.add("Firing a fully-drawn bow while shifted, while pointing directly at a non-boss undead, will instantly deal 42 damage to the undead instead of consuming an arrow. Cooldown: 20s.");
		mInfo.mDescriptions.add("The targeted undead explodes dealing 22 damage to undead within a 5 block radius. All affected enemies gain slowness 4 for 3s.");
		mInfo.mLinkedSpell = Spells.HALLOWED_BEAM;
		mInfo.mCooldown = 20 * 20;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		Player player = mPlayer;
		LivingEntity e = EntityUtils.getCrosshairTarget(player, 30, false, true, true, false);
		if (e != null && EntityUtils.isUndead(e)) {
			if (arrow.isCritical()) {
				arrow.remove();
				player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.85f);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
				Location loc = player.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(e.getEyeLocation(), loc);
				World world = mPlayer.getWorld();
				for (int i = 0; i < 30; i++) {
					loc.add(dir);
					world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
					if (loc.distance(e.getEyeLocation()) < 1.25) {
						loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
						loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
						break;
					}
				}
				EntityUtils.damageEntity(mPlugin, e, HALLOWED_DAMAGE_DIRECT, player, MagicType.HOLY, true, mInfo.mLinkedSpell);
				Location eLoc = e.getLocation().add(0, e.getHeight() / 2, 0);
				world.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
				world.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f);
				if (getAbilityScore() > 1) {
					// TODO: Revamp explosion effects
					world.spawnParticle(Particle.SPELL_INSTANT, e.getLocation(), 500, 2.5, 0.15f, 2.5, 1);
					world.spawnParticle(Particle.VILLAGER_HAPPY, e.getLocation(), 150, 2.55, 0.15f, 2.5, 1);
					for (LivingEntity le : EntityUtils.getNearbyMobs(eLoc, 5)) {
						if (EntityUtils.isUndead(le)) {
							EntityUtils.damageEntity(mPlugin, le, HALLOWED_DAMAGE_EXPLOSION, player, MagicType.HOLY, true, mInfo.mLinkedSpell);
						}
						PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, 20 * 5, 3, false, true));
					}

				}
				putOnCooldown();
			}
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
