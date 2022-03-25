package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Stasis extends ZeroArgumentEffect {

	public static final String GENERIC_NAME = "Stasis";

	public Stasis(int duration) {
		super(duration);
	}

	// Most functionality handled in StasisListener

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			player.addScoreboardTag(Constants.Tags.STASIS);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getDuration(), 100));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, getDuration(), 100));
			player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, getDuration(), 100));
			player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1, 1.2f);
			AbilityManager.getManager().getPlayerAbilities(player).silence();
		}

	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			entity.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
		}
		if (fourHertz) {
			Location loc = entity.getLocation();
			new PartialParticle(Particle.END_ROD, loc, 45, 1, 1, 1, .00000001).spawnAsEntityBuff(entity);
		}

	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			entity.removeScoreboardTag(Constants.Tags.STASIS);
			AbilityManager.getManager().updatePlayerAbilities(player);
			AbilityManager.getManager().getPlayerAbilities(player).unsilence();
		}
	}

	@Override
	public String toString() {
		return String.format("Stasis duration:%d", this.getDuration());
	}

}


