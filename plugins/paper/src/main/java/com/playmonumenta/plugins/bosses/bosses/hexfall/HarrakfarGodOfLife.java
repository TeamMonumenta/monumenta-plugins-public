package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SequentialSpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellBlueSummonAds;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellBlueUnchain;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellEssenceWave;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellGenerateBlueSpells;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellHarrakfarRecover;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellHyceneaDialogue;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;

public class HarrakfarGodOfLife extends BossAbilityGroup implements Listener {

	private final SequentialSpellManager mSpellQueue;
	private final Plugin mMonumentaPlugin;
	private final LivingEntity mBlue;
	private final Location mSpawnLoc;
	private final Parameters mParameters;
	public static final String identityTag = "boss_harrakfar";
	public static final int detectionRange = 36;
	public static final int mHealth = 75000;

	public static class Parameters extends BossParameters {
		public boolean FIRST = true;
	}

	public HarrakfarGodOfLife(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mMonumentaPlugin = plugin;
		mBlue = boss;
		mSpawnLoc = boss.getLocation();
		mParameters = BossParameters.getParameters(boss, identityTag, new Parameters());

		EntityUtils.setAttributeBase(mBlue, Attribute.GENERIC_MAX_HEALTH, mHealth);
		mBlue.setHealth(mHealth);

		mSpellQueue = new SequentialSpellManager(getActiveSpellsByPhase());

		Map<Integer, BossBarManager.BossHealthAction> blueEvents = new HashMap<>();

		blueEvents.put(50, mBoss -> {
			mSpellQueue.clearSpellQueue();
			mSpellQueue.addSpellToQueue(new SpellGenerateBlueSpells(this));
		});

		BossBarManager bossBar = new BossBarManager(boss, detectionRange * 2, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_6, blueEvents, true);

		super.constructBoss(mSpellQueue, getPassiveSpells(), detectionRange * 2, bossBar, 100, 1);
	}

	public void addSpellsToQueue() {
		for (Spell spell : getActiveSpellsByPhase()) {
			mSpellQueue.addSpellToQueue(spell);
		}
	}

	private List<Spell> getActiveSpellsByPhase() {
		List<Spell> activeSpells = new ArrayList<>();

		if (mParameters.FIRST) {
			activeSpells.add(new SpellBlueUnchain(mMonumentaPlugin, mBlue, detectionRange, 60 * 20, 0, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("Ye know not what... you’ve done. My consort... he feels my pain... he sees my rage... every scrap of my... bark that you tear allows him to buck at his chains...", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("Amour... allende... pour mai...", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("I feasted from the blood... of the Hex for centuries... the council knew not, but as I laid at the base of the tree... and I drank deep of their vile magics... all for you, my Wolf.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("Harrakfar... My love. My Lord. I beg of thee. Rise. Be Free.", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
		} else {
			activeSpells.add(new SpellBlueUnchain(mMonumentaPlugin, mBlue, detectionRange, 60 * 20, 0, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("Something is... wrong... my love... speak to me...", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("This viciousness... my Lord, I beg thee call out. Cry out and take... comfort in my... presence. You are free!", NamedTextColor.WHITE), 0, mSpawnLoc, true));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellHyceneaDialogue(Component.text("Harrakfar seems to roar only louder...", NamedTextColor.GRAY, TextDecoration.ITALIC), 0, mSpawnLoc, false));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
			activeSpells.add(new SpellEssenceWave(mMonumentaPlugin, mBlue, 8 * 20, 120, 10 * 20, 20, 50, 0.75f, mSpawnLoc));
		}

		activeSpells.add(new SpellGenerateBlueSpells(this));
		return activeSpells;
	}

	private List<Spell> getPassiveSpells() {
		List<Spell> passiveSpells = new ArrayList<>();
		passiveSpells.add(new SpellHarrakfarRecover(mSpawnLoc, mBlue));
		passiveSpells.add(new SpellBlueSummonAds(mMonumentaPlugin, mSpawnLoc, 10, 5 * 20, 4));
		return passiveSpells;
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (mBlue.getHealth() / mHealth <= 0.5) {
			event.setCancelled(true);
		}

		if (mBlue.getHealth() - event.getDamage() <= (double) mHealth / 2) {
			event.setCancelled(true);
			mBlue.setHealth((double) mHealth / 2);
		}
	}
}
