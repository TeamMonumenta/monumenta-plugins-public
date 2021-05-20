package com.playmonumenta.plugins.abilities.scout.hunter;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class SplitArrow extends Ability {

	private static final double SPLIT_ARROW_1_DAMAGE_PERCENT = 0.4;
	private static final double SPLIT_ARROW_2_DAMAGE_PERCENT = 0.7;
	private static final double SPLIT_ARROW_CHAIN_RANGE = 5;
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);

	private final double mDamagePercent;

	public SplitArrow(Plugin plugin, Player player) {
		super(plugin, player, "Split Arrow");
		mInfo.mLinkedSpell = ClassAbility.SPLIT_ARROW;
		mInfo.mScoreboardId = "SplitArrow";
		mInfo.mShorthandName = "SA";
		mInfo.mDescriptions.add("When you hit an enemy with an arrow, the next nearest enemy within 5 blocks takes 40% of the original arrow damage (ignores invulnerability frames).");
		mInfo.mDescriptions.add("Damage to the second target is increased to 70% of the original arrow damage.");
		mInfo.mIgnoreTriggerCap = true;

		mDamagePercent = getAbilityScore() == 1 ? SPLIT_ARROW_1_DAMAGE_PERCENT : SPLIT_ARROW_2_DAMAGE_PERCENT;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			LivingEntity nearestMob = EntityUtils.getNearestMob(damagee.getLocation(), SPLIT_ARROW_CHAIN_RANGE, damagee);

			if (nearestMob != null) {
				Location loc = damagee.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				World world = mPlayer.getWorld();
				for (int i = 0; i < 50; i++) {
					loc.add(dir.clone().multiply(0.1));
					world.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}
				world.spawnParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6);
				world.spawnParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6);
				world.playSound(eye, Sound.ENTITY_ARROW_HIT, 1, 1.2f);

				nearestMob.setNoDamageTicks(0);
				double lastDamage = nearestMob.getLastDamage();
				EntityUtils.damageEntity(mPlugin, nearestMob, event.getDamage() * mDamagePercent, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
				nearestMob.setLastDamage(lastDamage + event.getDamage() * mDamagePercent);
				MovementUtils.knockAway(damagee, nearestMob, 0.125f, 0.35f);
			}
		}

		return true;
	}

}
