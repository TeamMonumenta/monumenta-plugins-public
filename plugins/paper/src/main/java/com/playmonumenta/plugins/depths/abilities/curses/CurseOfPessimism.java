package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

public class CurseOfPessimism extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Pessimism";
	public static final double HEALTH_THRESHOLD = 0.5;
	public static final double SPEED = 0.5;
	public static final String SPEED_EFFECT_NAME = "CurseOfPessimismSpeedEffect";

	public static final DepthsAbilityInfo<CurseOfPessimism> INFO =
		new DepthsAbilityInfo<>(CurseOfPessimism.class, ABILITY_NAME, CurseOfPessimism::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.RED_GLAZED_TERRACOTTA)
			.descriptions(CurseOfPessimism::getDescription);

	private boolean mActive = false;

	public CurseOfPessimism(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Healing is common enough that it's probably more efficient (and accurate enough) to calculate here
		boolean wasActive = mActive;
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
		if (party == null) {
			return;
		}
		mActive = party.getPlayers().stream().filter(Objects::nonNull).filter(p -> p != mPlayer).anyMatch(p -> p.getHealth() < HEALTH_THRESHOLD * EntityUtils.getMaxHealth(p));

		if (wasActive != mActive) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		if (!mActive) {
			return;
		}
		double remainingHealth = EntityUtils.getMaxHealth(mPlayer) * HEALTH_THRESHOLD - mPlayer.getHealth();
		if (remainingHealth < 0) {
			event.setCancelled(true);
			return;
		}
		if (event.getAmount() > remainingHealth) {
			event.setAmount(remainingHealth);
		}
	}

	@Override
	public void playerGainAbsorptionEvent(EntityGainAbsorptionEvent event) {
		if (mActive) {
			event.setCancelled(true);
		}
	}

	private static Description<CurseOfPessimism> getDescription() {
		return new DescriptionBuilder<CurseOfPessimism>()
			.add("While any other member of your party is below ")
			.addPercent(HEALTH_THRESHOLD)
			.add(" health, you cannot heal above ")
			.addPercent(HEALTH_THRESHOLD)
			.add(" health or gain absorption.");
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
