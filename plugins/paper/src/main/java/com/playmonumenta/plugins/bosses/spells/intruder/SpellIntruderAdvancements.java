package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SpellIntruderAdvancements extends Spell {
	private final LivingEntity mBoss;
	private final List<Player> mDistortedPlayers = new ArrayList<>();
	public boolean mShadowscapeDuo = true;

	private int mKilledShadowCount = 0;
	private final List<Player> mPlayers;
	private boolean mHasLuciae;
	private boolean mGrantedFacelessDozen = false;

	public SpellIntruderAdvancements(LivingEntity boss) {
		mBoss = boss;
		mPlayers = IntruderBoss.playersInRange(boss.getLocation());
	}

	@Override
	public void run() {
		if (mPlayers.size() != 2 || mPlayers.stream()
			.anyMatch(player -> AbilityUtils.getClassNum(player) != Rogue.CLASS_ID)) {
			mShadowscapeDuo = false;
		}
	}

	public void bossStarted() {
		mHasLuciae = checkLuciae();
		if (mHasLuciae) {
			LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "AntumbralStalker");
			mBoss.addScoreboardTag(IntruderBoss.STALKER_ACTIVE_TAG);
		}
	}

	public void bossDefeated() {
		grantDistortedPlayers();
		// Grants completion by exiting the arena
		if (mShadowscapeDuo) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/intruder/shadowscape_duo");
		}
		if (mPlayers.size() == 1) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/intruder/solo_intruder");
		}
		if (mHasLuciae) {
			// Grant Insidious Tribute
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/intruder/insidious_tribute");
		}
	}

	public void addDistortedPlayer(Player player) {
		mDistortedPlayers.add(player);
	}

	public void grantDistortedPlayers() {
		mDistortedPlayers.stream()
			.filter(player -> player.isValid() && !player.getScoreboardTags().contains(IntruderBoss.DEAD_TAG))
			.forEach(player ->
				// Grant Brainwashing
				AdvancementUtils.grantAdvancement(player, "monumenta:challenges/r3/intruder/brainwashing"));
	}

	public void checkFacelessOnes() {
		if (mGrantedFacelessDozen) {
			return;
		}
		long facelessOneCount = EntityUtils.getNearbyMobs(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE)
			.stream().filter(entity -> entity.getScoreboardTags().contains("FacelessOne")).count();
		if (facelessOneCount >= 18) {
			mGrantedFacelessDozen = true;
			// Grant Faceless Crowd
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/intruder/faceless_dozen");
		}
	}

	public boolean checkLuciae() {
		return mPlayers.stream().anyMatch(player ->
			Arrays.stream(player.getInventory().getContents()).anyMatch(itemStack -> ItemUtils.getPlainName(itemStack).equals("Twisted Luciae"))
		);
	}

	public void bossNearbyEntityDeath(EntityDeathEvent event) {
		if (event.getEntity().getScoreboardTags().contains("LiminalShadow")) {
			mKilledShadowCount++;
		}
		if (mKilledShadowCount >= 25) {
			// Grant Lights On
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/intruder/lights_on");
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
