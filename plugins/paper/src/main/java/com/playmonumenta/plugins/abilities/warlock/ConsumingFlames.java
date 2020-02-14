package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class ConsumingFlames extends Ability {

	private static final int CONSUMING_FLAMES_1_RADIUS = 5;
	private static final int CONSUMING_FLAMES_2_RADIUS = 7;
	private static final int CONSUMING_FLAMES_DAMAGE = 1;
	private static final int CONSUMING_FLAMES_DURATION = 7 * 20;
	private static final int CONSUMING_FLAMES_COOLDOWN = 10 * 20;

	public ConsumingFlames(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Consuming Flames");
		mInfo.scoreboardId = "ConsumingFlames";
		mInfo.linkedSpell = Spells.CONSUMING_FLAMES;
		mInfo.cooldown = CONSUMING_FLAMES_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		Location loc = player.getLocation();
		World world = player.getWorld();
		int consumingFlames = getAbilityScore();
		int radius = (consumingFlames == 1) ? CONSUMING_FLAMES_1_RADIUS : CONSUMING_FLAMES_2_RADIUS;

		new BukkitRunnable() {
			double r = 0;
			Location loc = mPlayer.getLocation();
			@Override
			public void run() {
				r += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					loc.add(Math.cos(radian1) * r, 0.15, Math.sin(radian1) * r);
					mWorld.spawnParticle(Particle.FLAME, loc, 2, 0, 0, 0, 0.125);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 3, 0, 0, 0, 0.15);
					loc.subtract(Math.cos(radian1) * r, 0.15, Math.sin(radian1) * r);
				}

				if (r >= radius + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 30, 0, 0, 0, 0.15);
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);
		boolean effect = false;

		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), radius, mPlayer)) {
			PotionUtils.applyPotion(player, mob, new PotionEffect(PotionEffectType.WEAKNESS, CONSUMING_FLAMES_DURATION, 0, false, true));
			EntityUtils.applyFire(mPlugin, CONSUMING_FLAMES_DURATION, mob);

			EntityUtils.damageEntity(mPlugin, mob, CONSUMING_FLAMES_DAMAGE, player, MagicType.DARK_MAGIC);
			effect = true;
		}

		if (consumingFlames > 1 && effect) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, CONSUMING_FLAMES_DURATION, 0, false, true));
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())
		       && mPlayer.getLocation().getPitch() < 50;
	}

}
