package net.fabricmc.loader.api;

import net.fabricmc.loader.api.metadata.ModMetadata;

/**
 * TIS-S2 dev shim: minimal stand-in for fabric-loader's {@code net.fabricmc.loader.api.ModContainer}.
 * conditional-mixin only ever calls {@link #getMetadata()} on it (see {@code VersionCheckerImpl}).
 */
public interface ModContainer
{
	ModMetadata getMetadata();
}
