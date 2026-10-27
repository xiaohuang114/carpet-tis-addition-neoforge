package net.fabricmc.loader.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.loader.api.metadata.ModMetadata;

/**
 * TIS-S2/S3 dev shim for fabric-loader's {@code FabricLoader}, backed by NeoForge's mod lists.
 *
 * <p>Needed because conditional-mixin 0.6.4 is shipped as a Fabric mod
 * ({@code VersionCheckerImpl} calls {@code FabricLoader.getInstance()}) while its annotation classes are
 * required by {@code CarpetRuleRegistrar.parseSettingsClass}: the TISCM {@code @Rule} annotation carries
 * {@code me.fallenbreath.conditionalmixin.api.annotation.Restriction} members, so {@code AnnotationType}
 * must be able to resolve them. Since S3 the same lookup also runs inside conditional-mixin's
 * {@code SimpleRestrictionChecker}, which the mixin plugin evaluates while Mixin is still in
 * {@code MixinEnvironment.Phase.PREPARE}.</p>
 *
 * <p>S3 addition ("early bootstrap"): at {@code PREPARE} time NeoForge has not created the real
 * {@code ModList} yet ({@code ModList.get()} returns {@code null} until {@code FMLLoader.loadMods}), so a
 * {@code ModList}-only shim dies with an NPE inside {@code shouldApplyMixin} and Mixin turns that into
 * {@code InvalidMixinException: Error initialising mixin config}. The scanned-but-unbuilt mod files are
 * already visible through {@code net.neoforged.fml.loading.LoadingModList}, therefore:
 * <ol>
 *   <li>{@code ModList} non-null (runtime, post-launch) -&gt; authoritative answer, unchanged S2 behaviour;</li>
 *   <li>{@code ModList} null (mixin plugin at PREPARE) -&gt; answer from {@code LoadingModList.getMods()},
 *       plus the synthetic {@code minecraft}/{@code neoforge} ids taken from {@code FMLLoader.versionInfo()}.</li>
 * </ol>
 * Both branches answer the same two questions conditional-mixin asks ({@code isModLoaded},
 * {@code getModContainer().getMetadata().getVersion().getFriendlyString()}), so no semantics change -
 * only the data source does.</p>
 *
 * <p>Deliberately tiny: only {@link #isModLoaded(String)} and {@link #getModContainer(String)} exist,
 * because those are the only two members conditional-mixin links against (verified with
 * {@code javap -c me.fallenbreath.conditionalmixin.api.util.fabric.VersionCheckerImpl}).</p>
 *
 * <p>NeoForge types are reached through reflection so this shim compiles with no project classpath
 * (avoids a Gradle task cycle: stripConditionalMixin -&gt; compileFabricLoaderShim -&gt; compileClasspath -&gt;
 * stripConditionalMixin).</p>
 */
public interface FabricLoader
{
	static FabricLoader getInstance()
	{
		return Impl.INSTANCE;
	}

	boolean isModLoaded(String modId);

	Optional<ModContainer> getModContainer(String modId);

	/**
	 * NeoForge-backed implementation.
	 */
	final class Impl implements FabricLoader
	{
		static final Impl INSTANCE = new Impl();

		// post-launch source: net.neoforged.fml.ModList (null until FMLLoader.loadMods)
		private static Method GET;
		private static Method IS_LOADED;
		private static Method GET_CONTAINER;

		// pre-launch source: net.neoforged.fml.loading.LoadingModList + VersionInfo
		private static Method LOADING_GET;
		private static Method LOADING_MODS;
		private static Method FMLLOADER_VERSION_INFO;
		private static Method MC_VERSION;
		private static Method NEOFORGE_VERSION;
		private static Method INFO_ID;
		private static Method INFO_VERSION;

		/**
		 * {@code modId -> version string} snapshot built from the mod scan, or {@code null} while the scan is
		 * not observable yet. Cached on first successful build: mod files do not change during a run.
		 */
		private static Map<String, String> early;

		private static Object modList()
		{
			try
			{
				if (GET == null)
				{
					Class<?> cls = Class.forName("net.neoforged.fml.ModList");
					GET = cls.getMethod("get");
					IS_LOADED = cls.getMethod("isLoaded", String.class);
					GET_CONTAINER = cls.getMethod("getModContainerById", String.class);
				}
				return GET.invoke(null);
			}
			catch (ReflectiveOperationException e)
			{
				throw new IllegalStateException("NeoForge ModList is unavailable", e);
			}
		}

		private static synchronized Map<String, String> earlyMods()
		{
			if (early != null)
			{
				return early;
			}
			Map<String, String> mods = new HashMap<>();
			try
			{
				if (LOADING_GET == null)
				{
					Class<?> fml = Class.forName("net.neoforged.fml.loading.FMLLoader");
					FMLLOADER_VERSION_INFO = fml.getMethod("versionInfo");
					Class<?> vi = Class.forName("net.neoforged.fml.loading.VersionInfo");
					MC_VERSION = vi.getMethod("mcVersion");
					NEOFORGE_VERSION = vi.getMethod("neoForgeVersion");
					Class<?> lml = Class.forName("net.neoforged.fml.loading.LoadingModList");
					LOADING_GET = lml.getMethod("get");
					LOADING_MODS = lml.getMethod("getMods");
					Class<?> info = Class.forName("net.neoforged.fml.loading.moddiscovery.ModInfo");
					INFO_ID = info.getMethod("getModId");
					INFO_VERSION = info.getMethod("getVersion");
				}
				Object versionInfo = FMLLOADER_VERSION_INFO.invoke(null);
				mods.put("minecraft", String.valueOf(MC_VERSION.invoke(versionInfo)));
				mods.put("neoforge", String.valueOf(NEOFORGE_VERSION.invoke(versionInfo)));
				Object loading = LOADING_GET.invoke(null);
				if (loading != null)
				{
					@SuppressWarnings("unchecked")
					List<Object> infos = (List<Object>) LOADING_MODS.invoke(loading);
					for (Object mod : infos)
					{
						mods.put(String.valueOf(INFO_ID.invoke(mod)), String.valueOf(INFO_VERSION.invoke(mod)));
					}
				}
			}
			catch (ReflectiveOperationException e)
			{
				throw new IllegalStateException("NeoForge pre-launch mod list is unavailable", e);
			}
			early = mods;
			return early;
		}

		@Override
		public boolean isModLoaded(String modId)
		{
			try
			{
				Object loaded = modList();
				if (loaded != null)
				{
					return (Boolean) IS_LOADED.invoke(loaded, modId);
				}
				return earlyMods().containsKey(modId);
			}
			catch (ReflectiveOperationException e)
			{
				throw new IllegalStateException("ModList#isLoaded failed for " + modId, e);
			}
		}

		@Override
		public Optional<ModContainer> getModContainer(String modId)
		{
			try
			{
				Object loaded = modList();
				if (loaded != null)
				{
					@SuppressWarnings("unchecked")
					Optional<Object> found = (Optional<Object>) GET_CONTAINER.invoke(loaded, modId);
					return found.map(NeoModContainer::new);
				}
				String version = earlyMods().get(modId);
				return version == null ? Optional.empty() : Optional.of(new FixedModContainer(version));
			}
			catch (ReflectiveOperationException e)
			{
				throw new IllegalStateException("ModList#getModContainerById failed for " + modId, e);
			}
		}
	}

	/**
	 * Adapts a NeoForge {@code ModContainer} (object identity only, via reflection) to the fabric-loader shape.
	 */
	final class NeoModContainer implements ModContainer
	{
		private final Object delegate;

		NeoModContainer(Object delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public ModMetadata getMetadata()
		{
			return () -> () -> {
				try
				{
					Object info = this.delegate.getClass().getMethod("getModInfo").invoke(this.delegate);
					Object version = info.getClass().getMethod("getVersion").invoke(info);
					return String.valueOf(version);
				}
				catch (ReflectiveOperationException e)
				{
					throw new IllegalStateException("Cannot read mod version", e);
				}
			};
		}
	}

	/**
	 * Carries a version string known before {@code ModList} exists.
	 */
	final class FixedModContainer implements ModContainer
	{
		private final String version;

		FixedModContainer(String version)
		{
			this.version = version;
		}

		@Override
		public ModMetadata getMetadata()
		{
			return () -> () -> this.version;
		}
	}
}
