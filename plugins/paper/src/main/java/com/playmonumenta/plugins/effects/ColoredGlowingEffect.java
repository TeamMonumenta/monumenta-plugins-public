package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ColoredGlowingEffect extends Effect {
	public static final long UPDATE_DELAY = 3600000 / 50;

	@MonotonicNonNull
	private static BukkitRunnable task;

	public static void registerCleanerTask(Plugin plugin) {
		task = new BukkitRunnable() {
			@Override
			public void run() {
				ColoredTeam.cleanTeams();
			}
		};
		task.runTaskTimer(plugin, 0, UPDATE_DELAY);
	}

	public static final String effectID = "ColoredGlowingEffect";

	@Nullable
	public final ColoredTeam mTeam;

	@Nullable
	public Team mOldTeam = null;
	//if it is -1 it was either not initialized or the entity did not have the glowing effect before.
	public int mOldEffectSpoilTicks = -1;

	public ColoredGlowingEffect(int duration, @Nullable NamedTextColor color) {
		super(duration, effectID);
		mTeam = ColoredTeam.getFromColor(color);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity.isGlowing()) {
			Team entityTeam = ScoreboardUtils.getEntityTeam(entity);

			if (!(entity instanceof Item)) {
				PotionEffect oldGlowing = ((LivingEntity) entity).getPotionEffect(PotionEffectType.GLOWING);
				if (oldGlowing != null) {
					mOldEffectSpoilTicks = oldGlowing.getDuration();
				}
			}

			if (entityTeam != null) {
				mOldTeam = entityTeam;
			}
		}

		if (entity instanceof Item) {
			entity.setGlowing(true);
		} else {
			PotionUtils.applyPotion(null, (LivingEntity) entity, new PotionEffect(PotionEffectType.GLOWING, getDuration(), 0));
		}
		if (mTeam != null) {
			mTeam.addToTeam(entity);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (shouldRemoveEffect()) {
			entity.setGlowing(false);
		}

		if (mOldEffectSpoilTicks != -1 && mOldEffectSpoilTicks - getDuration() > 0) {
			PotionUtils.applyPotion(null, (LivingEntity) entity, new PotionEffect(PotionEffectType.GLOWING, mOldEffectSpoilTicks - getDuration(), 0));
		}

		if (mTeam != null) {
			mTeam.removeFromTeam(entity);
			if (mOldTeam != null) {
				mOldTeam.addEntry(entity.getUniqueId().toString());
			}
		}
	}

	private boolean shouldRemoveEffect() {
		if (mOldEffectSpoilTicks == -1) {
			return false;
		}
		//means it will spoil
		int postEffectTicks = mOldEffectSpoilTicks - getDuration();
		return postEffectTicks <= 0;
	}

	@Override
	public String toString() {
		return String.format("Glowing duration:%d color:%s", this.getDuration(), mTeam != null ? mTeam.mColor : "none");
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = super.serialize();

		if (mTeam != null) {
			object.addProperty("team", mTeam.toString());
		}
		if (mOldEffectSpoilTicks != -1) {
			object.addProperty("spoil", mOldEffectSpoilTicks);
		}
		if (mOldTeam != null) {
			object.addProperty("oldTeam", mOldTeam.getName());
		}

		return object;
	}

	public static ColoredGlowingEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		ColoredTeam team = ColoredTeam.valueOf(object.get("team").getAsString());
		//use the color as the team names could actually change in between versions.
		ColoredGlowingEffect effect = new ColoredGlowingEffect(duration, team.mColor);
		if (object.has("oldTeam")) {
			effect.mOldTeam = ScoreboardUtils.getExistingTeam(object.get("oldTeam").getAsString());
		}
		if (object.has("spoil")) {
			effect.mOldEffectSpoilTicks = object.get("spoil").getAsInt();
		}

		return effect;
	}

	public enum ColoredTeam {
		DARK_RED("GlowingDarkRed", NamedTextColor.DARK_RED),
		RED("GlowingRed", NamedTextColor.RED),
		GOLD("GlowingGold", NamedTextColor.GOLD),
		YELLOW("GlowingYellow", NamedTextColor.YELLOW),
		DARK_GREEN("GlowingDarkGreen", NamedTextColor.DARK_GREEN),
		GREEN("GlowingGreen", NamedTextColor.GREEN),
		AQUA("GlowingAqua", NamedTextColor.AQUA),
		DARK_AQUA("GlowingDarkAqua", NamedTextColor.DARK_AQUA),
		DARK_BLUE("GlowingDarkBlue", NamedTextColor.DARK_BLUE),
		BLUE("GlowingBlue", NamedTextColor.BLUE),
		LIGHT_PURPLE("GlowingLightPurple", NamedTextColor.LIGHT_PURPLE),
		DARK_PURPLE("GlowingDarkPurple", NamedTextColor.DARK_PURPLE),
		GRAY("GlowingGray", NamedTextColor.GRAY),
		DARK_GRAY("GlowingDarkGray", NamedTextColor.DARK_GRAY),
		BLACK("GlowingBlack", NamedTextColor.BLACK);

		final String mTeamName;
		final NamedTextColor mColor;

		ColoredTeam(String teamName, NamedTextColor color) {
			mTeamName = teamName;
			mColor = color;
		}

		public Team loadTeam() {
			Team team = ScoreboardUtils.getExistingTeamOrCreate(mTeamName);
			team.color(mColor);
			return team;
		}

		public void addToTeam(Entity entity) {
			loadTeam().addEntry(entity.getUniqueId().toString());
		}

		public void removeFromTeam(Entity entity) {
			Team team = ScoreboardUtils.getExistingTeam(mTeamName);
			if (team == null) {
				return;
			}
			if (team.hasEntry(entity.getUniqueId().toString())) {
				team.removeEntry(entity.getUniqueId().toString());
			}
		}

		public void clean() {
			Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(mTeamName);
			if (team != null) {
				team.unregister();
			}
		}

		public void cleanTeamMembers() {
			Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(mTeamName);
			if (team == null) {
				return;
			}

			for (String entry : team.getEntries()) {
				try {
					Entity entity = Bukkit.getEntity(UUID.fromString(entry));
					if (entity == null || !entity.isValid()) {
						team.removeEntry(entry);
						continue;
					}

					if (!EffectManager.getInstance().hasEffect(entity, ColoredGlowingEffect.class)) {
						team.removeEntry(entry);
					}
				} catch (Exception e) {
					Plugin.getInstance().getLogger().info("Caught error trying to get an entity: " + e);
				}
			}
		}

		public static void cleanTeams() {
			for (ColoredTeam team : values()) {
				team.cleanTeamMembers();
			}
		}

		@Nullable
		public static ColoredTeam getFromColor(@Nullable NamedTextColor color) {
			if (color == null) {
				return null;
			}

			for (ColoredTeam team : values()) {
				if (team.mColor == color) {
					return team;
				}
			}

			return null;
		}
	}
}
