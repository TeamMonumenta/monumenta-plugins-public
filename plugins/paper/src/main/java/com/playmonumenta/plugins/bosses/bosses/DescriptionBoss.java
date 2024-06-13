package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.libraryofsouls.bestiary.BestiaryManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DescriptionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_description";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Required. LoS name of the mob whose description you want to send.")
		public String LOS_NAME = "";
		@BossParam(help = "radius in which players can receive the description")
		public int RANGE = 20;
		@BossParam(help = "number of kills needed to stop seeing the description")
		public int KILL_REQ = 1;
		@BossParam(help = "whether to send description on spawn (disable to only show description when player hits the mob)")
		public boolean ON_SPAWN = true;
		@BossParam(help = "whether to send description on hurt (should always be true)")
		public boolean ON_HURT = true;
	}

	private final Parameters mParams;
	private boolean mSentOnce;

	public DescriptionBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		mSentOnce = false;

		if (mParams.ON_SPAWN) {
			sendDescription();
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.RANGE, null);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (mParams.ON_HURT && damager instanceof Player player && player.getLocation().distance(mBoss.getLocation()) < mParams.RANGE) {
			sendDescription();
		}
	}

	public void sendDescription() {
		if (mSentOnce) {
			return;
		}

		SoulEntry soul = SoulsDatabase.getInstance().getSoul(mParams.LOS_NAME);
		if (soul != null) {
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mParams.RANGE, true);
			for (Player player : players) {
				if (BestiaryManager.getKillsForMob(player, soul) < mParams.KILL_REQ) {
					List<Component> description = LibraryOfSoulsAPI.getDescription(mParams.LOS_NAME);
					// only once per tick, as we don't want it to send twice if spawner spawns multiple mobs
					if (description != null && MetadataUtils.checkOnceThisTick(mPlugin, player, "MobDescriptionFor" + player.getName())) {
						player.sendMessage(Component.text("New mob encountered! ", NamedTextColor.GREEN, TextDecoration.ITALIC)
							.append(Component.text(mBoss.getName(), NamedTextColor.GREEN, TextDecoration.BOLD))
							.append(Component.text(":", NamedTextColor.GREEN)));
						Component message = Component.empty();
						Iterator<Component> iter = description.iterator();
						while (iter.hasNext()) {
							message = message.append(iter.next().color(NamedTextColor.GREEN));
							if (iter.hasNext()) {
								message = message.appendNewline();
							}
						}
						player.sendMessage(message);
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 1f, 1f);
						mSentOnce = true;
					}
				}
			}
		}
	}
}