package com.playmonumenta.plugins.abilities.rogue;

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

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class DaggerThrow extends Ability {
	
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 6;
	private static final int DAGGER_THROW_2_DAMAGE = 12;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_1_VULN = 3;
	private static final int DAGGER_THROW_2_VULN = 7;
	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);
	
	@Override
	public boolean cast(Player player) {
		int daggerThrow = getAbilityScore(player);
		World world = player.getWorld();

		Location loc = player.getEyeLocation();
		Vector dir = loc.getDirection();

		double damage = (daggerThrow == 1) ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE;
		int vulnLevel = (daggerThrow == 1) ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN;

		// TODO: Upgrade this to raycast code
		for (int a = -1; a < 2; a++) {
			double angle = a * 0.463; //25o. Set to 0.524 for 30o or 0.349 for 20o
			Vector newDir = new Vector(Math.cos(angle) * dir.getX() + Math.sin(angle) * dir.getZ(), dir.getY(), Math.cos(angle) * dir.getZ() - Math.sin(angle) * dir.getX());
			newDir.normalize();

			boolean hit = false;

			for (int i = 1; i <= DAGGER_THROW_RANGE; i++) {
				Location mLoc = (loc.clone()).add((newDir.clone()).multiply(i));
				Location pLoc = mLoc.clone();

				for (int t = 0; t < 10; t++) {
					pLoc.add((newDir.clone()).multiply(0.1));
					world.spawnParticle(Particle.REDSTONE, pLoc, 1);
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mLoc, 1)) {
					EntityUtils.damageEntity(mPlugin, mob, damage, player);
					mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, DAGGER_THROW_DURATION, vulnLevel, true, false));

					hit = true;
				}

				if (mLoc.getBlock().getType().isSolid() || hit) {
					mLoc.subtract((newDir.clone()).multiply(0.5));
					world.spawnParticle(Particle.SWEEP_ATTACK, mLoc, 3, 0.3, 0.3, 0.3, 0.1);

					if (hit) {
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);
					}

					break;
				}
			}
		}

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.0f);
		return true;
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		info.linkedSpell = Spells.DAGGER_THROW;
		info.scoreboardId = "DaggerThrow";
		info.cooldown = DAGGER_THROW_COOLDOWN;
		info.trigger = AbilityTrigger.RIGHT_CLICK;
		return info;
	}
	
	@Override
	public boolean runCheck(Player player) {
		if (player.isSneaking()) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) 
				return true;
		}
		return false;
	}

}
