package com.playmonumenta.plugins.abilities.warlock;


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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class ConsumingFlames extends Ability {

	private static final int CONSUMING_FLAMES_RADIUS = 8;
	private static final int CONSUMING_FLAMES_1_DAMAGE = 1;
	private static final int CONSUMING_FLAMES_2_DAMAGE = 8;
	private static final int CONSUMING_FLAMES_DURATION = 7 * 20;
	private static final int CONSUMING_FLAMES_COOLDOWN = 10 * 20;

	private final int mDamage;

	public ConsumingFlames(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Consuming Flames");
		mInfo.scoreboardId = "ConsumingFlames";
		mInfo.mShorthandName = "CF";
		mInfo.mDescriptions.add("Sneaking and right-clicking while not looking down while holding a scythe knocks back and ignites mobs within 8 blocks of you for 7s, additionally dealing 1 damage. Amplifying Hex now counts fire as a debuff, and levels of inferno as extra debuff levels. (Cooldown: 10s)");
		mInfo.mDescriptions.add("The damage is increased to 8, and also afflict mobs with Weakness I.");
		mInfo.linkedSpell = Spells.CONSUMING_FLAMES;
		mInfo.cooldown = CONSUMING_FLAMES_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? CONSUMING_FLAMES_1_DAMAGE : CONSUMING_FLAMES_2_DAMAGE;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getLocation();

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

				if (r >= CONSUMING_FLAMES_RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 30, 0, 0, 0, 0.15);
		mWorld.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.35f);
		mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CONSUMING_FLAMES_RADIUS, mPlayer)) {
			EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.DARK_MAGIC, true, mInfo.linkedSpell);
			EntityUtils.applyFire(mPlugin, CONSUMING_FLAMES_DURATION, mob, mPlayer);

			if (getAbilityScore() > 1) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, CONSUMING_FLAMES_DURATION, 0, false, true));
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())
		       && mPlayer.getLocation().getPitch() < 50;
	}

}
