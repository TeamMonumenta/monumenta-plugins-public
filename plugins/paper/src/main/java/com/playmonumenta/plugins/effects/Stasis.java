package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.abilities.AbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Stasis extends Effect {

	private static int mDuration;
	public static String STASIS_NAME = "Stasis";

	public Stasis(int duration) {
		super(duration);
		Stasis.mDuration = duration;
	}

	// Most functionality handled in StasisListener

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			player.addScoreboardTag(Constants.Tags.STASIS);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mDuration, 100));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, mDuration, 100));
			player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, mDuration, 100));
			(player.getLocation().getWorld()).playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1, 1.2f);
			AbilityManager.getManager().getPlayerAbilities(player).silence();
		}

	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			entity.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
		}
		Location loc = entity.getLocation();
		World world = loc.getWorld();
		if (fourHertz) {
			world.spawnParticle(Particle.END_ROD, loc, 45, 1, 1, 1, .00000001);
		}

	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player) {
			entity.removeScoreboardTag(Constants.Tags.STASIS);
			AbilityManager.getManager().updatePlayerAbilities((Player) entity);
			AbilityManager.getManager().getPlayerAbilities((Player) entity).unsilence();
		}
	}

	@Override
	public String toString() {
		return String.format("Stasis duration:%d", this.getDuration());
	}

}


