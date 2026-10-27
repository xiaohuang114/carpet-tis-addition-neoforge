"""Simulate PCL2 CE's mods.toml mini-parser (ModLocalComp.vb:807-904) on a jar.

Faithfully mirrors the VB logic:
  * entry name is exactly 'META-INF/mods.toml'
  * strip '#...' comments, trim spaces/ideographic-space, drop empty lines
  * line starting '[' and ending ']'  -> section header (Trim("[]"))
  * line containing '=' -> key / raw value; value forms: "..." | ''' multi-line | true/false | number | raw
  * section named 'mods' must contain 'modId'; displayName/description/version read from it
  * displayURL / authors read from TomlData(0)  (the root section)

Usage: python pcl-parse-sim.py <jar> [<jar> ...]
"""
import sys
import zipfile


def read_text(entry_bytes):
    for enc in ("utf-8-sig", "utf-8", "gbk", "latin-1"):
        try:
            return entry_bytes.decode(enc)
        except UnicodeDecodeError:
            continue
    return entry_bytes.decode("latin-1", "replace")


def parse(toml_text):
    sections = [("", {})]
    lines = []
    for line in toml_text.replace("\r\n", "\n").replace("\r", "\n").split("\n"):
        if line.startswith("#"):
            continue
        if "#" in line:
            line = line[: line.index("#")]
        line = line.strip(" \t\u3000")
        if line:
            lines.append(line)

    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("[") and line.endswith("]"):
            sections.append((line.strip("[]"), {}))
        elif "=" in line:
            key = line[: line.index("=")].rstrip(" \t\u3000")
            raw = line[line.index("=") + 1 :].lstrip(" \t\u3000")
            if raw.startswith('"') and raw.endswith('"'):
                value = raw.strip('"')
            elif raw.startswith("'''"):
                chunk = [raw.lstrip("'")]
                if chunk[0].endswith("'''"):
                    chunk[0] = chunk[0].rstrip("'")
                else:
                    while i < len(lines) - 1:
                        i += 1
                        nxt = lines[i]
                        if nxt.endswith("'''"):
                            chunk.append(nxt.rstrip("'"))
                            break
                        chunk.append(nxt)
                value = "\n".join(chunk).strip("\n")
            elif raw.lower() in ("true", "false"):
                value = raw.lower() == "true"
            else:
                try:
                    num = float(raw)
                    value = int(num) if num == int(num) else num
                except ValueError:
                    value = raw
            sections[-1][1][key] = value
        i += 1
    return sections


def check(jar_path):
    print("== %s" % jar_path)
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        entry = "META-INF/mods.toml"
        if entry not in names:
            print("   FAIL  %s not present -> PCL2 falls back to filename" % entry)
            return False
        text = read_text(jar.read(entry))
        print("   entry %s : %d chars" % (entry, len(text)))
        sections = parse(text)
        mod_entry = next((d for name, d in sections if name == "mods"), None)
        if mod_entry is None or "modId" not in mod_entry:
            print("   FAIL  no [mods] section with modId")
            return False
        mod_id = mod_entry["modId"]
        print("   modId       = %s" % mod_id)
        print("   displayName = %s" % mod_entry.get("displayName", "(missing -> shows modId)"))
        ver = mod_entry.get("version", "(none)")
        print("   version     = %s" % ver)
        desc = str(mod_entry.get("description", ""))
        print("   description = %s" % (desc[:46] + ("..." if len(desc) > 46 else "")))
        root = sections[0][1]
        print("   root authors     = %s" % root.get("authors", "(none)"))
        print("   root displayURL  = %s" % root.get("displayURL", "(none)"))
        print("   RESULT: PCL2 will show '%s'" % mod_entry.get("displayName", mod_id))
        return True


if __name__ == "__main__":
    ok = all([check(p) for p in sys.argv[1:]] or [True])
    sys.exit(0 if ok else 1)
