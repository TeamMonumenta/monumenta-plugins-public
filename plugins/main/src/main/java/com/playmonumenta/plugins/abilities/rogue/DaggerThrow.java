package com.playmonumenta.plugins.abilities.rogue;

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
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.rogue.assassin.Preparation;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 6;
	private static final int DAGGER_THROW_2_DAMAGE = 12;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_1_VULN = 3;
	private static final int DAGGER_THROW_2_VULN = 7;
	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	public DaggerThrow(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DAGGER_THROW;
		mInfo.scoreboardId = "DaggerThrow";
		mInfo.cooldown = DAGGER_THROW_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast() {
		int daggerThrow = getAbilityScore();

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();

		double damage = (daggerThrow == 1) ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE;
		int vulnLevel = (daggerThrow == 1) ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN;

		Preparation pp = (Preparation) AbilityManager.getManager().getPlayerAbility(mPlayer, Preparation.class);
		int ppDuration = 0;
		if (pp != null) {
			ppDuration = pp.getBonus(mInfo.linkedSpell);
		}

		// TODO: Upgrade this to raycast code
		for (int a = -1; a < 2; a++) {
			double angle = a * 0.463; //25o. Set to 0.524 for 30o or 0.349 for 20o
			// ^ I'm sure you can just do Math.toRadians(degrees) to make it easier
			Vector newDir = new Vector(Math.cos(angle) * dir.getX() + Math.sin(angle) * dir.getZ(), dir.getY(), Math.cos(angle) * dir.getZ() - Math.sin(angle) * dir.getX());
			newDir.normalize();

			boolean hit = false;

			for (int i = 1; i <= DAGGER_THROW_RANGE; i++) {
				Location mLoc = (loc.clone()).add((newDir.clone()).multiply(i));
				Location pLoc = mLoc.clone();

				for (int t = 0; t < 10; t++) {
					pLoc.add((newDir.clone()).multiply(0.1));
					mWorld.spawnParticle(Particle.REDSTONE, pLoc, 1, 0.1, 0.1, 0.1, DAGGER_THROW_COLOR);
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mLoc, 1, mPlayer)) {
					if (MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, DAGGER_THROW_DURATION, vulnLevel, true, false));
						if (ppDuration > 0) {
							EntityUtils.applyStun(mPlugin, ppDuration, mob);
						}
					}

					hit = true;
					break;
				}

				if (mLoc.getBlock().getType().isSolid() || hit) {
					mLoc.subtract((newDir.clone()).multiply(0.5));
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, mLoc, 3, 0.3, 0.3, 0.3, 0.1);

					if (hit) {
						mWorld.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);
					}

					break;
				}
			}
		}

		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.0f);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();

			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				return mPlayer.getLocation().getPitch() > -50;
			}
		}
		return false;
	}

}
