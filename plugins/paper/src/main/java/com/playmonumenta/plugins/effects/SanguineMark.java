package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SanguineMark extends Effect {
	public static final String effectID = "SanguineMark";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(179, 0, 0), 1.0f);
	private double mHealPercent;
	private Player mPlayer;
	private Plugin mPlugin;

	public SanguineMark(double healPercent, int duration, Player player, Plugin plugin) {
		super(duration, effectID);
		mHealPercent = healPercent;
		mPlayer = player;
		mPlugin = plugin;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			Location loc = entity.getLocation().add(0, 1, 0);
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02).spawnAsEnemyBuff();
			new PartialParticle(Particle.CRIMSON_SPORE, loc, 4, 0.25, 0.5, 0.25, 0).spawnAsEnemyBuff();
			new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsEnemyBuff();
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != null) {
			Player player = event.getEntity().getKiller();
			double maxHealth = EntityUtils.getMaxHealth(player);
			PlayerUtils.healPlayer(mPlugin, player, mHealPercent * maxHealth, mPlayer);
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.0f, 0.8f);
		}
	}

	@Override
	public String toString() {
		return String.format("SanguineMark duration:%d", this.getDuration());
	}
}
