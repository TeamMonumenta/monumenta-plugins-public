package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Stasis extends Effect {
	public static final String effectID = "Stasis";

	public static final String GENERIC_NAME = "Stasis";

	public static final String SPEED_EFFECT_NAME = "StasisSpeedEffect";

	private final boolean mDoEffects;

	public Stasis(int duration) {
		this(duration, true);
	}

	public Stasis(int duration, boolean doEffects) {
		this(duration, doEffects, effectID);
	}

	protected Stasis(int duration, String effectID) {
		this(duration, true, effectID);
	}

	public Stasis(int duration, boolean doEffects, String effectID) {
		super(duration, effectID);
		mDoEffects = doEffects;
	}

	// Most functionality handled in StasisListener

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			if (mDoEffects) {
				player.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
				player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, getDuration(), 1));
				player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1, 1.2f);
			}
			Plugin.getInstance().mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(getDuration(), -1, SPEED_EFFECT_NAME).displays(false));
			AbilityManager.getManager().updateSilence(player, true);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (!mDoEffects) {
			return;
		}
		if (oneHertz) {
			entity.sendActionBar(Component.text("You are in stasis! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
		}
		if (fourHertz) {
			Location loc = entity.getLocation();
			new PartialParticle(Particle.END_ROD, loc, 10, 1, 1, 1, .00000001).spawnAsEntityActive(entity);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			AbilityManager.getManager().updatePlayerAbilities(player, false); // also updates silence
		}
		Plugin.getInstance().mEffectManager.clearEffects(entity, SPEED_EFFECT_NAME);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("doEffects", mDoEffects);

		return object;
	}

	public static Stasis deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		boolean doEffects = true;
		if (object.has("doEffects")) {
			doEffects = object.get("doEffects").getAsBoolean();
		}

		return new Stasis(duration, doEffects);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Stasis";
	}

	@Override
	public String toString() {
		return String.format("Stasis duration:%d", this.getDuration());
	}

}


