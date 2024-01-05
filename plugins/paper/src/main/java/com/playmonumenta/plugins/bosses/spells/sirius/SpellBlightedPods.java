package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellBlightedPods extends Spell {

	private Sirius mSirius;
	private Plugin mPlugin;
	private boolean mOnCooldown;
	private int mPodCount;
	private static final int BASEHEALTH = 750;
	private static final int HPSCALEPERPLAYER = 125;
	private static final int COOLDOWN = 25 * 20;
	private static final int DURATION = 15 * 20;
	private static final int FLIGHTTIME = 1 * 20;
	private static final List<String> MINIBOSSES = List.of(
		"ProcyonsGazer",
		"ArcturussBeast",
		"VegasMonstrosity"
	);
	//always spawns 1
	private static final int PLAYERSPERPOD = 4;

	public SpellBlightedPods(Sirius sirius, Plugin plugin) {
		mSirius = sirius;
		mPlugin = plugin;
		mOnCooldown = false;
	}

	@Override
	public void run() {
		mPodCount = 0;
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		for (Player p : mSirius.getPlayersInArena(false)) {
			p.playSound(p, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.5f, 0.4f);
			p.playSound(p, Sound.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 2f, 0.6f);
			p.playSound(p, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2f, 0.1f);
			p.playSound(p, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 0.6f, 0.6f);
			p.playSound(p, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 1.5f, 0.7f);
		}
		for (int i = 0; i < mSirius.getPlayersInArena(false).size() / PLAYERSPERPOD + 1; i++) {
			pod(mSirius.getValidLoc());
		}
		new BukkitRunnable() {
			int mTicks = 0;
			ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DURATION,
				Component.text("Incubating Pods", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				mBar.nextTick();
				if (mPodCount == 0 || mTicks > DURATION) {
					mBar.remove();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void pod(Location loc) {
		mPodCount++;
		BlockDisplay mDisplay = mSirius.mBoss.getWorld().spawn(loc, BlockDisplay.class);
		mDisplay.setTransformation(new Transformation(new Vector3f(0, 20, 0), new AxisAngle4f(), new Vector3f(1, 2, 1), new AxisAngle4f()));
		mDisplay.setInterpolationDelay(-1);
		mDisplay.setBlock(Bukkit.createBlockData(Material.WARPED_WART_BLOCK));
		mDisplay.setInterpolationDuration(FLIGHTTIME);
		mDisplay.addScoreboardTag("SiriusDisplay");

		new BukkitRunnable() {
			Team mGold = ScoreboardUtils.getExistingTeamOrCreate("gold");
			int mTicks = 0;
			@Nullable
			LivingEntity mHitBox = null;
			boolean mIncrease = true;

			@Override
			public void run() {
				//Interpolation doesnt take effect till 1 tick later.
				if (mTicks == 1) {
					mDisplay.setInterpolationDelay(-1);
					mDisplay.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), mDisplay.getTransformation().getScale(), new AxisAngle4f()));
					mDisplay.setInterpolationDelay(-1);
				}
				if (mTicks == FLIGHTTIME + 1) {
					int count = mSirius.getPlayersInArena(false).size();
					int hp = 250 + 75 * count;
					new PPExplosion(Particle.SCRAPE, loc).delta(2).count(25).spawnAsBoss();
					mHitBox = (LivingEntity) LibraryOfSoulsIntegration.summon(loc.clone().add(0.75, 0, 0.75), "BlightedPod");
					if (mHitBox != null) {
						Objects.requireNonNull(mHitBox.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(hp);
						mHitBox.setHealth(hp);
						mHitBox.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999999, 1, true, false));
						mGold.addEntity(mHitBox);
						mHitBox.setGlowing(true);
					}
					mDisplay.setInterpolationDuration(20);
					World world = mDisplay.getWorld();
					Location loc = mDisplay.getLocation();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.4f, 1.2f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 1.4f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 2f);

				}
				if (mTicks >= FLIGHTTIME + 1 && mTicks % 20 == 0) {
					if (mIncrease) {
						mDisplay.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(1.5f, 2.5f, 1.5f), new AxisAngle4f()));
					} else {
						mDisplay.setTransformation(new Transformation(new Vector3f(0.25f, 0, 0.25f), new AxisAngle4f(), new Vector3f(1, 2, 1), new AxisAngle4f()));
					}
					mIncrease = !mIncrease;
					mDisplay.setInterpolationDelay(-1);
				}
				if (mHitBox != null) {
					if (mHitBox.isDead()) {
						new PPExplosion(Particle.REDSTONE, mHitBox.getLocation()).count(25).delta(2.5).data(new Particle.DustOptions(Color.fromRGB(0, 130, 130), 1f)).spawnAsBoss();
						mDisplay.remove();
						this.cancel();
					}
				}
				if (mTicks >= DURATION + FLIGHTTIME) {
					if (mHitBox != null) {
						new PPExplosion(Particle.REDSTONE, mHitBox.getLocation()).count(50).delta(1.5f).data(new Particle.DustOptions(Color.fromRGB(3, 135, 126), 2.0f)).spawnAsBoss();
						World world = mHitBox.getWorld();
						Location loc = mHitBox.getLocation();
						mSirius.mStarBlightConverter.convertPartialSphere(3, loc);
						world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 0.3f, 1.2f);
						world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.7f, 1.4f);
						world.playSound(loc, Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 0.7f, 0.4f);
						world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 0.7f, 1f);
						spawn(mHitBox);
						if (mSirius.mBlocks <= 10) {
							spawn(mHitBox);
						}
					}
					this.cancel();
				}
				if (mSirius.mBoss.isDead()) {
					this.cancel();
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				if (mHitBox != null) {
					mHitBox.remove();
				}
				mPodCount--;
				mDisplay.remove();
				super.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void spawn(LivingEntity hitbox) {
		double currentHP = hitbox.getHealth();
		double maxHP = Objects.requireNonNull(hitbox.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
		LivingEntity boss = (LivingEntity) LibraryOfSoulsIntegration.summon(hitbox.getLocation(), FastUtils.getRandomElement(MINIBOSSES));
		if (boss != null) {
			int maxHealth = BASEHEALTH + mSirius.getPlayersInArena(false).size() * HPSCALEPERPLAYER;
			Objects.requireNonNull(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
			boss.setHealth(maxHealth / 2.0f + (maxHealth / 2.0f) * (currentHP / maxHP)); //half of the hp is effected by damage done to the pod
			boss.addScoreboardTag(Sirius.MOB_TAG);
		}
	}
}
