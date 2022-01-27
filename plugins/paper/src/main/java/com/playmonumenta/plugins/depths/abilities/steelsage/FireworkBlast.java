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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireworkBlast extends DepthsAbility {
	public static final String ABILITY_NAME = "Firework Blast";
	private static final String ABILITY_METAKEY = "FireworkBlastMetakey";
	private static final int COOLDOWN = 12 * 20;
	private static final int[] DAMAGE = {16, 20, 24, 28, 32, 40};
	private static final int[] DAMAGE_CAP = {32, 40, 48, 56, 64, 80};
	private static final double DAMAGE_INCREASE_PER_BLOCK = 0.1;
	private static final int RADIUS = 4;

	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(244, 56, 0), 1.0f);
	private static final Particle.DustOptions ORANGE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 127, 20), 1.0f);

	public FireworkBlast(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.FIREWORKBLAST;
		mDisplayItem = Material.FIREWORK_ROCKET;
		mTree = DepthsTree.METALLIC;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();

		Firework rocket = (Firework)mPlayer.getWorld().spawnEntity(mPlayer.getLocation().add(0, 1.3, 0), EntityType.FIREWORK);
		rocket.setShooter(mPlayer);
		rocket.setShotAtAngle(true);
		rocket.setMetadata(ABILITY_METAKEY, new FixedMetadataValue(mPlugin, null));

		Vector vel = mPlayer.getLocation().getDirection();
		rocket.setVelocity(vel);

		FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.BLACK, Color.WHITE, Color.RED).withFade(Color.GRAY, Color.SILVER, Color.ORANGE).build();
		FireworkMeta meta = rocket.getFireworkMeta();
		meta.addEffect(effect);
		meta.setPower(3);

		rocket.setFireworkMeta(meta);

		mPlugin.mProjectileEffectTimers.addEntity(rocket, Particle.SMOKE_NORMAL);

		ProjectileLaunchEvent event = new ProjectileLaunchEvent(rocket);
		Bukkit.getPluginManager().callEvent(event);
		rocket.setVelocity(rocket.getVelocity().multiply(2));

		FixedMetadataValue playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsMetadata(mPlayer);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (rocket.isDead() || !rocket.isValid() || rocket.getVelocity().equals(new Vector(0, 0, 0))) {
					Location loc = rocket.getLocation();
					World world = rocket.getWorld();

					for (LivingEntity e : EntityUtils.getNearbyMobs(rocket.getLocation(), RADIUS)) {

						//Deals an extra 5% for every block traveled (distance from player to firework)
						//Also does 33% less damage for every block from the firework to the enemy after 1 block, minimum damage 0
						double damage = (mPlayer.getLocation().distance(loc) * DAMAGE_INCREASE_PER_BLOCK + 1) * DAMAGE[mRarity - 1];

						//Max damage cap from array
						DamageEvent damageEvent = new DamageEvent(e, mPlayer, mPlayer, DamageType.PROJECTILE_SKILL, mInfo.mLinkedSpell, Math.min(damage, DAMAGE_CAP[mRarity - 1]));
						damageEvent.setDelayed(true);
						damageEvent.setPlayerItemStat(playerItemStats);
						DamageUtils.damage(damageEvent, false, true, null);

						world.spawnParticle(Particle.SMOKE_LARGE, e.getLocation(), 20, 0, 0, 0, 0.2);
						world.spawnParticle(Particle.REDSTONE, e.getLocation(), 10, 0.25, 0.25, 0.25, GRAY_COLOR);
						world.spawnParticle(Particle.REDSTONE, e.getLocation(), 5, 0.25, 0.25, 0.25, RED_COLOR);
						world.spawnParticle(Particle.REDSTONE, e.getLocation(), 5, 0.25, 0.25, 0.25, ORANGE_COLOR);
						world.playSound(e.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 2);
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public static boolean isDamaging(Firework fw) {
		return fw.hasMetadata(ABILITY_METAKEY);
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking and holding a weapon to shoot a firework that deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " projectile damage to enemies within " + RADIUS + " blocks of its explosion. The damage is increased by " + (int) DepthsUtils.roundPercent(DAMAGE_INCREASE_PER_BLOCK) + "% for every block the firework travels, up to " + DepthsUtils.getRarityColor(rarity) + DAMAGE_CAP[rarity - 1] + ChatColor.WHITE + " damage. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}
