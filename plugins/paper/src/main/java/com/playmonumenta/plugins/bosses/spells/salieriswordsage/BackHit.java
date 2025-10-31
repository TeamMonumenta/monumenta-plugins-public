package com.playmonumenta.plugins.bosses.spells.salieriswordsage;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BackHit extends Spell {
	private final LivingEntity mBoss;
	private final List<Player> mWarned = new ArrayList<>();
	private final double mDamageMultiplier;

	public BackHit(Plugin plugin, LivingEntity boss, double damageMultiplier) {
		mBoss = boss;
		mDamageMultiplier = damageMultiplier;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(plugin, 0, 20 * 5);
	}

	@Override
	public void run() {

	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	//If the player is facing away from the boss when hit, take more damage and play effect
	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && entityBehindPlayer(mBoss, player) && event.getType() == DamageEvent.DamageType.MELEE_SKILL) {
			World world = mBoss.getWorld();

			event.setFlatDamage(event.getDamage() * mDamageMultiplier);

			if (!mWarned.contains(player)) {
				MessagingUtils.sendNPCMessage(player, "Salieri", Component.text("You're being careless, never leave your back exposed.", NamedTextColor.DARK_RED));
				mWarned.add(player);
			}
			world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_STEP, SoundCategory.HOSTILE, 3, 1.3f);
			new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 20).delta(0.3, 0.3, 0.3).data(Bukkit.createBlockData(Material.REDSTONE_BLOCK)).spawnAsBoss();
			new PartialParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 20).delta(0.3, 0.3, 0.3).extra(0.5).spawnAsBoss();
		}
	}

	//Determines if player is facing away from entity
	private static boolean entityBehindPlayer(Entity entity, Player player) {
		double yaw = 2 * Math.PI - Math.PI * player.getLocation().getYaw() / 180;
		Vector v = entity.getLocation().toVector().subtract(player.getLocation().toVector());
		Vector r = new Vector(Math.sin(yaw), 0, Math.cos(yaw));
		float theta = r.angle(v);
		return (Math.PI / 2 < theta && theta < 3 * Math.PI / 2);
	}
}
