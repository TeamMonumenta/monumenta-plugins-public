package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class HungeringVortex extends Ability {
	private static final int HUNGERING_VORTEX_DURATION = 8 * 20;
	private static final int HUNGERING_VORTEX_COOLDOWN = 18 * 20;
	private static final int HUNGERING_VORTEX_RADIUS = 7;
	private static final int HUNGERING_VORTEX_1_WEAKNESS_AMPLIFIER = 0;
	private static final int HUNGERING_VORTEX_2_WEAKNESS_AMPLIFIER = 1;
	private static final double HUNGERING_VORTEX_1_EXTRA_DAMAGE = 0.5;
	private static final double HUNGERING_VORTEX_2_EXTRA_DAMAGE = 1;

	/*
	 * Hungering Vortex: Shift + right click looking down pulls
	 * all mobs in a 7-block radius towards you, afflicting them
	 * with Weakness I / II for 8 s and increasing your melee
	 * damage by 0.5 / 1 for each affected enemy, up to a maximum
	 * of 4 / 8 for 8s. Cooldown: 18 s
	 */

	public HungeringVortex(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "HungeringVortex";
		mInfo.linkedSpell = Spells.HUNGERING_VORTEX;
		mInfo.cooldown = HUNGERING_VORTEX_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		int vortex = getAbilityScore();
		int weakness = vortex == 1 ? HUNGERING_VORTEX_1_WEAKNESS_AMPLIFIER : HUNGERING_VORTEX_2_WEAKNESS_AMPLIFIER;
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1, 1.25f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0.75f);
		Location loc = mPlayer.getLocation();
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 200, 3.5, 3.5, 3.5, 1);
		float velocity = loc.getBlock().isLiquid() ? 0.2f : 0.3f;
		/*
		 * Creates a fast-spiraling helix.
		 */
		new BukkitRunnable() {
			double rotation = 0;
			double radius = HUNGERING_VORTEX_RADIUS;

			@Override
			public void run() {
				for (int j = 0; j < 5; j++) {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(rotation + (72 * i));
						loc.add(Math.cos(radian1) * radius, 0.5, Math.sin(radian1) * radius);
						mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
						mPlayer.getWorld().spawnParticle(Particle.PORTAL, loc, 5, 0.1, 0.1, 0.1, 0);
						loc.subtract(Math.cos(radian1) * radius, 0.5, Math.sin(radian1) * radius);
					}
					rotation += 8;
					radius -= 0.25;
					if (radius <= 0) {
						this.cancel();
						return;
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		List<Mob> mobs = EntityUtils.getNearbyMobs(loc, HUNGERING_VORTEX_RADIUS);
		for (LivingEntity mob : mobs) {
			mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, HUNGERING_VORTEX_DURATION, weakness));
			MovementUtils.PullTowards(mPlayer, mob, velocity);
		}

		double damageInc = vortex == 1 ? HUNGERING_VORTEX_1_EXTRA_DAMAGE : HUNGERING_VORTEX_2_EXTRA_DAMAGE;
		double extra_dam = mobs.size() * damageInc;
		if (extra_dam > 4 * vortex) {
			extra_dam = 4 * vortex;
		}

		//Fire Note: I'd recommend we find some sort of workaround this. I'm always iffy on changing player Attributes.
		//The reason for that is because we may end up screwing up their attributes and forget to reset them.
		mPlayer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1 + extra_dam);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				if (t >= HUNGERING_VORTEX_DURATION || mPlayer.isDead()) {
					this.cancel();
					mPlayer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1);
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "The power of your Vortex fades away...");
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > 50 &&
		       (mainHand == null || mainHand.getType() != Material.BOW) &&
		       (offHand == null || offHand.getType() != Material.BOW) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());

	}

}
