package net.fabricmc.loader.api;

/**
 * TIS-S2 dev shim: minimal stand-in for fabric-loader's {@code net.fabricmc.loader.api.Version}.
 *
 * <p>Only the surface that conditional-mixin 0.6.4 actually links against is provided here.
 * The real implementation is backed by NeoForge's mod metadata; see
 * {@code net.fabricmc.loader.api.FabricLoader.Impl}.</p>
 *
 * <p>This package is NOT part of the ported mod source: it lives in {@code compat-src/} and is compiled
 * into the dev-only classpath jar produced by the {@code stripConditionalMixin} Gradle task.
 * It disappears once conditional-mixin is actually ported (S3).</p>
 */
public interface Version
{
	/**
	 * Mirrors fabric-loader's {@code Version#getFriendlyString()}.
	 */
	String getFriendlyString();
}
