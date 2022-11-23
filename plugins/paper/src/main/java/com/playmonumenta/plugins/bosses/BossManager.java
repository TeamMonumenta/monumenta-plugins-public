package com.playmonumenta.plugins.bosses;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.*;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeDaggerCraftingBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTargetNPCBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTurretBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.DropShardBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.LavaCannonBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayBookSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayDemonSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayGolemSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayScarabSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GraySummoned;
import com.playmonumenta.plugins.bosses.bosses.lich.LichAlchBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichClericBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichConquestBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichCurseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichDemiseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichJudgementBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichKeyGlowBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichMageBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichRogueBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichScoutBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichShieldBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichStrifeBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarlockBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarriorBoss;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.delves.mobabilities.TwistedMiniBoss;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.bosses.GalleryMobRisingBoss;
import com.playmonumenta.plugins.gallery.bosses.GallerySummonMobBoss;
import com.playmonumenta.plugins.parrots.RainbowParrot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

public class BossManager implements Listener {

	/********************************************************************************
	 * Classes/Interfaces
	 *******************************************************************************/

	@FunctionalInterface
	public interface StatelessBossConstructor {
		BossAbilityGroup construct(Plugin plugin, LivingEntity entity) throws Exception;
	}

	@FunctionalInterface
	public interface StatefulBossConstructor {
		BossAbilityGroup construct(Plugin plugin, LivingEntity entity, Location spawnLoc, Location endLoc);
	}

	@FunctionalInterface
	public interface BossDeserializer {
		BossAbilityGroup deserialize(Plugin plugin, LivingEntity entity) throws Exception;
	}

	/********************************************************************************
	 * Static Fields
	 *******************************************************************************/

	/*
	 * Holds a static reference to the most recently instantiated boss manager
	 */
	private static @Nullable BossManager INSTANCE = null;

	private static final Map<String, StatelessBossConstructor> mStatelessBosses;
	private static final Map<String, StatefulBossConstructor> mStatefulBosses;
	private static final Map<String, BossDeserializer> mBossDeserializers;
	public static final Map<String, BossParameters> mBossParameters;

	static {
		/* Stateless bosses are those that have no end location set where a redstone block would be spawned when they die */
		mStatelessBosses = new HashMap<String, StatelessBossConstructor>();
		mStatelessBosses.put(BlockLockBoss.identityTag, (Plugin p, LivingEntity e) -> new BlockLockBoss(p, e));
		mStatelessBosses.put(ImmortalPassengerBoss.identityTag, (Plugin p, LivingEntity e) -> new ImmortalPassengerBoss(p, e));
		mStatelessBosses.put(ChestLockBoss.identityTag, (Plugin p, LivingEntity e) -> new ChestLockBoss(p, e));
		mStatelessBosses.put(UnyieldingBoss.identityTag, (Plugin p, LivingEntity e) -> new UnyieldingBoss(p, e));
		mStatelessBosses.put(ToughBoss.identityTag, (Plugin p, LivingEntity e) -> new ToughBoss(p, e));
		mStatelessBosses.put(UnseenBoss.identityTag, (Plugin p, LivingEntity e) -> new UnseenBoss(p, e));
		mStatelessBosses.put(WhispersBoss.identityTag, (Plugin p, LivingEntity e) -> new WhispersBoss(p, e));
		mStatelessBosses.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> new GenericBoss(p, e));
		mStatelessBosses.put(HiddenBoss.identityTag, (Plugin p, LivingEntity e) -> new HiddenBoss(p, e));
		mStatelessBosses.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> new InvisibleBoss(p, e));
		mStatelessBosses.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> new FireResistantBoss(p, e));
		mStatelessBosses.put(HungerCloudBoss.identityTag, (Plugin p, LivingEntity e) -> new HungerCloudBoss(p, e));
		mStatelessBosses.put(BlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> new BlockBreakBoss(p, e));
		mStatelessBosses.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> new PulseLaserBoss(p, e));
		mStatelessBosses.put(ArcaneLaserBoss.identityTag, (Plugin p, LivingEntity e) -> new ArcaneLaserBoss(p, e));
		mStatelessBosses.put(WeaponSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> new WeaponSwitchBoss(p, e));
		mStatelessBosses.put(ShieldSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> new ShieldSwitchBoss(p, e));
		mStatelessBosses.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> new ChargerBoss(p, e));
		mStatelessBosses.put(BlastResistBoss.identityTag, (Plugin p, LivingEntity e) -> new BlastResistBoss(p, e));
		mStatelessBosses.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> new InfestedBoss(p, e));
		mStatelessBosses.put(FireballBoss.identityTag, (Plugin p, LivingEntity e) -> new FireballBoss(p, e));
		mStatelessBosses.put(TpBehindBoss.identityTag, (Plugin p, LivingEntity e) -> new TpBehindBoss(p, e));
		mStatelessBosses.put(TpBehindTargetedBoss.identityTag, (Plugin p, LivingEntity e) -> new TpBehindTargetedBoss(p, e));
		mStatelessBosses.put(TpSwapBoss.identityTag, (Plugin p, LivingEntity e) -> new TpSwapBoss(p, e));
		mStatelessBosses.put(FlameNovaBoss.identityTag, (Plugin p, LivingEntity e) -> new FlameNovaBoss(p, e));
		mStatelessBosses.put(PlayerTargetBoss.identityTag, (Plugin p, LivingEntity e) -> new PlayerTargetBoss(p, e));
		mStatelessBosses.put(DamageReducedBoss.identityTag, (Plugin p, LivingEntity e) -> new DamageReducedBoss(p, e));
		mStatelessBosses.put(WinterSnowmanEventBoss.identityTag, (Plugin p, LivingEntity e) -> new WinterSnowmanEventBoss(p, e));
		mStatelessBosses.put(TrainingDummyBoss.identityTag, (Plugin p, LivingEntity e) -> new TrainingDummyBoss(p, e));
		mStatelessBosses.put(FestiveTesseractSnowmanBoss.identityTag, (Plugin p, LivingEntity e) -> new FestiveTesseractSnowmanBoss(p, e));
		mStatelessBosses.put(CrowdControlImmunityBoss.identityTag, (Plugin p, LivingEntity e) -> new CrowdControlImmunityBoss(p, e));
		mStatelessBosses.put(FloatBoss.identityTag, (Plugin p, LivingEntity e) -> new FloatBoss(p, e));
		mStatelessBosses.put(FrostNovaBoss.identityTag, (Plugin p, LivingEntity e) -> new FrostNovaBoss(p, e));
		mStatelessBosses.put(DebuffHitBoss.identityTag, (Plugin p, LivingEntity e) -> new DebuffHitBoss(p, e));
		mStatelessBosses.put(IceAspectBoss.identityTag, (Plugin p, LivingEntity e) -> new IceAspectBoss(p, e));
		mStatelessBosses.put(TsunamiChargerBoss.identityTag, (Plugin p, LivingEntity e) -> new TsunamiChargerBoss(p, e));
		mStatelessBosses.put(BombTossBoss.identityTag, (Plugin p, LivingEntity e) -> new BombTossBoss(p, e));
		mStatelessBosses.put(BombTossNoBlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> new BombTossNoBlockBreakBoss(p, e));
		mStatelessBosses.put(RejuvenationBoss.identityTag, (Plugin p, LivingEntity e) -> new RejuvenationBoss(p, e));
		mStatelessBosses.put(HandSwapBoss.identityTag, (Plugin p, LivingEntity e) -> new HandSwapBoss(p, e));
		mStatelessBosses.put(UnstableBoss.identityTag, (Plugin p, LivingEntity e) -> new UnstableBoss(p, e));
		mStatelessBosses.put(BerserkerBoss.identityTag, (Plugin p, LivingEntity e) -> new BerserkerBoss(p, e));
		mStatelessBosses.put(SnowballDamageBoss.identityTag, (Plugin p, LivingEntity e) -> new SnowballDamageBoss(p, e));
		mStatelessBosses.put(CorruptInfestedBoss.identityTag, (Plugin p, LivingEntity e) -> new CorruptInfestedBoss(p, e));
		mStatelessBosses.put(FlameLaserBoss.identityTag, (Plugin p, LivingEntity e) -> new FlameLaserBoss(p, e));
		mStatelessBosses.put(SpecterParticleBoss.identityTag, (Plugin p, LivingEntity e) -> new SpecterParticleBoss(p, e));
		mStatelessBosses.put(DreadnaughtParticleBoss.identityTag, (Plugin p, LivingEntity e) -> new DreadnaughtParticleBoss(p, e));
		mStatelessBosses.put(DreadlingBoss.identityTag, (Plugin p, LivingEntity e) -> new DreadlingBoss(p, e));
		mStatelessBosses.put(ProjectileDeflectionBoss.identityTag, (Plugin p, LivingEntity e) -> new ProjectileDeflectionBoss(p, e));
		mStatelessBosses.put(LivingBladeBoss.identityTag, (Plugin p, LivingEntity e) -> new LivingBladeBoss(p, e));
		mStatelessBosses.put(PrimordialElementalKaulBoss.identityTag, (Plugin p, LivingEntity e) -> new PrimordialElementalKaulBoss(p, e));
		mStatelessBosses.put(ImmortalElementalKaulBoss.identityTag, (Plugin p, LivingEntity e) -> new ImmortalElementalKaulBoss(p, e));
		mStatelessBosses.put(CyanSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new CyanSummonBoss(p, e));
		mStatelessBosses.put(WitherHitBoss.identityTag, (Plugin p, LivingEntity e) -> new WitherHitBoss(p, e));
		mStatelessBosses.put(VolatileBoss.identityTag, (Plugin p, LivingEntity e) -> new VolatileBoss(p, e));
		mStatelessBosses.put(SwapOnDismountBoss.identityTag, (Plugin p, LivingEntity e) -> new SwapOnDismountBoss(p, e));
		mStatelessBosses.put(PlayerDamageOnlyBoss.identityTag, (Plugin p, LivingEntity e) -> new PlayerDamageOnlyBoss(p, e));
		mStatelessBosses.put(GrayDemonSummoner.identityTag, (Plugin p, LivingEntity e) -> new GrayDemonSummoner(p, e));
		mStatelessBosses.put(GrayGolemSummoner.identityTag, (Plugin p, LivingEntity e) -> new GrayGolemSummoner(p, e));
		mStatelessBosses.put(GrayScarabSummoner.identityTag, (Plugin p, LivingEntity e) -> new GrayScarabSummoner(p, e));
		mStatelessBosses.put(GrayBookSummoner.identityTag, (Plugin p, LivingEntity e) -> new GrayBookSummoner(p, e));
		mStatelessBosses.put(GraySummoned.identityTag, (Plugin p, LivingEntity e) -> new GraySummoned(p, e));
		mStatelessBosses.put(IceBreakBoss.identityTag, (Plugin p, LivingEntity e) -> new IceBreakBoss(p, e));
		mStatelessBosses.put(PunchResistBoss.identityTag, (Plugin p, LivingEntity e) -> new PunchResistBoss(p, e));
		mStatelessBosses.put(HalloweenCreeperBoss.identityTag, (Plugin p, LivingEntity e) -> new HalloweenCreeperBoss(p, e));
		mStatelessBosses.put(NoExperienceBoss.identityTag, (Plugin p, LivingEntity e) -> new NoExperienceBoss(p, e));
		mStatelessBosses.put(FocusFireBoss.identityTag, (Plugin p, LivingEntity e) -> new FocusFireBoss(p, e));
		mStatelessBosses.put(ForceBoss.identityTag, (Plugin p, LivingEntity e) -> new ForceBoss(p, e));
		mStatelessBosses.put(AvengerBoss.identityTag, (Plugin p, LivingEntity e) -> new AvengerBoss(p, e));
		mStatelessBosses.put(RageBoss.identityTag, (Plugin p, LivingEntity e) -> new RageBoss(p, e));
		mStatelessBosses.put(EarthshakeBoss.identityTag, (Plugin p, LivingEntity e) -> new EarthshakeBoss(p, e));
		mStatelessBosses.put(MagicArrowBoss.identityTag, (Plugin p, LivingEntity e) -> new MagicArrowBoss(p, e));
		mStatelessBosses.put(SeekingProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> new SeekingProjectileBoss(p, e));
		mStatelessBosses.put(TrackingProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> new TrackingProjectileBoss(p, e));
		mStatelessBosses.put(WrathBoss.identityTag, (Plugin p, LivingEntity e) -> new WrathBoss(p, e));
		mStatelessBosses.put(LeapBoss.identityTag, (Plugin p, LivingEntity e) -> new LeapBoss(p, e));
		mStatelessBosses.put(BarrierBoss.identityTag, (Plugin p, LivingEntity e) -> new BarrierBoss(p, e));
		mStatelessBosses.put(CrowdControlResistanceBoss.identityTag, (Plugin p, LivingEntity e) -> new CrowdControlResistanceBoss(p, e));
		mStatelessBosses.put(MeteorSlamBoss.identityTag, (Plugin p, LivingEntity e) -> new MeteorSlamBoss(p, e));
		mStatelessBosses.put(SwingBoss.identityTag, (Plugin p, LivingEntity e) -> new SwingBoss(p, e));
		mStatelessBosses.put(MistMob.identityTag, (Plugin p, LivingEntity e) -> new MistMob(p, e));
		mStatelessBosses.put(HookBoss.identityTag, (Plugin p, LivingEntity e) -> new HookBoss(p, e));
		mStatelessBosses.put(FrostGiantIcicle.identityTag, (Plugin p, LivingEntity e) -> new FrostGiantIcicle(p, e));
		mStatelessBosses.put(SpellSlingerBoss.identityTag, (Plugin p, LivingEntity e) -> new SpellSlingerBoss(p, e));
		mStatelessBosses.put(VindictiveBoss.identityTag, (Plugin p, LivingEntity e) -> new VindictiveBoss(p, e));
		mStatelessBosses.put(ShadowTrailBoss.identityTag, (Plugin p, LivingEntity e) -> new ShadowTrailBoss(p, e));
		mStatelessBosses.put(KineticProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> new KineticProjectileBoss(p, e));
		mStatelessBosses.put(FlameTrailBoss.identityTag, (Plugin p, LivingEntity e) -> new FlameTrailBoss(p, e));
		mStatelessBosses.put(ShadeParticleBoss.identityTag, (Plugin p, LivingEntity e) -> new ShadeParticleBoss(p, e));
		mStatelessBosses.put(FireBombTossBoss.identityTag, (Plugin p, LivingEntity e) -> new FireBombTossBoss(p, e));
		mStatelessBosses.put(CommanderBoss.identityTag, (Plugin p, LivingEntity e) -> new CommanderBoss(p, e));
		mStatelessBosses.put(ShadePossessedBoss.identityTag, (Plugin p, LivingEntity e) -> new ShadePossessedBoss(p, e));
		mStatelessBosses.put(TwistedDespairBoss.identityTag, (Plugin p, LivingEntity e) -> new TwistedDespairBoss(p, e));
		mStatelessBosses.put(CoordinatedAttackBoss.identityTag, (Plugin p, LivingEntity e) -> new CoordinatedAttackBoss(p, e));
		mStatelessBosses.put(AbilitySilenceBoss.identityTag, (Plugin p, LivingEntity e) -> new AbilitySilenceBoss(p, e));
		mStatelessBosses.put(ShiftingBoss.identityTag, (Plugin p, LivingEntity e) -> new ShiftingBoss(p, e));
		mStatelessBosses.put(BulletHellBoss.identityTag, (Plugin p, LivingEntity e) -> new BulletHellBoss(p, e));
		mStatelessBosses.put(CarapaceBoss.identityTag, (Plugin p, LivingEntity e) -> new CarapaceBoss(p, e));
		mStatelessBosses.put(KamikazeBoss.identityTag, (Plugin p, LivingEntity e) -> new KamikazeBoss(p, e));
		mStatelessBosses.put(TinyBombTossBoss.identityTag, (Plugin p, LivingEntity e) -> new TinyBombTossBoss(p, e));
		mStatelessBosses.put(AntiRangeBoss.identityTag, (Plugin p, LivingEntity e) -> new AntiRangeBoss(p, e));
		mStatelessBosses.put(AntiRangeChivalrousBoss.identityTag, (Plugin p, LivingEntity e) -> new AntiRangeChivalrousBoss(p, e));
		mStatelessBosses.put(ImmortalMountBoss.identityTag, (Plugin p, LivingEntity e) -> new ImmortalMountBoss(p, e));
		mStatelessBosses.put(SilenceOnHitBoss.identityTag, (Plugin p, LivingEntity e) -> new SilenceOnHitBoss(p, e));
		mStatelessBosses.put(FalseSpiritPortal.identityTag, (Plugin p, LivingEntity e) -> new FalseSpiritPortal(p, e));
		mStatelessBosses.put(TffBookSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new TffBookSummonBoss(p, e));
		mStatelessBosses.put(ArcaneProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> new ArcaneProjectileBoss(p, e));
		mStatelessBosses.put(JumpBoss.identityTag, (Plugin p, LivingEntity e) -> new JumpBoss(p, e));
		mStatelessBosses.put(RebornBoss.identityTag, (Plugin p, LivingEntity e) -> new RebornBoss(p, e));
		mStatelessBosses.put(NoFireBoss.identityTag, (Plugin p, LivingEntity e) -> new NoFireBoss(p, e));
		mStatelessBosses.put(DistanceCloserBoss.identityTag, (Plugin p, LivingEntity e) -> new DistanceCloserBoss(p, e));
		mStatelessBosses.put(WeakHookBoss.identityTag, (Plugin p, LivingEntity e) -> new WeakHookBoss(p, e));
		mStatelessBosses.put(AuraEffectBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraEffectBoss(p, e));
		mStatelessBosses.put(DummyDecoyBoss.identityTag, (Plugin p, LivingEntity e) -> new DummyDecoyBoss(p, e));
		mStatelessBosses.put(LaserBoss.identityTag, (Plugin p, LivingEntity e) -> new LaserBoss(p, e));
		mStatelessBosses.put(SpinBoss.identityTag, (Plugin p, LivingEntity e) -> new SpinBoss(p, e));
		mStatelessBosses.put(OnHitBoss.identityTag, (Plugin p, LivingEntity e) -> new OnHitBoss(p, e));
		mStatelessBosses.put(NovaBoss.identityTag, (Plugin p, LivingEntity e) -> new NovaBoss(p, e));
		mStatelessBosses.put(ProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> new ProjectileBoss(p, e));
		mStatelessBosses.put(RainbowParrot.identityTag, (Plugin p, LivingEntity e) -> new RainbowParrot(p, e));
		mStatelessBosses.put(SpawnMobsBoss.identityTag, (Plugin p, LivingEntity e) -> new SpawnMobsBoss(p, e));
		mStatelessBosses.put(LandSlowBoss.identityTag, (Plugin p, LivingEntity e) -> new LandSlowBoss(p, e));
		mStatelessBosses.put(PounceBoss.identityTag, (Plugin p, LivingEntity e) -> new PounceBoss(p, e));
		mStatelessBosses.put(NoAbilityDamageBoss.identityTag, (Plugin p, LivingEntity e) -> new NoAbilityDamageBoss(p, e));
		mStatelessBosses.put(NoGlowingBoss.identityTag, (Plugin p, LivingEntity e) -> new NoGlowingBoss(p, e));
		mStatelessBosses.put(GenericTargetBoss.identityTag, (Plugin p, LivingEntity e) -> new GenericTargetBoss(p, e));
		mStatelessBosses.put(MobRisingBoss.identityTag, (Plugin p, LivingEntity e) -> new MobRisingBoss(p, e));
		mStatelessBosses.put(GrenadeLauncherBoss.identityTag, (Plugin p, LivingEntity e) -> new GrenadeLauncherBoss(p, e));
		mStatelessBosses.put(SizeChangerBoss.identityTag, (Plugin p, LivingEntity e) -> new SizeChangerBoss(p, e));
		mStatelessBosses.put(DelveScalingBoss.identityTag, (Plugin p, LivingEntity e) -> new DelveScalingBoss(p, e));
		mStatelessBosses.put(DeathSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new DeathSummonBoss(p, e));
		mStatelessBosses.put(SummonOnExplosionBoss.identityTag, (Plugin p, LivingEntity e) -> new SummonOnExplosionBoss(p, e));
		mStatelessBosses.put(HostileBoss.identityTag, (Plugin p, LivingEntity e) -> new HostileBoss(p, e));
		mStatelessBosses.put(StarfallBoss.identityTag, (Plugin p, LivingEntity e) -> new StarfallBoss(p, e));
		mStatelessBosses.put(ShatterBoss.identityTag, (Plugin p, LivingEntity e) -> new ShatterBoss(p, e));
		mStatelessBosses.put(StatMultiplierBoss.identityTag, (Plugin p, LivingEntity e) -> new StatMultiplierBoss(p, e));
		mStatelessBosses.put(SpectralSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new SpectralSummonBoss(p, e));
		mStatelessBosses.put(DreadfulSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new DreadfulSummonBoss(p, e));
		mStatelessBosses.put(FriendlyBoss.identityTag, (Plugin p, LivingEntity e) -> new FriendlyBoss(p, e));
		mStatelessBosses.put(ScoutVolleyBoss.identityTag, (Plugin p, LivingEntity e) -> new ScoutVolleyBoss(p, e));
		mStatelessBosses.put(MageCosmicMoonbladeBoss.identityTag, (Plugin p, LivingEntity e) -> new MageCosmicMoonbladeBoss(p, e));
		mStatelessBosses.put(WarriorShieldWallBoss.identityTag, (Plugin p, LivingEntity e) -> new WarriorShieldWallBoss(p, e));
		mStatelessBosses.put(DodgeBoss.identityTag, (Plugin p, LivingEntity e) -> new DodgeBoss(p, e));
		mStatelessBosses.put(BlockPlacerBoss.identityTag, (Plugin p, LivingEntity e) -> new BlockPlacerBoss(p, e));
		mStatelessBosses.put(ScoutVolleyBoss.identityTag, (Plugin p, LivingEntity e) -> new ScoutVolleyBoss(p, e));
		mStatelessBosses.put(WarlockAmpHexBoss.identityTag, (Plugin p, LivingEntity e) -> new WarlockAmpHexBoss(p, e));
		mStatelessBosses.put(LimitedLifespanBoss.identityTag, (Plugin p, LivingEntity e) -> new LimitedLifespanBoss(p, e));
		mStatelessBosses.put(BlueFireBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueFireBoss(p, e));
		mStatelessBosses.put(BlueEarthBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueEarthBoss(p, e));
		mStatelessBosses.put(BlueAirBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueAirBoss(p, e));
		mStatelessBosses.put(BlueWaterBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueWaterBoss(p, e));
		mStatelessBosses.put(DropShardBoss.identityTag, (Plugin p, LivingEntity e) -> new DropShardBoss(p, e));
		mStatelessBosses.put(BlueStrikeDaggerCraftingBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueStrikeDaggerCraftingBoss(p, e));
		mStatelessBosses.put(BlueStrikeTargetNPCBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueStrikeTargetNPCBoss(p, e));
		mStatelessBosses.put(BlueStrikeTurretBoss.identityTag, (Plugin p, LivingEntity e) -> new BlueStrikeTurretBoss(p, e));
		mStatelessBosses.put(LavaCannonBoss.identityTag, (Plugin p, LivingEntity e) -> new LavaCannonBoss(p, e));
		mStatelessBosses.put(SoundBoss.identityTag, (Plugin p, LivingEntity e) -> new SoundBoss(p, e));

		mStatelessBosses.put(LichMageBoss.identityTag, (Plugin p, LivingEntity e) -> new LichMageBoss(p, e));
		mStatelessBosses.put(LichRogueBoss.identityTag, (Plugin p, LivingEntity e) -> new LichRogueBoss(p, e));
		mStatelessBosses.put(LichClericBoss.identityTag, (Plugin p, LivingEntity e) -> new LichClericBoss(p, e));
		mStatelessBosses.put(LichWarlockBoss.identityTag, (Plugin p, LivingEntity e) -> new LichWarlockBoss(p, e));
		mStatelessBosses.put(LichAlchBoss.identityTag, (Plugin p, LivingEntity e) -> new LichAlchBoss(p, e));
		mStatelessBosses.put(LichScoutBoss.identityTag, (Plugin p, LivingEntity e) -> new LichScoutBoss(p, e));
		mStatelessBosses.put(LichWarriorBoss.identityTag, (Plugin p, LivingEntity e) -> new LichWarriorBoss(p, e));
		mStatelessBosses.put(LichConquestBoss.identityTag, (Plugin p, LivingEntity e) -> new LichConquestBoss(p, e));
		mStatelessBosses.put(LichDemiseBoss.identityTag, (Plugin p, LivingEntity e) -> new LichDemiseBoss(p, e));
		mStatelessBosses.put(LichJudgementBoss.identityTag, (Plugin p, LivingEntity e) -> new LichJudgementBoss(p, e));
		mStatelessBosses.put(LichStrifeBoss.identityTag, (Plugin p, LivingEntity e) -> new LichStrifeBoss(p, e));
		mStatelessBosses.put(LichCurseBoss.identityTag, (Plugin p, LivingEntity e) -> new LichCurseBoss(p, e));
		mStatelessBosses.put(LichShieldBoss.identityTag, (Plugin p, LivingEntity e) -> new LichShieldBoss(p, e));
		mStatelessBosses.put(LichKeyGlowBoss.identityTag, (Plugin p, LivingEntity e) -> new LichKeyGlowBoss(p, e));
		mStatelessBosses.put(FestiveTessUpgradeSnowmenBoss.identityTag, (Plugin p, LivingEntity e) -> new FestiveTessUpgradeSnowmenBoss(p, e));
		mStatelessBosses.put(MetalmancyBoss.identityTag, (Plugin p, LivingEntity e) -> new MetalmancyBoss(p, e));
		mStatelessBosses.put(RestlessSoulsBoss.identityTag, (Plugin p, LivingEntity e) -> new RestlessSoulsBoss(p, e));
		mStatelessBosses.put(AlchemicalAberrationBoss.identityTag, (Plugin p, LivingEntity e) -> new AlchemicalAberrationBoss(p, e));
		mStatelessBosses.put(ThrowSummonBoss.identityTag, (Plugin p, LivingEntity e) -> new ThrowSummonBoss(p, e));
		mStatelessBosses.put(PotionThrowBoss.identityTag, (Plugin p, LivingEntity e) -> new PotionThrowBoss(p, e));
		mStatelessBosses.put(TwistedMiniBoss.identityTag, (Plugin p, LivingEntity e) -> new TwistedMiniBoss(p, e));
		mStatelessBosses.put(BrownPositiveBoss.identityTag, (Plugin p, LivingEntity e) -> new BrownPositiveBoss(p, e));
		mStatelessBosses.put(BrownNegativeBoss.identityTag, (Plugin p, LivingEntity e) -> new BrownNegativeBoss(p, e));
		mStatelessBosses.put(BrownMagnetSwapBoss.identityTag, (Plugin p, LivingEntity e) -> new BrownMagnetSwapBoss(p, e));
		mStatelessBosses.put(ParadoxSwapBoss.identityTag, (Plugin p, LivingEntity e) -> new ParadoxSwapBoss(p, e));
		mStatelessBosses.put(TemporalShieldBoss.identityTag, (Plugin p, LivingEntity e) -> new TemporalShieldBoss(p, e));
		mStatelessBosses.put(GalleryMobRisingBoss.identityTag, (Plugin p, LivingEntity e) -> new GalleryMobRisingBoss(p, e));
		mStatelessBosses.put(GallerySummonMobBoss.identityTag, (Plugin p, LivingEntity e) -> new GallerySummonMobBoss(p, e));
		mStatelessBosses.put(TagScalingBoss.identityTag, (Plugin p, LivingEntity e) -> new TagScalingBoss(p, e));
		mStatelessBosses.put(CancelDamageBoss.identityTag, (Plugin p, LivingEntity e) -> new CancelDamageBoss(p, e));


		/* Stateful bosses have a remembered spawn location and end location where a redstone block is set when they die */
		mStatefulBosses = new HashMap<String, StatefulBossConstructor>();
		mStatefulBosses.put(CAxtal.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new CAxtal(p, e, s, l));
		mStatefulBosses.put(Masked.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Masked(p, e, s, l));
		mStatefulBosses.put(Virius.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Virius(p, e, s, l));
		mStatefulBosses.put(Orangyboi.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Orangyboi(p, e, s, l));
		mStatefulBosses.put(Azacor.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Azacor(p, e, s, l));
		mStatefulBosses.put(AzacorNormal.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new AzacorNormal(p, e, s, l));
		mStatefulBosses.put(CShura.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new CShura(p, e, s, l));
		mStatefulBosses.put(SwordsageRichter.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new SwordsageRichter(p, e, s, l));
		mStatefulBosses.put(Kaul.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Kaul(p, e, s, l));
		mStatefulBosses.put(TCalin.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new TCalin(p, e, s, l));
		mStatefulBosses.put(CrownbearerBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new CrownbearerBoss(p, e, s, l));
		mStatefulBosses.put(RabbitGodBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new RabbitGodBoss(p, e, s, l));
		mStatefulBosses.put(OldLabsBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new OldLabsBoss(p, e, s, l));
		mStatefulBosses.put(HeadlessHorsemanBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new HeadlessHorsemanBoss(p, e, s, l));
		mStatefulBosses.put(Varcosa.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Varcosa(p, e, s, l));
		mStatefulBosses.put(FrostGiant.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new FrostGiant(p, e, s, l));
		mStatefulBosses.put(TealQuestBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new TealQuestBoss(p, e, s, l));
		mStatefulBosses.put(VarcosaSummonerBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new VarcosaSummonerBoss(p, e, s, l));
		mStatefulBosses.put(VarcosasLastBreathBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new VarcosasLastBreathBoss(p, e, s, l));
		mStatefulBosses.put(VarcosaLingeringWillBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new VarcosaLingeringWillBoss(p, e, s, l));
		mStatefulBosses.put(MimicQueen.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new MimicQueen(p, e, s, l));
		mStatefulBosses.put(FalseSpirit.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new FalseSpirit(p, e, s, l));
		mStatefulBosses.put(SnowSpirit.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new SnowSpirit(p, e, s, l));
		mStatefulBosses.put(Lich.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Lich(p, e, s, l));
		mStatefulBosses.put(Hedera.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Hedera(p, e, s, l));
		mStatefulBosses.put(Davey.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Davey(p, e, s, l));
		mStatefulBosses.put(Nucleus.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Nucleus(p, e, s, l));
		mStatefulBosses.put(Ghalkor.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Ghalkor(p, e, s, l));
		mStatefulBosses.put(Svalgot.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Svalgot(p, e, s, l));
		mStatefulBosses.put(BeastOfTheBlackFlame.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new BeastOfTheBlackFlame(p, e, s, l));
		mStatefulBosses.put(RKitxet.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new RKitxet(p, e, s, l));
		mStatefulBosses.put(VerdantMinibossBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new VerdantMinibossBoss(p, e, s, l));
		mStatefulBosses.put(PortalBoss.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new PortalBoss(p, e, s, l));
		mStatefulBosses.put(ImperialConstruct.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new ImperialConstruct(p, e, s, l));
		mStatefulBosses.put(Samwell.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Samwell(p, e, s, l));
		mStatefulBosses.put(TealSpirit.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new TealSpirit(p, e, s, l));

		/* All bosses have a deserializer which gives the boss back their abilities when chunks re-load */
		mBossDeserializers = new HashMap<String, BossDeserializer>();
		mBossDeserializers.put(BlockLockBoss.identityTag, (Plugin p, LivingEntity e) -> BlockLockBoss.deserialize(p, e));
		mBossDeserializers.put(ImmortalPassengerBoss.identityTag, (Plugin p, LivingEntity e) -> ImmortalPassengerBoss.deserialize(p, e));
		mBossDeserializers.put(ChestLockBoss.identityTag, (Plugin p, LivingEntity e) -> ChestLockBoss.deserialize(p, e));
		mBossDeserializers.put(UnyieldingBoss.identityTag, (Plugin p, LivingEntity e) -> UnyieldingBoss.deserialize(p, e));
		mBossDeserializers.put(ToughBoss.identityTag, (Plugin p, LivingEntity e) -> ToughBoss.deserialize(p, e));
		mBossDeserializers.put(UnseenBoss.identityTag, (Plugin p, LivingEntity e) -> UnseenBoss.deserialize(p, e));
		mBossDeserializers.put(WhispersBoss.identityTag, (Plugin p, LivingEntity e) -> WhispersBoss.deserialize(p, e));
		mBossDeserializers.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> GenericBoss.deserialize(p, e));
		mBossDeserializers.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> InvisibleBoss.deserialize(p, e));
		mBossDeserializers.put(HiddenBoss.identityTag, (Plugin p, LivingEntity e) -> HiddenBoss.deserialize(p, e));
		mBossDeserializers.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> FireResistantBoss.deserialize(p, e));
		mBossDeserializers.put(HungerCloudBoss.identityTag, (Plugin p, LivingEntity e) -> HungerCloudBoss.deserialize(p, e));
		mBossDeserializers.put(BlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> BlockBreakBoss.deserialize(p, e));
		mBossDeserializers.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> PulseLaserBoss.deserialize(p, e));
		mBossDeserializers.put(ArcaneLaserBoss.identityTag, (Plugin p, LivingEntity e) -> ArcaneLaserBoss.deserialize(p, e));
		mBossDeserializers.put(WeaponSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> WeaponSwitchBoss.deserialize(p, e));
		mBossDeserializers.put(ShieldSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> ShieldSwitchBoss.deserialize(p, e));
		mBossDeserializers.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> ChargerBoss.deserialize(p, e));
		mBossDeserializers.put(BlastResistBoss.identityTag, (Plugin p, LivingEntity e) -> BlastResistBoss.deserialize(p, e));
		mBossDeserializers.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> InfestedBoss.deserialize(p, e));
		mBossDeserializers.put(FireballBoss.identityTag, (Plugin p, LivingEntity e) -> FireballBoss.deserialize(p, e));
		mBossDeserializers.put(TpBehindBoss.identityTag, (Plugin p, LivingEntity e) -> TpBehindBoss.deserialize(p, e));
		mBossDeserializers.put(TpBehindTargetedBoss.identityTag, (Plugin p, LivingEntity e) -> TpBehindTargetedBoss.deserialize(p, e));
		mBossDeserializers.put(TpSwapBoss.identityTag, (Plugin p, LivingEntity e) -> TpSwapBoss.deserialize(p, e));
		mBossDeserializers.put(FlameNovaBoss.identityTag, (Plugin p, LivingEntity e) -> FlameNovaBoss.deserialize(p, e));
		mBossDeserializers.put(PlayerTargetBoss.identityTag, (Plugin p, LivingEntity e) -> PlayerTargetBoss.deserialize(p, e));
		mBossDeserializers.put(DamageReducedBoss.identityTag, (Plugin p, LivingEntity e) -> DamageReducedBoss.deserialize(p, e));
		mBossDeserializers.put(WinterSnowmanEventBoss.identityTag, (Plugin p, LivingEntity e) -> WinterSnowmanEventBoss.deserialize(p, e));
		mBossDeserializers.put(TrainingDummyBoss.identityTag, (Plugin p, LivingEntity e) -> TrainingDummyBoss.deserialize(p, e));
		mBossDeserializers.put(FestiveTesseractSnowmanBoss.identityTag, (Plugin p, LivingEntity e) -> FestiveTesseractSnowmanBoss.deserialize(p, e));
		mBossDeserializers.put(CrowdControlImmunityBoss.identityTag, (Plugin p, LivingEntity e) -> CrowdControlImmunityBoss.deserialize(p, e));
		mBossDeserializers.put(FloatBoss.identityTag, (Plugin p, LivingEntity e) -> FloatBoss.deserialize(p, e));
		mBossDeserializers.put(FrostNovaBoss.identityTag, (Plugin p, LivingEntity e) -> FrostNovaBoss.deserialize(p, e));
		mBossDeserializers.put(DebuffHitBoss.identityTag, (Plugin p, LivingEntity e) -> DebuffHitBoss.deserialize(p, e));
		mBossDeserializers.put(IceAspectBoss.identityTag, (Plugin p, LivingEntity e) -> IceAspectBoss.deserialize(p, e));
		mBossDeserializers.put(CAxtal.identityTag, (Plugin p, LivingEntity e) -> CAxtal.deserialize(p, e));
		mBossDeserializers.put(Masked.identityTag, (Plugin p, LivingEntity e) -> Masked.deserialize(p, e));
		mBossDeserializers.put(Virius.identityTag, (Plugin p, LivingEntity e) -> Virius.deserialize(p, e));
		mBossDeserializers.put(Orangyboi.identityTag, (Plugin p, LivingEntity e) -> Orangyboi.deserialize(p, e));
		mBossDeserializers.put(Azacor.identityTag, (Plugin p, LivingEntity e) -> Azacor.deserialize(p, e));
		mBossDeserializers.put(AzacorNormal.identityTag, (Plugin p, LivingEntity e) -> AzacorNormal.deserialize(p, e));
		mBossDeserializers.put(CShura.identityTag, (Plugin p, LivingEntity e) -> CShura.deserialize(p, e));
		mBossDeserializers.put(TsunamiChargerBoss.identityTag, (Plugin p, LivingEntity e) -> TsunamiChargerBoss.deserialize(p, e));
		mBossDeserializers.put(BombTossBoss.identityTag, (Plugin p, LivingEntity e) -> BombTossBoss.deserialize(p, e));
		mBossDeserializers.put(BombTossNoBlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> BombTossNoBlockBreakBoss.deserialize(p, e));
		mBossDeserializers.put(RejuvenationBoss.identityTag, (Plugin p, LivingEntity e) -> RejuvenationBoss.deserialize(p, e));
		mBossDeserializers.put(HandSwapBoss.identityTag, (Plugin p, LivingEntity e) -> HandSwapBoss.deserialize(p, e));
		mBossDeserializers.put(UnstableBoss.identityTag, (Plugin p, LivingEntity e) -> UnstableBoss.deserialize(p, e));
		mBossDeserializers.put(BerserkerBoss.identityTag, (Plugin p, LivingEntity e) -> BerserkerBoss.deserialize(p, e));
		mBossDeserializers.put(SnowballDamageBoss.identityTag, (Plugin p, LivingEntity e) -> SnowballDamageBoss.deserialize(p, e));
		mBossDeserializers.put(CorruptInfestedBoss.identityTag, (Plugin p, LivingEntity e) -> CorruptInfestedBoss.deserialize(p, e));
		mBossDeserializers.put(FlameLaserBoss.identityTag, (Plugin p, LivingEntity e) -> FlameLaserBoss.deserialize(p, e));
		mBossDeserializers.put(SwordsageRichter.identityTag, (Plugin p, LivingEntity e) -> SwordsageRichter.deserialize(p, e));
		mBossDeserializers.put(SpecterParticleBoss.identityTag, (Plugin p, LivingEntity e) -> new SpecterParticleBoss(p, e));
		mBossDeserializers.put(DreadnaughtParticleBoss.identityTag, (Plugin p, LivingEntity e) -> new DreadnaughtParticleBoss(p, e));
		mBossDeserializers.put(DreadlingBoss.identityTag, (Plugin p, LivingEntity e) -> new DreadlingBoss(p, e));
		mBossDeserializers.put(ProjectileDeflectionBoss.identityTag, (Plugin p, LivingEntity e) -> new ProjectileDeflectionBoss(p, e));
		mBossDeserializers.put(LivingBladeBoss.identityTag, (Plugin p, LivingEntity e) -> LivingBladeBoss.deserialize(p, e));
		mBossDeserializers.put(Kaul.identityTag, (Plugin p, LivingEntity e) -> Kaul.deserialize(p, e));
		mBossDeserializers.put(PrimordialElementalKaulBoss.identityTag, (Plugin p, LivingEntity e) -> PrimordialElementalKaulBoss.deserialize(p, e));
		mBossDeserializers.put(ImmortalElementalKaulBoss.identityTag, (Plugin p, LivingEntity e) -> ImmortalElementalKaulBoss.deserialize(p, e));
		mBossDeserializers.put(TCalin.identityTag, (Plugin p, LivingEntity e) -> TCalin.deserialize(p, e));
		mBossDeserializers.put(CrownbearerBoss.identityTag, (Plugin p, LivingEntity e) -> CrownbearerBoss.deserialize(p, e));
		mBossDeserializers.put(CyanSummonBoss.identityTag, (Plugin p, LivingEntity e) -> CyanSummonBoss.deserialize(p, e));
		mBossDeserializers.put(WitherHitBoss.identityTag, (Plugin p, LivingEntity e) -> WitherHitBoss.deserialize(p, e));
		mBossDeserializers.put(VolatileBoss.identityTag, (Plugin p, LivingEntity e) -> VolatileBoss.deserialize(p, e));
		mBossDeserializers.put(PlayerDamageOnlyBoss.identityTag, (Plugin p, LivingEntity e) -> PlayerDamageOnlyBoss.deserialize(p, e));
		mBossDeserializers.put(RabbitGodBoss.identityTag, (Plugin p, LivingEntity e) -> RabbitGodBoss.deserialize(p, e));
		mBossDeserializers.put(GrayDemonSummoner.identityTag, (Plugin p, LivingEntity e) -> GrayDemonSummoner.deserialize(p, e));
		mBossDeserializers.put(GrayGolemSummoner.identityTag, (Plugin p, LivingEntity e) -> GrayGolemSummoner.deserialize(p, e));
		mBossDeserializers.put(GrayScarabSummoner.identityTag, (Plugin p, LivingEntity e) -> GrayScarabSummoner.deserialize(p, e));
		mBossDeserializers.put(GrayBookSummoner.identityTag, (Plugin p, LivingEntity e) -> GrayBookSummoner.deserialize(p, e));
		mBossDeserializers.put(GraySummoned.identityTag, (Plugin p, LivingEntity e) -> GraySummoned.deserialize(p, e));
		mBossDeserializers.put(IceBreakBoss.identityTag, (Plugin p, LivingEntity e) -> new IceBreakBoss(p, e));
		mBossDeserializers.put(PunchResistBoss.identityTag, (Plugin p, LivingEntity e) -> PunchResistBoss.deserialize(p, e));
		mBossDeserializers.put(OldLabsBoss.identityTag, (Plugin p, LivingEntity e) -> OldLabsBoss.deserialize(p, e));
		mBossDeserializers.put(HalloweenCreeperBoss.identityTag, (Plugin p, LivingEntity e) -> HalloweenCreeperBoss.deserialize(p, e));
		mBossDeserializers.put(HeadlessHorsemanBoss.identityTag, (Plugin p, LivingEntity e) -> HeadlessHorsemanBoss.deserialize(p, e));
		mBossDeserializers.put(NoExperienceBoss.identityTag, (Plugin p, LivingEntity e) -> NoExperienceBoss.deserialize(p, e));
		mBossDeserializers.put(FocusFireBoss.identityTag, (Plugin p, LivingEntity e) -> FocusFireBoss.deserialize(p, e));
		mBossDeserializers.put(ForceBoss.identityTag, (Plugin p, LivingEntity e) -> ForceBoss.deserialize(p, e));
		mBossDeserializers.put(AvengerBoss.identityTag, (Plugin p, LivingEntity e) -> AvengerBoss.deserialize(p, e));
		mBossDeserializers.put(RageBoss.identityTag, (Plugin p, LivingEntity e) -> RageBoss.deserialize(p, e));
		mBossDeserializers.put(Varcosa.identityTag, (Plugin p, LivingEntity e) -> Varcosa.deserialize(p, e));
		mBossDeserializers.put(VarcosaSummonerBoss.identityTag, (Plugin p, LivingEntity e) -> VarcosaSummonerBoss.deserialize(p, e));
		mBossDeserializers.put(VarcosasLastBreathBoss.identityTag, (Plugin p, LivingEntity e) -> VarcosasLastBreathBoss.deserialize(p, e));
		mBossDeserializers.put(VarcosaLingeringWillBoss.identityTag, (Plugin p, LivingEntity e) -> VarcosaLingeringWillBoss.deserialize(p, e));
		mBossDeserializers.put(EarthshakeBoss.identityTag, (Plugin p, LivingEntity e) -> EarthshakeBoss.deserialize(p, e));
		mBossDeserializers.put(MagicArrowBoss.identityTag, (Plugin p, LivingEntity e) -> MagicArrowBoss.deserialize(p, e));
		mBossDeserializers.put(SeekingProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> SeekingProjectileBoss.deserialize(p, e));
		mBossDeserializers.put(TrackingProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> TrackingProjectileBoss.deserialize(p, e));
		mBossDeserializers.put(MimicQueen.identityTag, (Plugin p, LivingEntity e) -> MimicQueen.deserialize(p, e));
		mBossDeserializers.put(WrathBoss.identityTag, (Plugin p, LivingEntity e) -> WrathBoss.deserialize(p, e));
		mBossDeserializers.put(LeapBoss.identityTag, (Plugin p, LivingEntity e) -> LeapBoss.deserialize(p, e));
		mBossDeserializers.put(BarrierBoss.identityTag, (Plugin p, LivingEntity e) -> BarrierBoss.deserialize(p, e));
		mBossDeserializers.put(CrowdControlResistanceBoss.identityTag, (Plugin p, LivingEntity e) -> CrowdControlResistanceBoss.deserialize(p, e));
		mBossDeserializers.put(MeteorSlamBoss.identityTag, (Plugin p, LivingEntity e) -> MeteorSlamBoss.deserialize(p, e));
		mBossDeserializers.put(SwingBoss.identityTag, (Plugin p, LivingEntity e) -> SwingBoss.deserialize(p, e));
		mBossDeserializers.put(TealQuestBoss.identityTag, (Plugin p, LivingEntity e) -> TealQuestBoss.deserialize(p, e));
		mBossDeserializers.put(MistMob.identityTag, (Plugin p, LivingEntity e) -> MistMob.deserialize(p, e));
		mBossDeserializers.put(HookBoss.identityTag, (Plugin p, LivingEntity e) -> HookBoss.deserialize(p, e));
		mBossDeserializers.put(FrostGiant.identityTag, (Plugin p, LivingEntity e) -> FrostGiant.deserialize(p, e));
		mBossDeserializers.put(FrostGiantIcicle.identityTag, (Plugin p, LivingEntity e) -> FrostGiantIcicle.deserialize(p, e));
		mBossDeserializers.put(SpellSlingerBoss.identityTag, (Plugin p, LivingEntity e) -> SpellSlingerBoss.deserialize(p, e));
		mBossDeserializers.put(VindictiveBoss.identityTag, (Plugin p, LivingEntity e) -> VindictiveBoss.deserialize(p, e));
		mBossDeserializers.put(ShadowTrailBoss.identityTag, (Plugin p, LivingEntity e) -> ShadowTrailBoss.deserialize(p, e));
		mBossDeserializers.put(KineticProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> KineticProjectileBoss.deserialize(p, e));
		mBossDeserializers.put(FlameTrailBoss.identityTag, (Plugin p, LivingEntity e) -> FlameTrailBoss.deserialize(p, e));
		mBossDeserializers.put(ShadeParticleBoss.identityTag, (Plugin p, LivingEntity e) -> ShadeParticleBoss.deserialize(p, e));
		mBossDeserializers.put(FireBombTossBoss.identityTag, (Plugin p, LivingEntity e) -> FireBombTossBoss.deserialize(p, e));
		mBossDeserializers.put(CommanderBoss.identityTag, (Plugin p, LivingEntity e) -> CommanderBoss.deserialize(p, e));
		mBossDeserializers.put(ShadePossessedBoss.identityTag, (Plugin p, LivingEntity e) -> ShadePossessedBoss.deserialize(p, e));
		mBossDeserializers.put(TwistedDespairBoss.identityTag, (Plugin p, LivingEntity e) -> TwistedDespairBoss.deserialize(p, e));
		mBossDeserializers.put(CoordinatedAttackBoss.identityTag, (Plugin p, LivingEntity e) -> CoordinatedAttackBoss.deserialize(p, e));
		mBossDeserializers.put(AbilitySilenceBoss.identityTag, (Plugin p, LivingEntity e) -> AbilitySilenceBoss.deserialize(p, e));
		mBossDeserializers.put(ShiftingBoss.identityTag, (Plugin p, LivingEntity e) -> ShiftingBoss.deserialize(p, e));
		mBossDeserializers.put(CarapaceBoss.identityTag, (Plugin p, LivingEntity e) -> CarapaceBoss.deserialize(p, e));
		mBossDeserializers.put(BulletHellBoss.identityTag, (Plugin p, LivingEntity e) -> BulletHellBoss.deserialize(p, e));
		mBossDeserializers.put(KamikazeBoss.identityTag, (Plugin p, LivingEntity e) -> KamikazeBoss.deserialize(p, e));
		mBossDeserializers.put(TinyBombTossBoss.identityTag, (Plugin p, LivingEntity e) -> TinyBombTossBoss.deserialize(p, e));
		mBossDeserializers.put(AntiRangeBoss.identityTag, (Plugin p, LivingEntity e) -> AntiRangeBoss.deserialize(p, e));
		mBossDeserializers.put(AntiRangeChivalrousBoss.identityTag, (Plugin p, LivingEntity e) -> AntiRangeChivalrousBoss.deserialize(p, e));
		mBossDeserializers.put(ImmortalMountBoss.identityTag, (Plugin p, LivingEntity e) -> ImmortalMountBoss.deserialize(p, e));
		mBossDeserializers.put(SilenceOnHitBoss.identityTag, (Plugin p, LivingEntity e) -> SilenceOnHitBoss.deserialize(p, e));
		mBossDeserializers.put(FalseSpirit.identityTag, (Plugin p, LivingEntity e) -> FalseSpirit.deserialize(p, e));
		mBossDeserializers.put(FalseSpiritPortal.identityTag, (Plugin p, LivingEntity e) -> FalseSpiritPortal.deserialize(p, e));
		mBossDeserializers.put(SnowSpirit.identityTag, (Plugin p, LivingEntity e) -> SnowSpirit.deserialize(p, e));
		mBossDeserializers.put(TffBookSummonBoss.identityTag, (Plugin p, LivingEntity e) -> TffBookSummonBoss.deserialize(p, e));
		mBossDeserializers.put(ArcaneProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> ArcaneProjectileBoss.deserialize(p, e));
		mBossDeserializers.put(JumpBoss.identityTag, (Plugin p, LivingEntity e) -> JumpBoss.deserialize(p, e));
		mBossDeserializers.put(RebornBoss.identityTag, (Plugin p, LivingEntity e) -> RebornBoss.deserialize(p, e));
		mBossDeserializers.put(NoFireBoss.identityTag, (Plugin p, LivingEntity e) -> NoFireBoss.deserialize(p, e));
		mBossDeserializers.put(Ghalkor.identityTag, (Plugin p, LivingEntity e) -> Ghalkor.deserialize(p, e));
		mBossDeserializers.put(Svalgot.identityTag, (Plugin p, LivingEntity e) -> Svalgot.deserialize(p, e));
		mBossDeserializers.put(BeastOfTheBlackFlame.identityTag, (Plugin p, LivingEntity e) -> BeastOfTheBlackFlame.deserialize(p, e));
		mBossDeserializers.put(DistanceCloserBoss.identityTag, (Plugin p, LivingEntity e) -> DistanceCloserBoss.deserialize(p, e));
		mBossDeserializers.put(WeakHookBoss.identityTag, (Plugin p, LivingEntity e) -> WeakHookBoss.deserialize(p, e));
		mBossDeserializers.put(AuraEffectBoss.identityTag, (Plugin p, LivingEntity e) -> AuraEffectBoss.deserialize(p, e));
		mBossDeserializers.put(DummyDecoyBoss.identityTag, (Plugin p, LivingEntity e) -> DummyDecoyBoss.deserialize(p, e));
		mBossDeserializers.put(Hedera.identityTag, (Plugin p, LivingEntity e) -> Hedera.deserialize(p, e));
		mBossDeserializers.put(Davey.identityTag, (Plugin p, LivingEntity e) -> Davey.deserialize(p, e));
		mBossDeserializers.put(Nucleus.identityTag, (Plugin p, LivingEntity e) -> Nucleus.deserialize(p, e));
		mBossDeserializers.put(LaserBoss.identityTag, (Plugin p, LivingEntity e) -> LaserBoss.deserialize(p, e));
		mBossDeserializers.put(SpinBoss.identityTag, (Plugin p, LivingEntity e) -> SpinBoss.deserialize(p, e));
		mBossDeserializers.put(OnHitBoss.identityTag, (Plugin p, LivingEntity e) -> OnHitBoss.deserialize(p, e));
		mBossDeserializers.put(NovaBoss.identityTag, (Plugin p, LivingEntity e) -> NovaBoss.deserialize(p, e));
		mBossDeserializers.put(ProjectileBoss.identityTag, (Plugin p, LivingEntity e) -> ProjectileBoss.deserialize(p, e));
		mBossDeserializers.put(RainbowParrot.identityTag, (Plugin p, LivingEntity e) -> RainbowParrot.deserialize(p, e));
		mBossDeserializers.put(SpawnMobsBoss.identityTag, (Plugin p, LivingEntity e) -> SpawnMobsBoss.deserialize(p, e));
		mBossDeserializers.put(LandSlowBoss.identityTag, (Plugin p, LivingEntity e) -> LandSlowBoss.deserialize(p, e));
		mBossDeserializers.put(PounceBoss.identityTag, (Plugin p, LivingEntity e) -> PounceBoss.deserialize(p, e));
		mBossDeserializers.put(NoAbilityDamageBoss.identityTag, (Plugin p, LivingEntity e) -> NoAbilityDamageBoss.deserialize(p, e));
		mBossDeserializers.put(NoGlowingBoss.identityTag, (Plugin p, LivingEntity e) -> NoGlowingBoss.deserialize(p, e));
		mBossDeserializers.put(RKitxet.identityTag, (Plugin p, LivingEntity e) -> RKitxet.deserialize(p, e));
		mBossDeserializers.put(VerdantMinibossBoss.identityTag, (Plugin p, LivingEntity e) -> VerdantMinibossBoss.deserialize(p, e));
		mBossDeserializers.put(GenericTargetBoss.identityTag, (Plugin p, LivingEntity e) -> GenericTargetBoss.deserialize(p, e));
		mBossDeserializers.put(MobRisingBoss.identityTag, (Plugin p, LivingEntity e) -> MobRisingBoss.deserialize(p, e));
		mBossDeserializers.put(GrenadeLauncherBoss.identityTag, (Plugin p, LivingEntity e) -> GrenadeLauncherBoss.deserialize(p, e));
		mBossDeserializers.put(SizeChangerBoss.identityTag, (Plugin p, LivingEntity e) -> SizeChangerBoss.deserialize(p, e));
		mBossDeserializers.put(DelveScalingBoss.identityTag, (Plugin p, LivingEntity e) -> DelveScalingBoss.deserialize(p, e));
		mBossDeserializers.put(DeathSummonBoss.identityTag, (Plugin p, LivingEntity e) -> DeathSummonBoss.deserialize(p, e));
		mBossDeserializers.put(StarfallBoss.identityTag, (Plugin p, LivingEntity e) -> StarfallBoss.deserialize(p, e));
		mBossDeserializers.put(ShatterBoss.identityTag, (Plugin p, LivingEntity e) -> ShatterBoss.deserialize(p, e));
		mBossDeserializers.put(SpectralSummonBoss.identityTag, (Plugin p, LivingEntity e) -> SpectralSummonBoss.deserialize(p, e));
		mBossDeserializers.put(DreadfulSummonBoss.identityTag, (Plugin p, LivingEntity e) -> DreadfulSummonBoss.deserialize(p, e));
		mBossDeserializers.put(StatMultiplierBoss.identityTag, (Plugin p, LivingEntity e) -> StatMultiplierBoss.deserialize(p, e));
		mBossDeserializers.put(SummonOnExplosionBoss.identityTag, (Plugin p, LivingEntity e) -> SummonOnExplosionBoss.deserialize(p, e));
		mBossDeserializers.put(WarriorShieldWallBoss.identityTag, (Plugin p, LivingEntity e) -> WarriorShieldWallBoss.deserialize(p, e));
		mBossDeserializers.put(WarlockAmpHexBoss.identityTag, (Plugin p, LivingEntity e) -> WarlockAmpHexBoss.deserialize(p, e));
		mBossDeserializers.put(DodgeBoss.identityTag, (Plugin p, LivingEntity e) -> DodgeBoss.deserialize(p, e));
		mBossDeserializers.put(BlockPlacerBoss.identityTag, (Plugin p, LivingEntity e) -> BlockPlacerBoss.deserialize(p, e));
		mBossDeserializers.put(PotionThrowBoss.identityTag, (Plugin p, LivingEntity e) -> PotionThrowBoss.deserialize(p, e));
		mBossDeserializers.put(LimitedLifespanBoss.identityTag, (Plugin p, LivingEntity e) -> LimitedLifespanBoss.deserialize(p, e));
		mBossDeserializers.put(BlueFireBoss.identityTag, (Plugin p, LivingEntity e) -> BlueFireBoss.deserialize(p, e));
		mBossDeserializers.put(BlueEarthBoss.identityTag, (Plugin p, LivingEntity e) -> BlueEarthBoss.deserialize(p, e));
		mBossDeserializers.put(BlueAirBoss.identityTag, (Plugin p, LivingEntity e) -> BlueAirBoss.deserialize(p, e));
		mBossDeserializers.put(BlueWaterBoss.identityTag, (Plugin p, LivingEntity e) -> BlueWaterBoss.deserialize(p, e));
		mBossDeserializers.put(Samwell.identityTag, (Plugin p, LivingEntity e) -> Samwell.deserialize(p, e));
		mBossDeserializers.put(BlueStrikeDaggerCraftingBoss.identityTag, (Plugin p, LivingEntity e) -> BlueStrikeDaggerCraftingBoss.deserialize(p, e));
		mBossDeserializers.put(BlueStrikeTargetNPCBoss.identityTag, (Plugin p, LivingEntity e) -> BlueStrikeTargetNPCBoss.deserialize(p, e));
		mBossDeserializers.put(BlueStrikeTurretBoss.identityTag, (Plugin p, LivingEntity e) -> BlueStrikeTurretBoss.deserialize(p, e));
		mBossDeserializers.put(LavaCannonBoss.identityTag, (Plugin p, LivingEntity e) -> LavaCannonBoss.deserialize(p, e));
		mBossDeserializers.put(SoundBoss.identityTag, (Plugin p, LivingEntity e) -> SoundBoss.deserialize(p, e));

		mBossDeserializers.put(Lich.identityTag, (Plugin p, LivingEntity e) -> Lich.deserialize(p, e));
		mBossDeserializers.put(LichAlchBoss.identityTag, (Plugin p, LivingEntity e) -> LichAlchBoss.deserialize(p, e));
		mBossDeserializers.put(LichClericBoss.identityTag, (Plugin p, LivingEntity e) -> LichClericBoss.deserialize(p, e));
		mBossDeserializers.put(LichMageBoss.identityTag, (Plugin p, LivingEntity e) -> LichMageBoss.deserialize(p, e));
		mBossDeserializers.put(LichRogueBoss.identityTag, (Plugin p, LivingEntity e) -> LichRogueBoss.deserialize(p, e));
		mBossDeserializers.put(LichScoutBoss.identityTag, (Plugin p, LivingEntity e) -> LichScoutBoss.deserialize(p, e));
		mBossDeserializers.put(LichWarlockBoss.identityTag, (Plugin p, LivingEntity e) -> LichWarlockBoss.deserialize(p, e));
		mBossDeserializers.put(LichWarriorBoss.identityTag, (Plugin p, LivingEntity e) -> LichWarriorBoss.deserialize(p, e));
		mBossDeserializers.put(LichConquestBoss.identityTag, (Plugin p, LivingEntity e) -> LichConquestBoss.deserialize(p, e));
		mBossDeserializers.put(LichDemiseBoss.identityTag, (Plugin p, LivingEntity e) -> LichDemiseBoss.deserialize(p, e));
		mBossDeserializers.put(LichJudgementBoss.identityTag, (Plugin p, LivingEntity e) -> LichJudgementBoss.deserialize(p, e));
		mBossDeserializers.put(LichStrifeBoss.identityTag, (Plugin p, LivingEntity e) -> LichStrifeBoss.deserialize(p, e));
		mBossDeserializers.put(LichCurseBoss.identityTag, (Plugin p, LivingEntity e) -> LichCurseBoss.deserialize(p, e));
		mBossDeserializers.put(LichShieldBoss.identityTag, (Plugin p, LivingEntity e) -> LichShieldBoss.deserialize(p, e));
		mBossDeserializers.put(LichKeyGlowBoss.identityTag, (Plugin p, LivingEntity e) -> LichKeyGlowBoss.deserialize(p, e));
		mBossDeserializers.put(FestiveTessUpgradeSnowmenBoss.identityTag, (Plugin p, LivingEntity e) -> FestiveTessUpgradeSnowmenBoss.deserialize(p, e));
		mBossDeserializers.put(MetalmancyBoss.identityTag, (Plugin p, LivingEntity e) -> MetalmancyBoss.deserialize(p, e));
		mBossDeserializers.put(RestlessSoulsBoss.identityTag, (Plugin p, LivingEntity e) -> RestlessSoulsBoss.deserialize(p, e));
		mBossDeserializers.put(AlchemicalAberrationBoss.identityTag, (Plugin p, LivingEntity e) -> AlchemicalAberrationBoss.deserialize(p, e));
		mBossDeserializers.put(ThrowSummonBoss.identityTag, (Plugin p, LivingEntity e) -> ThrowSummonBoss.deserialize(p, e));
		mBossDeserializers.put(HostileBoss.identityTag, (Plugin p, LivingEntity e) -> HostileBoss.deserialize(p, e));
		mBossDeserializers.put(FriendlyBoss.identityTag, (Plugin p, LivingEntity e) -> FriendlyBoss.deserialize(p, e));
		mBossDeserializers.put(PortalBoss.identityTag, (Plugin p, LivingEntity e) -> PortalBoss.deserialize(p, e));
		mBossDeserializers.put(ImperialConstruct.identityTag, (Plugin p, LivingEntity e) -> ImperialConstruct.deserialize(p, e));
		mBossDeserializers.put(ScoutVolleyBoss.identityTag, (Plugin p, LivingEntity e) -> ScoutVolleyBoss.deserialize(p, e));
		mBossDeserializers.put(MageCosmicMoonbladeBoss.identityTag, (Plugin p, LivingEntity e) -> MageCosmicMoonbladeBoss.deserialize(p, e));
		mBossDeserializers.put(TwistedMiniBoss.identityTag, (Plugin p, LivingEntity e) -> TwistedMiniBoss.deserialize(p, e));
		mBossDeserializers.put(BrownNegativeBoss.identityTag, (Plugin p, LivingEntity e) -> BrownNegativeBoss.deserialize(p, e));
		mBossDeserializers.put(BrownPositiveBoss.identityTag, (Plugin p, LivingEntity e) -> BrownPositiveBoss.deserialize(p, e));
		mBossDeserializers.put(BrownMagnetSwapBoss.identityTag, (Plugin p, LivingEntity e) -> BrownMagnetSwapBoss.deserialize(p, e));
		mBossDeserializers.put(ParadoxSwapBoss.identityTag, (Plugin p, LivingEntity e) -> ParadoxSwapBoss.deserialize(p, e));
		mBossDeserializers.put(TealSpirit.identityTag, (Plugin p, LivingEntity e) -> TealSpirit.deserialize(p, e));
		mBossDeserializers.put(TemporalShieldBoss.identityTag, (Plugin p, LivingEntity e) -> TemporalShieldBoss.deserialize(p, e));
		mBossDeserializers.put(GalleryMobRisingBoss.identityTag, (Plugin p, LivingEntity e) -> GalleryMobRisingBoss.deserialize(p, e));
		mBossDeserializers.put(GallerySummonMobBoss.identityTag, (Plugin p, LivingEntity e) -> GallerySummonMobBoss.deserialize(p, e));
		mBossDeserializers.put(MusicBoss.identityTag, (Plugin p, LivingEntity e) -> MusicBoss.deserialize(p, e));
		mBossDeserializers.put(TagScalingBoss.identityTag, (Plugin p, LivingEntity e) -> TagScalingBoss.deserialize(p, e));
		mBossDeserializers.put(CancelDamageBoss.identityTag, (Plugin p, LivingEntity e) -> CancelDamageBoss.deserialize(p, e));

		/***************************************************
		 * Boss Parameters
		 ****************************************************/
		mBossParameters = new HashMap<>();
		mBossParameters.put(BlockLockBoss.identityTag, new BlockLockBoss.Parameters());
		mBossParameters.put(ChestLockBoss.identityTag, new ChestLockBoss.Parameters());
		mBossParameters.put(AntiRangeBoss.identityTag, new AntiRangeBoss.Parameters());
		mBossParameters.put(UnyieldingBoss.identityTag, new UnyieldingBoss.Parameters());
		mBossParameters.put(ToughBoss.identityTag, new ToughBoss.Parameters());
		mBossParameters.put(UnseenBoss.identityTag, new UnseenBoss.Parameters());
		mBossParameters.put(WhispersBoss.identityTag, new WhispersBoss.Parameters());
		mBossParameters.put(NovaBoss.identityTag, new NovaBoss.Parameters());
		mBossParameters.put(LaserBoss.identityTag, new LaserBoss.Parameters());
		mBossParameters.put(ProjectileBoss.identityTag, new ProjectileBoss.Parameters());
		mBossParameters.put(AuraEffectBoss.identityTag, new AuraEffectBoss.Parameters());
		mBossParameters.put(AvengerBoss.identityTag, new AvengerBoss.Parameters());
		mBossParameters.put(BarrierBoss.identityTag, new BarrierBoss.Parameters());
		mBossParameters.put(BulletHellBoss.identityTag, new BulletHellBoss.Parameters());
		mBossParameters.put(ChargerBoss.identityTag, new ChargerBoss.Parameters());
		mBossParameters.put(CommanderBoss.identityTag, new CommanderBoss.Parameters());
		mBossParameters.put(CrowdControlResistanceBoss.identityTag, new CrowdControlResistanceBoss.Parameters());
		mBossParameters.put(EarthshakeBoss.identityTag, new EarthshakeBoss.Parameters());
		mBossParameters.put(FireBombTossBoss.identityTag, new FireBombTossBoss.Parameters());
		mBossParameters.put(FlameTrailBoss.identityTag, new FlameTrailBoss.Parameters());
		mBossParameters.put(ForceBoss.identityTag, new ForceBoss.Parameters());
		mBossParameters.put(JumpBoss.identityTag, new JumpBoss.Parameters());
		mBossParameters.put(MeteorSlamBoss.identityTag, new MeteorSlamBoss.Parameters());
		mBossParameters.put(OnHitBoss.identityTag, new OnHitBoss.Parameters());
		mBossParameters.put(PounceBoss.identityTag, new PounceBoss.Parameters());
		mBossParameters.put(RejuvenationBoss.identityTag, new RejuvenationBoss.Parameters());
		mBossParameters.put(SpawnMobsBoss.identityTag, new SpawnMobsBoss.Parameters());
		mBossParameters.put(SwingBoss.identityTag, new SwingBoss.Parameters());
		mBossParameters.put(TpBehindBoss.identityTag, new TpBehindBoss.Parameters());
		mBossParameters.put(TpSwapBoss.identityTag, new TpSwapBoss.Parameters());
		mBossParameters.put(UnstableBoss.identityTag, new UnstableBoss.Parameters());
		mBossParameters.put(SeekingProjectileBoss.identityTag, new SeekingProjectileBoss.Parameters());
		mBossParameters.put(MobRisingBoss.identityTag, new MobRisingBoss.Parameters());
		mBossParameters.put(FestiveTessUpgradeSnowmenBoss.identityTag, new FestiveTessUpgradeSnowmenBoss.Parameters());
		mBossParameters.put(GrenadeLauncherBoss.identityTag, new GrenadeLauncherBoss.Parameters());
		mBossParameters.put(SizeChangerBoss.identityTag, new SizeChangerBoss.Parameters());
		mBossParameters.put(WrathBoss.identityTag, new WrathBoss.Parameters());
		mBossParameters.put(ThrowSummonBoss.identityTag, new ThrowSummonBoss.Parameters());
		mBossParameters.put(DelveScalingBoss.identityTag, new DelveScalingBoss.Parameters());
		mBossParameters.put(DeathSummonBoss.identityTag, new DeathSummonBoss.Parameters());
		mBossParameters.put(SummonOnExplosionBoss.identityTag, new SummonOnExplosionBoss.Parameters());
		mBossParameters.put(ScoutVolleyBoss.identityTag, new ScoutVolleyBoss.Parameters());
		mBossParameters.put(HostileBoss.identityTag, new HostileBoss.Parameters());
		mBossParameters.put(FriendlyBoss.identityTag, new FriendlyBoss.Parameters());
		mBossParameters.put(RebornBoss.identityTag, new RebornBoss.Parameters());
		mBossParameters.put(BlockBreakBoss.identityTag, new BlockBreakBoss.Parameters());
		mBossParameters.put(GenericBoss.identityTag, new GenericBoss.Parameters());
		mBossParameters.put(MageCosmicMoonbladeBoss.identityTag, new MageCosmicMoonbladeBoss.Parameters());
		mBossParameters.put(StarfallBoss.identityTag, new StarfallBoss.Parameters());
		mBossParameters.put(ShatterBoss.identityTag, new ShatterBoss.Parameters());
		mBossParameters.put(WarriorShieldWallBoss.identityTag, new WarriorShieldWallBoss.Parameters());
		mBossParameters.put(WarlockAmpHexBoss.identityTag, new WarlockAmpHexBoss.Parameters());
		mBossParameters.put(DodgeBoss.identityTag, new DodgeBoss.Parameters());
		mBossParameters.put(GenericBoss.identityTag, new GenericBoss.Parameters());
		mBossParameters.put(PotionThrowBoss.identityTag, new PotionThrowBoss.Parameters());
		mBossParameters.put(GenericTargetBoss.identityTag, new GenericTargetBoss.Parameters());
		mBossParameters.put(LimitedLifespanBoss.identityTag, new LimitedLifespanBoss.Parameters());
		mBossParameters.put(ImmortalMountBoss.identityTag, new ImmortalMountBoss.Parameters());
		mBossParameters.put(BrownMagnetSwapBoss.identityTag, new BrownMagnetSwapBoss.Parameters());
		mBossParameters.put(BrownPositiveBoss.identityTag, new BrownPositiveBoss.Parameters());
		mBossParameters.put(BrownNegativeBoss.identityTag, new BrownNegativeBoss.Parameters());
		mBossParameters.put(MusicBoss.identityTag, new MusicBoss.Parameters());
		mBossParameters.put(TagScalingBoss.identityTag, new TagScalingBoss.Parameters());

	}

	/********************************************************************************
	 * Member Variables
	 *******************************************************************************/
	private final Plugin mPlugin;
	private final Map<UUID, Boss> mBosses;
	private boolean mNearbyEntityDeathEnabled = false;
	private boolean mNearbyBlockBreakEnabled = false;
	private boolean mNearbyPlayerDeathEnabled = false;

	public BossManager(Plugin plugin) {
		mPlugin = plugin;
		mBosses = new HashMap<UUID, Boss>();

		/* When starting up, look for bosses in all current world entities */
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (!(entity instanceof LivingEntity)) {
					continue;
				}

				processEntity((LivingEntity) entity);
			}
		}

		INSTANCE = this;
	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living
			    && !living.getScoreboardTags().isEmpty()) {
			// EntityAddToWorldEvent is called at an inconvenient time in Minecraft's code, which can cause deadlocks,
			// so we delay initialisation of boss data slightly to be outside of the entity loading code.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				processEntity(living);
			});
		}
	}

	// Creature spawn event is also listened to to set up boss data for initial spawn at the moment the mob is summoned,
	// which allows to immediately use the boss after summoning it.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void creatureSpawnEvent(CreatureSpawnEvent event) {
		processEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living) {
			unload(living, false);
		}
	}

	// Some entities seem to persist despite their chunk being unloaded,
	// so we manually load/unload their boss data when the chunk is loaded/unloaded.

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> { // delay by a tick as the chunk is loaded before entities in 1.18+
			for (Entity entity : event.getChunk().getEntities()) {
				if (entity instanceof LivingEntity living) {
					processEntity(living);
				}
			}
		}, 1);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity living) {
				unload(living, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (mNearbyEntityDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(entity.getLocation(), 12.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyEntityDeath(event);
				}
			}
		}

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.death(event);
			if (entity.getHealth() <= 0) {
				unload(boss, false);
				mBosses.remove(entity.getUniqueId());

				/*
				 * Remove special serialization data from drops. Should not be
				 * necessary since loaded bosses already have this data stripped
				 */
				SerializationUtils.stripSerializationDataFromDrops(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (mNearbyBlockBreakEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(event.getBlock().getLocation(), 62.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyBlockBreak(event);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (entity != null && entity instanceof Creeper) {
			Boss boss = mBosses.remove(entity.getUniqueId());
			if (boss != null) {
				boss.death(null);
				unload(boss, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile proj = event.getEntity();
		if (proj != null) {
			ProjectileSource shooter = proj.getShooter();
			if (shooter != null && shooter instanceof LivingEntity) {
				Boss boss = mBosses.get(((LivingEntity) shooter).getUniqueId());
				if (boss != null) {
					// May cancel the event
					boss.bossLaunchedProjectile(event);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		if (proj != null) {
			ProjectileSource shooter = proj.getShooter();
			if (shooter != null && shooter instanceof LivingEntity) {
				Boss boss = mBosses.get(((LivingEntity) shooter).getUniqueId());
				if (boss != null) {
					boss.bossProjectileHit(event);
				}
			}
			if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
				LivingEntity hit = (LivingEntity) event.getHitEntity();
				Boss boss = mBosses.get(hit.getUniqueId());
				if (boss != null) {
					boss.bossHitByProjectile(event);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Make a copy so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<LivingEntity>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.areaEffectAppliedToBoss(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Make a copy so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<LivingEntity>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.splashPotionAppliedToBoss(event);
			}
		}

		if (event.getEntity().getShooter() instanceof LivingEntity le) {
			Boss boss = mBosses.get(le.getUniqueId());
			if (boss != null) {
				boss.bossSplashPotion(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());
		if (boss != null) {
			boss.entityPotionEffectEvent(event);
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable <T extends BossAbilityGroup> T getBoss(Entity entity, Class<T> cls) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			for (BossAbilityGroup ability : boss.getAbilities()) {
				if (cls.isInstance(ability)) {
					return (T) ability;
				}
			}
		}

		return null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHurt(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		Boss boss = mBosses.get(damagee.getUniqueId());
		if (boss != null) {
			// May cancel the event
			boss.onHurt(event);
			if (damager != null) {
				boss.onHurtByEntity(event, damager);
				if (source != null) {
					boss.onHurtByEntityWithSource(event, damager, source);
				}
			}
			boss.setLastHitBy(source);
		}

		Entity vehicle = damagee.getVehicle();
		if (vehicle instanceof LivingEntity mount) {
			Boss mountBoss = mBosses.get(mount.getUniqueId());
			if (mountBoss != null) {
				mountBoss.onPassengerHurt(event);
			}
		}
	}

	// Must be before player items and abilities, which use priority HIGH
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDamage(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();
		if (source != null) {
			Boss boss = mBosses.get(source.getUniqueId());
			if (boss != null) {
				// May cancel the event
				boss.onDamage(event, damagee);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void spellCastEvent(SpellCastEvent event) {
		LivingEntity boss = event.getBoss();
		Boss b = mBosses.get(boss.getUniqueId());
		if (b != null && !EntityUtils.isSilenced(boss)) {
			b.bossCastAbility(event);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityPathfindEvent(EntityPathfindEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossPathfind(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityTargetEvent(EntityTargetEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossChangedTarget(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void slimeSplitEvent(SlimeSplitEvent event) {
		if (event.getEntity().getScoreboardTags().contains("boss_nosplit")) {
			/*
			 * This annoying special-case boss won't work with the normal system because
			 * the Boss that needs to be matched unloads before this event gets called
			 */
			event.setCancelled(true);
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applyStun() */
	/* TODO: Probably make this an actual event? */
	public void entityStunned(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossStunned();
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applySilence() */
	/* TODO: Probably make this an actual event? */
	public void entitySilenced(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossSilenced();
		}
	}

	/* Kind of a weird one - not hooked to bosses but used for snowman killer */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player.hasMetadata(WinterSnowmanEventBoss.deathMetakey)) {
			Entity snowman = Bukkit.getEntity(UUID.fromString(player.getMetadata(WinterSnowmanEventBoss.deathMetakey).get(0).asString()));
			Component snowmanName = snowman != null ? snowman.customName() : null;
			Component deathMessage = Component.text("")
				.append(Component.selector(player.getName()))
				.append(Component.text(" was snowballed by "))
				.append(snowmanName != null ? snowmanName : Component.text("a snowman"));
			event.deathMessage(deathMessage);
			player.removeMetadata(WinterSnowmanEventBoss.deathMetakey, mPlugin);
		}

		if (mNearbyPlayerDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(player.getLocation(), 75.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyPlayerDeath(event);
				}
			}
		}
	}

	/* Another weird one - used for exorcism potion */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void lingeringPotionSplashEvent(LingeringPotionSplashEvent event) {
		if (event.getEntityType() == EntityType.SPLASH_POTION) {
			ThrownPotion potEntity = event.getEntity();
			ItemStack potItem = potEntity.getItem();
			if (potItem.getType() == Material.LINGERING_POTION) {
				if (InventoryUtils.testForItemWithLore(potItem, "Exorcism")) {
					AreaEffectCloud cloud = event.getAreaEffectCloud();
					if (event.getAreaEffectCloud() != null) {
						cloud.setMetadata("MonumentaBossesGrayExorcism", new FixedMetadataValue(mPlugin, 1));
						cloud.setRadius(6.5f);
						cloud.setDurationOnUse(0);
						cloud.setRadiusOnUse(0);
						cloud.setRadiusPerTick(-0.004f);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerLeashEntityEvent(PlayerLeashEntityEvent event) {
		if (mBosses.get(event.getEntity().getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.customEffectAppliedToBoss(event);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void bossExploded(EntityExplodeEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.bossExploded(event);
		}
	}

	// Only acts on fire applied by the plugin, called in EntityUtils
	public void bossIgnited(Entity entity, int ticks) {
		Boss boss = mBosses.get(entity.getUniqueId());

		if (boss != null) {
			boss.bossIgnited(ticks);
		}
	}

	/********************************************************************************
	 * Static public methods
	 *******************************************************************************/
	public static @Nullable BossManager getInstance() {
		return INSTANCE;
	}

	public static void registerStatelessBoss(String identityTag, StatelessBossConstructor fn) {
		mStatelessBosses.put(identityTag, fn);
	}

	public static void registerStatefulBoss(String identityTag, StatefulBossConstructor fn) {
		mStatefulBosses.put(identityTag, fn);
	}

	public static void registerBossDeserializer(String identityTag, BossDeserializer fn) {
		mBossDeserializers.put(identityTag, fn);
	}

	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public @Nullable List<BossAbilityGroup> getAbilities(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			return boss.getAbilities();
		}
		return null;
	}

	/*
	 * Every way to unload a boss needs to bounce through this function to ensure
	 * state is updated correctly!
	 */
	private void unload(Boss boss, boolean shuttingDown) {
		if (!shuttingDown) {
			checkDisablePerformanceEvents(boss);
		}
		boss.unload(shuttingDown);
	}

	public void unload(LivingEntity entity, boolean shuttingDown) {
		Boss boss = mBosses.remove(entity.getUniqueId());
		if (boss != null) {
			unload(boss, shuttingDown);
		}
	}

	public void unloadAll(boolean shuttingDown) {
		for (Map.Entry<UUID, Boss> entry : mBosses.entrySet()) {
			unload(entry.getValue(), shuttingDown);
		}
		mBosses.clear();
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatelessBossConstructor stateless = mStatelessBosses.get(requestedTag);
		if (stateless != null) {
			INSTANCE.createBossInternal(targetEntity, stateless.construct(INSTANCE.mPlugin, targetEntity));
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (mStatefulBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						"' but it requires positional arguments");
					sender.sendMessage(ChatColor.GOLD + "Try again with some ending location coordinates");
				}
			} else {
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "No boss found with the tag '" + requestedTag + "'");
				}
			}
		}
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag, Location endLoc) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatefulBossConstructor stateful = INSTANCE.mStatefulBosses.get(requestedTag);
		if (stateful != null) {
			INSTANCE.createBossInternal(targetEntity, stateful.construct(INSTANCE.mPlugin, targetEntity, targetEntity.getLocation(), endLoc));
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (INSTANCE.mStatelessBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						"' but it does not take positional arguments");
					sender.sendMessage(ChatColor.GOLD + "Try again without the coordinates");
				}
			} else {
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "No boss found with the tag '" + requestedTag + "'");
				}
			}
		}
	}

	/* Machine readable list */
	public String[] listBosses() {
		Set<String> allBossTags = new HashSet<String>(mStatelessBosses.keySet());
		allBossTags.addAll(mStatefulBosses.keySet());
		return allBossTags.toArray(String[]::new);
	}

	/* Machine readable list */
	public String[] listStatelessBosses() {
		return mStatelessBosses.keySet().toArray(String[]::new);
	}

	/********************************************************************************
	 * Private Methods
	 *******************************************************************************/

	/*
	 * This function should be called whenever an ability is loaded to enable events
	 * that are more performance sensative (if applicable)
	 */
	private void checkEnablePerformanceEvents(BossAbilityGroup ability) {
		if (ability.hasNearbyEntityDeathTrigger()) {
			mNearbyEntityDeathEnabled = true;
		}

		if (ability.hasNearbyBlockBreakTrigger()) {
			mNearbyBlockBreakEnabled = true;
		}

		if (ability.hasNearbyPlayerDeathTrigger()) {
			mNearbyPlayerDeathEnabled = true;
		}
	}

	/*
	 * This function should be called whenever a boss is unloaded to disable events
	 * that are more performance sensative (if applicable)
	 */
	private void checkDisablePerformanceEvents(Boss boss) {
		if (boss.hasNearbyEntityDeathTrigger()) {
			if (!mNearbyEntityDeathEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyEntityDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyEntityDeathTrigger)) {
				mNearbyEntityDeathEnabled = false;
			}
		}

		if (boss.hasNearbyBlockBreakTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyBlockBreakTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyBlockBreakTrigger)) {
				mNearbyBlockBreakEnabled = false;
			}
		}

		if (boss.hasNearbyPlayerDeathTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyPlayerDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyPlayerDeathTrigger)) {
				mNearbyPlayerDeathEnabled = false;
			}
		}
	}

	public void createBossInternal(LivingEntity targetEntity, BossAbilityGroup ability) throws Exception {
		/* Set up boss health / armor / etc */
		ability.init();

		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(targetEntity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
		} else {
			boss.add(ability);
		}

		mBosses.put(targetEntity.getUniqueId(), boss);
	}

	private void processEntity(LivingEntity entity) {
		if (mBosses.containsKey(entity.getUniqueId())) {
			// already loaded
			return;
		}

		Set<String> tags = entity.getScoreboardTags();
		if (!tags.isEmpty()) {
			Boss boss = null;
			/*
			 * Note - it is important to make a copy here to avoid concurrent modification exception
			 * which happens while iterating and the boss constructor changes the tags on the mob
			 */
			for (String tag : new ArrayList<String>(tags)) {
				BossDeserializer deserializer = mBossDeserializers.get(tag);
				if (deserializer != null) {
					BossAbilityGroup ability;
					try {
						ability = deserializer.deserialize(mPlugin, entity);
					} catch (Exception ex) {
						mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
						continue;
					}

					checkEnablePerformanceEvents(ability);

					if (boss == null) {
						boss = new Boss(mPlugin, ability);
						mBosses.put(entity.getUniqueId(), boss);
					} else {
						boss.add(ability);
					}
				}
			}
		}
	}

	public void sendBossDebugInfo(CommandSender sender) {
		sender.sendMessage("");
		sender.sendMessage("Total number of loaded bosses: " + mBosses.size());
		sender.sendMessage("mNearbyEntityDeathEnabled: " + mNearbyEntityDeathEnabled);
		sender.sendMessage("mNearbyBlockBreakEnabled: " + mNearbyBlockBreakEnabled);
		sender.sendMessage("mNearbyPlayerDeathEnabled: " + mNearbyPlayerDeathEnabled);

		Map<String, Integer> bossCounts = new HashMap<>();
		for (Boss boss : mBosses.values()) {
			String tags = String.join(" ", boss.getIdentityTags());
			Integer count = bossCounts.get(tags);
			if (count == null) {
				count = 1;
			} else {
				count += 1;
			}
			bossCounts.put(tags, count);
		}

		List<Map.Entry<String, Integer>> list = new ArrayList<>(bossCounts.entrySet());
		list.sort(Map.Entry.comparingByValue());

		for (Map.Entry<String, Integer> entry : list) {
			sender.sendMessage("  " + entry.getKey() + ": " + entry.getValue());
		}
	}

	public void manuallyRegisterBoss(LivingEntity entity, BossAbilityGroup ability) {
		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
			mBosses.put(entity.getUniqueId(), boss);
		} else {
			boss.add(ability);
		}
	}

}
