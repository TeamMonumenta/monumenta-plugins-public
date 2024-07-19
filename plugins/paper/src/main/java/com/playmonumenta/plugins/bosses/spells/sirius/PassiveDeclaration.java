package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationAurora;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationBarrage;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationDamage;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationMobs;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationPoints;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclarationTp;
import com.playmonumenta.plugins.bosses.spells.sirius.declaration.DeclerationTuulen;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class PassiveDeclaration extends Spell {
	public static final int COOLDOWN = 17 * 20;
	public boolean mSwapping;
	public List<Spell> mDeclerations;
	public Sirius mSirius;
	public boolean mTp;
	private boolean mOnCooldown;
	private boolean mFirstRun;
	private final Plugin mPlugin;
	private @Nullable Spell mLastSpell;
	public boolean mTpBlocked;

	public PassiveDeclaration(Plugin plugin, Sirius sirius, PassiveStarBlightConversion converter, SpellSummonTheStars spawner) {
		mSirius = sirius;
		mSwapping = false;
		mFirstRun = true;
		mTp = false;
		mTpBlocked = false;
		mDeclerations = new ArrayList<>();
		mPlugin = plugin;
		//Less common as they are more powerful
		mDeclerations.add(new DeclarationAurora(plugin, sirius.mSpawnLoc.clone().subtract(25, -2, 0), sirius));
		mDeclerations.add(new DeclerationTuulen(spawner, sirius, plugin));
		//Common Declarations
		mDeclerations.add(new DeclarationTp(plugin, sirius));
		mDeclerations.add(new DeclarationTp(plugin, sirius));
		mDeclerations.add(new DeclarationBarrage(plugin, sirius));
		mDeclerations.add(new DeclarationBarrage(plugin, sirius));
		mDeclerations.add(new DeclarationDamage(plugin, sirius, converter));
		mDeclerations.add(new DeclarationDamage(plugin, sirius, converter));
		mDeclerations.add(new DeclarationMobs(spawner, sirius));
		mDeclerations.add(new DeclarationMobs(spawner, sirius));
		mDeclerations.add(new DeclarationPoints(mSirius, plugin, converter));
		mDeclerations.add(new DeclarationPoints(mSirius, plugin, converter));
		//mDeclerations.add(new DeclerationTemp(mSirius));
		mOnCooldown = false;
		mLastSpell = null;
		mTpBlocked = false;
	}


	@Override
	public void run() {
		if (mFirstRun) {
			mFirstRun = false;
			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), this::run, 2 * 20);
			return;
		}
		if (!mOnCooldown && !mSirius.mCheeseLock) {
			mTp = false;
			for (Player p : mSirius.getPlayers()) {
				p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.2f, 0.1f);
				p.playSound(p, Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.4f, 0.1f);
				p.playSound(p, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 2f, 0.1f);
				p.playSound(p, Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 0.2f, 0.1f);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.2f, 0.7f);
				}, 4);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.2f, 1.1f);
					p.playSound(p, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 2f, 0.7f);
				}, 8);
			}
			mOnCooldown = true;
			mSwapping = false;
			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN);
			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mSwapping = true, COOLDOWN - 4 * 20);
			Spell declaration = FastUtils.getRandomElement(mDeclerations);
			if (mLastSpell == null) {
				//force the first cast to be points to help people understand the mechanic
				declaration = mDeclerations.get(11);
				mLastSpell = mDeclerations.get(0);
			}
			while (mLastSpell.getClass().equals(declaration.getClass())
					|| (declaration.getClass().equals(DeclarationDamage.class) && mSirius.mDamagePhase)
					|| (mTpBlocked && declaration.getClass().equals(DeclarationTp.class))) {
				declaration = FastUtils.getRandomElement(mDeclerations);
			}
			if (declaration.getClass() == DeclarationTp.class) {
				mTp = true;
			}
			declaration.run();
			mLastSpell = declaration;
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}


}
