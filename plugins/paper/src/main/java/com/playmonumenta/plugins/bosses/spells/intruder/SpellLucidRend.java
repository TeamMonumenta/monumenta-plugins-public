package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellLucidRend extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final IntruderBoss.Dialogue mDialogue;
	private final List<LivingEntity> mLucidRends = new ArrayList<>();

	public SpellLucidRend(Plugin plugin, LivingEntity boss, IntruderBoss.Dialogue dialogue) {
		mPlugin = plugin;
		mBoss = boss;
		mDialogue = dialogue;
	}

	@Override
	public void run() {
		mDialogue.dialogue(0, List.of("I. WILL. CARVE MY WAY OUT.", "IF I MUST."));

		new BukkitRunnable() {
			int mSpawned = 0;

			@Override
			public void run() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 1.0f, 0.1f + mSpawned / 10f, 36);
				LivingEntity lucidRend = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(LocationUtils.randomLocationInCircle(mBoss.getLocation().add(0, 2, 0), 2), "LucidRend"));
				mLucidRends.add(lucidRend);
				EntityUtils.setRemoveEntityOnUnload(lucidRend);
				mSpawned++;
				if (mSpawned >= 3) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	public void killLucidRends() {
		mLucidRends.forEach(entity -> entity.setHealth(0));
		mLucidRends.clear();
	}
}
