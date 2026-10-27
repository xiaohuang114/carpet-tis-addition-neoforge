package net.fabricmc.loader.api.metadata.version;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.loader.api.Version;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * TIS-S2 dev shim for fabric-loader's {@code VersionPredicate}.
 *
 * <p>Supports the predicate syntax that actually appears in the TISCM sources:
 * {@code >=1.17}, {@code <1.20.3}, {@code <=1.21.1}, {@code >1.20.2 <26.3} (space = AND),
 * {@code >=0.27.14 <0.28}, {@code <1.21.9-} and {@code a || b} (double pipe = OR).
 * Wildcards ({@code 1.21.x} / {@code 1.21.*}) compare by prefix.</p>
 *
 * <p>Comparison is delegated to Maven's {@link ComparableVersion}, which is the same ordering NeoForge
 * uses for mod versions ({@code IModInfo#getVersion()} is an {@code ArtifactVersion}).</p>
 */
public interface VersionPredicate extends Predicate<Version>
{
	static VersionPredicate parse(String predicate)
	{
		return new Parsed(predicate);
	}

	/**
	 * Parsed form of a predicate string.
	 */
	final class Parsed implements VersionPredicate
	{
		private final List<List<String>> alternatives = new ArrayList<>();

		Parsed(String predicate)
		{
			for (String alternative : predicate.split("\\|\\|"))
			{
				List<String> clauses = new ArrayList<>();
				for (String clause : alternative.trim().split("\\s+"))
				{
					if (!clause.isEmpty())
					{
						clauses.add(clause);
					}
				}
				if (!clauses.isEmpty())
				{
					this.alternatives.add(clauses);
				}
			}
			if (this.alternatives.isEmpty())
			{
				throw new IllegalArgumentException("Empty version predicate: " + predicate);
			}
		}

		@Override
		public boolean test(Version version)
		{
			ComparableVersion actual = new ComparableVersion(version.getFriendlyString());
			for (List<String> clauses : this.alternatives)
			{
				boolean all = true;
				for (String clause : clauses)
				{
					if (!matches(actual, clause))
					{
						all = false;
						break;
					}
				}
				if (all)
				{
					return true;
				}
			}
			return false;
		}

		private static boolean matches(ComparableVersion actual, String clause)
		{
			String op = "";
			String rest = clause;
			for (String candidate : new String[]{">=", "<=", ">", "<", "=", "^", "~"})
			{
				if (clause.startsWith(candidate))
				{
					op = candidate;
					rest = clause.substring(candidate.length()).trim();
					break;
				}
			}
			if (rest.endsWith(".*"))
			{
				rest = rest.substring(0, rest.length() - 2) + ".x";
			}
			if (rest.endsWith(".x"))
			{
				String prefix = rest.substring(0, rest.length() - 1 - 1); // strip ".x"
				return actual.toString().equals(prefix) || actual.toString().startsWith(prefix + ".")
						|| actual.toString().startsWith(prefix + "-");
			}
			int cmp = actual.compareTo(new ComparableVersion(rest));
			switch (op)
			{
				case ">=":
					return cmp >= 0;
				case "<=":
					return cmp <= 0;
				case ">":
					return cmp > 0;
				case "<":
					return cmp < 0;
				default: // "=", "^", "~" and bare version all behave like an equality test here
					return cmp == 0;
			}
		}
	}
}
