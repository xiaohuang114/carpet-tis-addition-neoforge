package net.fabricmc.loader.api.metadata;

import net.fabricmc.loader.api.Version;

/**
 * TIS-S2 dev shim: minimal stand-in for fabric-loader's {@code ModMetadata}.
 * conditional-mixin only ever calls {@link #getVersion()} on it.
 */
public interface ModMetadata
{
	Version getVersion();
}
