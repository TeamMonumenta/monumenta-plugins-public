package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

public class JudgementChainMobEffect extends Effect {
	public static final String effectID = "JudgementChainMobEffect";

	private final Player mPlayer;
	private final String mModifierName;
	private final Team mChainTeam;

	public JudgementChainMobEffect(int duration, Player player, String source) {
		super(duration, effectID);
		mPlayer = player;
		mModifierName = source;
		mChainTeam = ScoreboardUtils.getExistingTeamOrCreate("chainColor", NamedTextColor.DARK_GRAY);
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.addAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(mModifierName, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}

		// Only change glowing color if:
		// mob not in a team
		if (Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getUniqueId().toString()) == null) {
			mChainTeam.addEntry(entity.getUniqueId().toString());
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.removeAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, mModifierName);
		}

		// Revert glowing
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mChainTeam.hasEntry(entity.getUniqueId().toString())) {
					mChainTeam.removeEntry(entity.getUniqueId().toString());
				}
			}
		}.runTaskLater(Plugin.getInstance(), 10);

	}

	@Override
	public void onHurt(@NotNull LivingEntity entity, @NotNull DamageEvent event) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		List<LivingEntity> e = EntityUtils.getNearbyMobs(entity.getLocation(), 8, entity, true);
		e.remove(entity);
		if (!e.isEmpty()) {
			event.setDamage(0);
		}
	}

	@Override
	public void onDamage(@NotNull LivingEntity entity, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (enemy instanceof Player) {
			if (enemy != mPlayer) {
				event.setDamage(0);
			} else {
				if (event.getType() == DamageEvent.DamageType.TRUE) {
					return;
				}
				event.setDamage(event.getDamage() / 2);
			}
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		PotionUtils.applyPotion(mPlayer, (LivingEntity) entity,
			new PotionEffect(PotionEffectType.GLOWING, 6, 0, true, false));
	}

	@Override
	public String toString() {
		return String.format("JudgementChainMobEffect duration:%d", this.getDuration());
	}
}
