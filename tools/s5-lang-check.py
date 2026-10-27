# S5 acceptance helper: compare src yml (nested) with build json (flat).
# Usage: python tools/s5-lang-check.py
# Notes:
#  - The yml is a nested map; only "simple" scalars are compared here.
#  - Keys whose value is a YAML block scalar (|- / | / >-) are reported separately,
#    because flattening those needs a real YAML parser (pyyaml is not installed).
#  - Literal "\\n" in yml is normalised to a real newline before comparison.
import json, os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(BASE, "src", "main", "resources", "assets", "carpettisaddition", "lang")
DST = os.path.join(BASE, "build", "resources", "main", "assets", "carpettisaddition", "lang")
BLOCK = ("|-", "|", ">-", ">")


def flatten_yml(path):
    simple, blocks = {}, set()
    stack = []
    pending_block = None  # (keypath, indent)
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            indent = len(line) - len(line.lstrip(" "))
            if pending_block is not None:
                pk, pind = pending_block
                if indent > pind:
                    continue  # body line of the block scalar
                pending_block = None
            body = line.strip()
            if ":" not in body:
                continue
            k, _, v = body.partition(":")
            k = k.strip().strip('"').strip("'")
            v = v.strip()
            while stack and stack[-1][0] >= indent:
                stack.pop()
            keypath = ".".join([s[1] for s in stack] + [k])
            if v == "":
                stack.append((indent, k))
                continue
            if v in BLOCK:
                blocks.add(keypath)
                pending_block = (keypath, indent)
                continue
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
                v = v[1:-1]
            simple[keypath] = v
    return simple, blocks


def norm(s):
    return s.replace("\\n", "\n")


for lang in ("en_us", "zh_cn"):
    simple, blocks = flatten_yml(os.path.join(SRC, lang + ".yml"))
    with open(os.path.join(DST, lang + ".json"), "r", encoding="utf-8") as f:
        js = json.load(f)
    missing = sorted(set(simple) - set(js))
    diff = sorted(k for k in set(simple) & set(js) if norm(simple[k]) != norm(js[k]))
    print("== %s" % lang)
    print("   json_keys=%d  yml_simple_keys=%d  yml_block_scalar_keys=%d"
          % (len(js), len(simple), len(blocks)))
    print("   simple_key_missing_in_json=%d  simple_key_value_mismatch=%d"
          % (len(missing), len(diff)))
    print("   block_scalar_keys_not_auto_compared=%d (need real YAML parser)" % len(blocks))
    for k in missing[:5]:
        print("     MISSING %s" % k)
    for k in diff[:5]:
        print("     DIFF    %s yml=%r json=%r" % (k, simple[k], js[k]))
    if lang == "en_us":
        samples = [k for k in js if k in simple and k.count(".") >= 3][:3]
        for k in samples:
            same = norm(simple[k]) == norm(js[k])
            print("   SAMPLE %-70s equal=%s value=%r" % (k, same, js[k]))
