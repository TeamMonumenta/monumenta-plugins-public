package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

public class JudgementChainMobEffect extends Effect {
	public static final String effectID = "JudgementChainMobEffect";

	private final Player mPlayer;
	private final Team mChainTeam;

	public JudgementChainMobEffect(int duration, Player player, Team team) {
		super(duration, effectID);
		mPlayer = player;
		mChainTeam = team;
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		// Only change glowing color if:
		// mob not in a team
		if (Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getUniqueId().toString()) == null) {
			mChainTeam.addEntry(entity.getUniqueId().toString());
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mChainTeam.hasEntry(entity.getUniqueId().toString())) {
			mChainTeam.removeEntry(entity.getUniqueId().toString());
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
