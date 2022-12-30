package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
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
import org.jetbrains.annotations.Nullable;

public class Stasis extends ZeroArgumentEffect {
	public static final String effectID = "Stasis";

	public static final String GENERIC_NAME = "Stasis";

	public Stasis(int duration) {
		super(duration, effectID);
	}

	protected Stasis(int duration, String effectID) {
		super(duration, effectID);
	}

	// Most functionality handled in StasisListener

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			player.addScoreboardTag(Constants.Tags.STASIS);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getDuration(), 9));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, getDuration(), 4));
			player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, getDuration(), 1));
			player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1, 1.2f);
			AbilityManager.getManager().updateSilence(player, true);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			entity.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
		}
		if (fourHertz) {
			Location loc = entity.getLocation();
			new PartialParticle(Particle.END_ROD, loc, 45, 1, 1, 1, .00000001).spawnAsEntityActive(entity);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			entity.removeScoreboardTag(Constants.Tags.STASIS);
			AbilityManager.getManager().updatePlayerAbilities(player, false); // also updates silence
		}
	}

	public static Stasis deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new Stasis(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Stasis";
	}

	@Override
	public String toString() {
		return String.format("Stasis duration:%d", this.getDuration());
	}

}


