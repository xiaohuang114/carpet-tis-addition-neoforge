# -*- coding: utf-8 -*-
"""S5 记账：把 S5 归档写进 PORT-STATE.md / BLOCKERS.md / MIXIN-LOG.md（幂等：已存在则跳过）"""
import io
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(p):
    return io.open(os.path.join(BASE, p), encoding='utf-8').read()


def write(p, s):
    io.open(os.path.join(BASE, p), 'w', encoding='utf-8', newline='\n').write(s)


def append(p, marker, text):
    s = read(p)
    if marker in s:
        print('SKIP (already present): %s :: %s' % (p, marker))
        return
    if not s.endswith('\n'):
        s += '\n'
    write(p, s + text)
    print('APPENDED: %s' % p)


# ---------------------------------------------------------------- PORT-STATE §13
PORT13 = r'''
---

## 13. S5 归档：功能验收（RCON 通道）

- 目标：只做功能验收（跑命令、看行为、记证据）。**1009 个 .java 业务逻辑一字节未动**，未出包、未改 lang 源、未动 mixin 配置与 toml。
- 通道：`tools\s5-rcon.ps1`（按任务书规格自实现的最小 RCON 客户端）+ `tools\s5-batch.ps1`（34 条批量）+ `tools\s5-report.py`（报告生成器）。服务器常驻方式：`cmd /c ".\gradlew.bat --no-daemon --console=plain -PtisFull runServer > _run_s5.log 2>&1"`。
- 运行事实：14:44:19 启动 → `Done` → 14:53:26 `/stop`，`_run_s5.log` 收尾 `BUILD SUCCESSFUL in 9m 29s`，无残留 java 进程。
- 逐条原始响应全表见 **`S5-ACCEPT.md`**；`build\s5-command-raw.txt` 为原始证据。

### 13.1 命令验收表（摘要）

| 判定 | 条数 | 命令 |
|---|---|---|
| OK | 15 | `/carpet`、`/tick`(裸)、`/tick query|freeze|step 5|rate 40|unfreeze`、`/player Steve kill`、`/log`、`/log mobcaps|microTiming|phantom`、`/lifetime`、`/lifetime tracking`、`/raid list` |
| INCOMPLETE-OK | 4 | `/info entity`、`/info block`、`/info world`、`/spawn`（`/help <root>` 均能列出真实用法） |
| MISSING | 10 | `/tis`、`/tis help`、`/tis info`、`/tis raid`、`/tis speedtest`、`/counter`、`/counter redstone`、`/counter dye`、`/man`、`/stat` |
| ERROR | 1 | `/player Steve spawn`（RCON 响应体为空，假玩家未生成） |
| SPEC-MISMATCH | 4 | `/carpet tisAdditionEnabled` 的四个分支（规则名在 1.82 源码中不存在） |
| BARE-KEY | **0** | 34 条响应里无任何 `carpettisaddition.*` / `carpet.*.rule.*` 裸 key |

MISSING 的 4 个根节点（`/tis` `/counter` `/man` `/stat`）在源码里 grep `literal(...)` 全为 0，`/help <root>` 也一律 `Unknown command or insufficient permissions` → 属**验收清单与 1.82 实际命令树不符**，不是移植缺陷（B-S5-3）。1.82 的真实命令面：`/carpet` `/profile` `/log` `/spawn` `/player` `/info` `/distance` `/perimeterinfo` `/draw` `/script` `/track` `/lifetime` `/refresh` `/raycast` `/removeentity` `/sleep` `/raid` `/scounter` `/xcounter` `/speedtest` `/manipulate`（后四条受 `hopperNoItemCost` / `hopperXpCounters` / `commandSpeedTest` / `commandManipulate` 规则门控，默认关闭时 `/help` 按 `requires` 过滤不可见）。

### 13.2 翻译链路（S2 自造 convertTranslations 的实测）

| 项 | 结果 |
|---|---|
| `build\resources\main\assets\carpettisaddition\lang\en_us.json` | 存在，133325 B，**1142 key** |
| `zh_cn.json` | 存在，**1142 key** |
| `src\main\resources\...\lang\` | 仍是 `en_us.yml` / `zh_cn.yml`（转换只在构建期，源文件未改） |
| yml ≈ json 抽样比对 | 3/3 键值相等；展平覆盖：en_us 1130 简单键 + 11 block scalar，json 缺 0；zh_cn 不一致 0 |

结论：翻译管线可用，裸 key 在 34 条命令响应中出现 **0** 次。

### 13.3 记录器落盘路径（COMPACT_NAME 实测）

```
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\lifetime\recorder_config.json
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn-1.21.1+build.3-v2.tiny
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn_version.json
```

目录名 = **`carpettisaddition`**（不是 `carpet_tis_addition`），与 S2 的 `COMPACT_NAME` 结论一致。`/lifetime tracking start|stop` 实测可用（`Lifetime Tracker started` / `Tracked 0.00 min ... No result yet`）。`/counter` 不存在，等价计数命令为 `/scounter`、`/xcounter`（内存计数 + `/log`，不落盘）。

### 13.4 日志与自检

- `run\logs\latest.log`：`[*/ERROR]` 9 条 + `[FATAL]` 1 条，全部归因（5 条 `Lithium*Tester` 实例化失败 → B-S5-2；3 条 `createFake delayed task error` → B-S5-1；1 条 `Error executing task on Server` → B-S5-2）。`compatibility level` 命中 3 行 = 2 WARN（`carpet-tis-addition.mixins.json` `JAVA_21`、`carpet.mixins.json` `JAVA_17`）+ 1 INFO，符合"允许 3 条"。
- 自检：`require\s*=\s*0` → **4**；`@Overwrite` → **2**；`TODO|FIXME|UnsupportedOperationException`（`-CaseSensitive`）→ **21**（不加开关为 22，多出的是 `SpeedTestPinger.java:146` `mapToDouble` 误报）；`net\.fabricmc` → **8 行**；`(method|field)_\d+` → **0**。与基线完全一致。
- `build\restriction-skipped.txt`：**71 条** `[TISCM] Disabled mixin` 行（17 `required mod ... not found` + 1 级联 `required mixin class` + 53 `version predicates`），逐条判读见 `MIXIN-LOG.md` §11，**全部合规**。

### 13.5 两条技术债（本轮只记录，不改代码）

1. **`logger\phantom\PhantomSpawnerMixin` 的 `index = 19` 硬编码局部变量槽位**
   `src\main\java\carpettisaddition\mixins\logger\phantom\PhantomSpawnerMixin.java:66` `index = 19`，注释自述"NeoForge supplies is stored at offset 353 (istore 19 …), implicit int matching is ambiguous (Found 2..5 candidate variables but exactly 1 is required)"。
   风险：槽位号是 1.21.1 + NeoForge 21.1.209 这一组具体编译产物的产物，MC 小版本或 NeoForge 补丁改了 `PhantomSpawner.tick` 的局部变量表就会静默绑错变量（注错对象不会报错，只会行为错）。正确做法是改按语义定位（`@Local(ordinal=…)` + 类型约束，或 `@At` 锚定 `istore` 前的调用）——属 .java 改动，S5 铁律禁改，留档。
2. **`carpet\hooks\CarpetServerMixin` 依赖 carpet fork 的私有字段 `CarpetServer.minecraft_server`**
   `src\main\java\carpettisaddition\mixins\carpet\hooks\CarpetServerMixin.java:52` `target = "Lcarpet/CarpetServer;minecraft_server:Lnet/minecraft/server/MinecraftServer;"`，`@Inject(at = FIELD, opcode = PUTSTATIC, remap = false)`。
   风险：`remap = false` + 直接指向 fork 的私有字段名 —— 上游 fabric-carpet 没有这个字段（fork 为了让 `onServerLoaded(ServerAboutToStartEvent)` 传服务器对象才加的）。一旦 fork 改名/改可见性，本 mixin 直接 `InvalidInjectionException`，且 `CarpetTISAdditionServer.minecraft_server` 会一直是 null → 所有 rule validator 的服务器上下文失效。属跨仓库耦合债，留档。

### 13.6 边界

- 未改：1009 个 .java 业务逻辑、`lang\*.yml`、`carpet-tis-addition.mixins.json`、`neoforge.mods.toml`、`libs\*`、`run\mods\carpet.jar`。
- 已改：`run\server.properties`（验收期 `enable-rcon=true`，结束时改回 `false` 并留注释）、`tools\s5-rcon.ps1` / `s5-batch.ps1` / `s5-report.py` / `s5-state-writeback.py`（新建）、三份状态文件 + `S5-ACCEPT.md`。
- `CarpetTISAdditionMixinPlugin` 保持 S4 的 `LOGGER.info`（抓跳过名单用），未回调 debug。

### 13.7 下一步（仅一条）

**S6：修 B-S5-1（`CustomPayloadC2SPacketMixin` 的 `@ModifyArg` 改锚 `CustomPacketPayload.codec` 的真实调用点）与 B-S5-2（`LithiumConfigAccess` 静态初始化去掉 mixin 阶段的 `ModList.get()`），修完重跑 `/player <name> spawn` 与 `hopperXpCounters` 两条用例。**
'''

append('PORT-STATE.md', '## 13. S5 归档', PORT13)


# ---------------------------------------------------------------- BLOCKERS
s = read('BLOCKERS.md')
old_h = '### B-S3-1（真阻塞，属 S4 范围）24 条 mixin 注入在 `defaultRequire=1` 下必失败'
new_h = '### B-S3-1（**已于 S4 关闭**）24 条 mixin 注入在 `defaultRequire=1` 下必失败'
if old_h in s:
    write('BLOCKERS.md', s.replace(old_h, new_h))
    print('PATCHED: BLOCKERS.md B-S3-1 header')
else:
    print('SKIP (header already patched or missing): BLOCKERS.md')

BLOCK14 = r'''
## 14. S4 关闭记录 + S5 阻塞项

### 14.1 B-S3-1 关闭（S4 完成，S5 复核）

- 24 条全部修完：A 类 15 条把 `method_\d+` / `field_\d+` 换成 `javap -p` 实测的 Mojmap/合成名，B 类 9 条对齐 fork 与 1.21.1 签名。
- 复核证据（S5 阶段实测，非重新核对）：`_run_s4_final.log` = `_run_s4_iter12.log` 原件，`Done (4.044s)!`，`Mixin apply failed` / `Critical injection failure` / `InvalidInjectionException` / `Invalid descriptor on` 全为 0；全仓库 `(method|field)_\d+` 命中 **0**；`_run_s5.log` 本轮再次跑到 `Done` 且同样 4 类致命串 0 条。
- 状态：**CLOSED**。原处置口径（不许 require=0 / 不许删条目）在本阶段继续遵守。

### B-S5-1（真阻塞）`CustomPayloadC2SPacketMixin` 的 `@ModifyArg` 在 1.21.1 下 0 候选 → `GameProtocols` clinit 炸 → 假玩家全链路失效

- 现象（`run\logs\latest.log:942`，`_run_s5.log` 同）：
  ```
  [Server thread/ERROR] [mixin/]: <!-- /* InjectionError: Critical injection failure: Argument modifier method
  registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; in carpet-tis-addition.mixins.json:network.CustomPayloadC2SPacketMixin
  from mod carpet_tis_addition failed injection check, (0/1) succeeded. Scanned 0 target(s). No refMap loaded. */ -->
  ```
- 影响链：`net.minecraft.network.protocol.game.GameProtocols` 的 `<clinit>` 依赖 `CustomPacketPayload.codec(...)` 的注入 → clinit 抛 `ExceptionInInitializerError` → TISCM 自定义 payload 注册表不完整 → `/player Steve spawn` 的假玩家 `createFake` 任务失败（响应体为空，`/list` 仍 0 玩家），日志三处 `createFake delayed task error`（L901 / L1029 / L1520）。
- 根因方向：目标方法锚在 `CustomPayloadC2SPacket` 的 `<clinit>` 内 `INVOKE CustomPacketPayload;codec(...)`，1.21.1 Mojmap 下该方法体的 codec 调用形态已变（`StreamCodec` 泛型擦除 + `codec` 变为 `CustomPacketPayload.Type` 相关的静态辅助），`@ModifyArg` 找不到可修饰的参数。
- 为什么不现场修：修它必须改 mixin `.java`（换 `@At` 目标或改 handler 描述符），S5 铁律只允许跑命令看行为 → 记 B-S5-x 并跳过该条用例。

### B-S5-2（真阻塞）`LithiumConfigAccess` 静态初始化在 mixin 阶段调用 `ModList.get()` → `NoClassDefFoundError`

- 现象：
  - 启动期 5 条 ERROR（L39/L40/L54/L86/L87）：`Failed to instantiate a ConditionTester from class carpettisaddition.utils.mixin.testers.LithiumEntityWorldTickSchedulerTester[ $Inverted] / LithiumBlockHopperTester / LithiumEntityMovementOptimizationTester: java.lang.reflect.InvocationTargetException`。这些 tester 是 conditional-mixin 的 `@Restriction(require = @ConditionType(...))` 判定器，构造失败 → 相关 mixin 的跳过判定退化为"最保守"。
  - 运行期：`/carpet hopperXpCounters true` → `NoClassDefFoundError: Could not initialize class carpettisaddition.utils.mixin.LithiumConfigAccess`，`BlockableEventLoop` 打 `[FATAL] Error executing task on Server`（L1411）。
- 根因：S2 把该类的 `isModLoaded(x)` 改成 `ModList.get().isLoaded(x)`（`LithiumConfigAccess.java:39`），但静态字段初始化（`:35`）在 Mixin `PREPARE` 阶段就可能触发，此时 `ModList.get()` 仍为 null；S3 给 fabric-loader-shim 加的 early-bootstrap 分支没覆盖这条直连 NeoForge API 的路径。
- 为什么不现场修：属 .java 改动（把静态初始化改成惰性 + null 守卫），S5 禁改。

### B-S5-3（口径阻塞，非代码缺陷）验收清单与 1.82 实际命令树不符

- `/tis`（含 `help/info/raid/speedtest`）、`/counter`（含 `redstone/dye`）、`/man`、`/stat` 共 10 条在 1.82 源码中**没有对应根节点**（`literal("tis"|"counter"|"man"|"stat")` 全 0 命中，`/help <root>` 一律 Unknown command）。
- `/carpet tisAdditionEnabled` 四个分支同理：1.82 的 `CarpetTISAdditionSettings` 里没有 `tisAdditionEnabled` 这个规则。
- 处置：按任务书判定口径记为 MISSING / SPEC-MISMATCH，**不当作移植缺陷**；后续验收清单应按真实命令树重写（见 `S5-ACCEPT.md` §2.1）。

## 15. 下一步（仅一条）

**S6：修 B-S5-1 与 B-S5-2 两处 .java（`network\CustomPayloadC2SPacketMixin.java` 换 `@At` 锚点；`utils\compat\LithiumConfigAccess.java` 静态初始化改惰性 + 空守卫），修完重跑 `/player <name> spawn`、`/carpet hopperXpCounters true` 两条用例，并确认 `latest.log` 的 9 ERROR + 1 FATAL 归零。**
'''

append('BLOCKERS.md', '## 14. S4 关闭记录 + S5 阻塞项', BLOCK14)


# ---------------------------------------------------------------- MIXIN-LOG §10 / §11
MIX10 = r'''
---

## 10. S4 定稿账目（补记）

| 项 | 值 | 证据 |
|---|---|---|
| 正式配置注册条目 | 587 + client 12 | `carpet-tis-addition.mixins.json` |
| `Done` 时间 | **4.044s** | `_run_s4_iter12.log`（原件另存 `_run_s4_final.log`） |
| applied | 439 条 | 同上 |
| `Mixin apply failed` / `Critical injection failure` / `InvalidInjectionException` / `Invalid descriptor on` | 0 / 0 / 0 / 0 | 同上 |
| 全仓库 `(method|field)_\d+` | 0 命中 | S5 复核 |
| S3 的 `TranslatableTextAccessor` ClassCastException | 消失 | 同上 |

> §1 / §3 原先写的 `Done (4.743s)` 是笔误，已在本轮按原件订正为 `4.044s`。

---

## 11. `@Restriction` 跳过名单判读（`build\restriction-skipped.txt`，71 条）

生成方式：`CarpetTISAdditionMixinPlugin.onRestrictionCheckFailed` 的日志级别在 S4 已由 `LOGGER.debug` 改为 `LOGGER.info`，本轮带 `-Dmixin.debug.verbose=true` 跑一次 `runServer`，抓全部 `[TISCM] Disabled mixin ... due to ...` 行。

统计：**71 条 = 53 条版本谓词 + 17 条依赖 mod 缺失 + 1 条级联**；**判为"条件写错（缺陷）"的：0 条**。

口径：
- **Fabric 专属（合规）**：`required mod X not found` —— 依赖的是 Fabric 生态 mod（lithium / tic_tacs / mctester / fabric-transfer-api-v1 / malilib / minihud），NeoForge 侧不存在这些 mod id；`libs\lithium.jar` 只是编译期 classpath，不是 mod，故判"找不到"是**正确行为**。
- **版本谓词（合规）**：`mod minecraft@1.21.1 does not matches version predicates [...]` —— 上游为其它 MC 版本写的兼容分支（`<1.15` / `<1.16` / `<1.16.2` / `<1.17` / `<1.18` / `<1.19` / `<=1.19` / `<=1.19.4-pre3` / `<=1.20.2` / `<1.20.3` / `<1.20.4` / `<1.20.2-alpha.0` / `>=1.16 <1.17` / `>1.21.1` / `>=26.2`），1.21.1 不满足是设计意图。
- **级联（合规）**：`required mixin class ... disabled` —— 依赖的 `ItemStorageMixin` 已因 fabric-transfer-api-v1 缺失被跳过。

> 注意：本阶段真正暴露的两处**缺陷**（`CustomPayloadC2SPacketMixin` 注入 0 候选、`LithiumConfigAccess` 静态初始化炸）**不在**这份跳过名单里 —— 它们是"没被跳过但注入/初始化失败"，见 `BLOCKERS.md` B-S5-1 / B-S5-2。

| # | mixin 条目 | 跳过原因 | 判读 |
|---|---|---|---|
'''

lines = io.open(os.path.join(BASE, 'build', 'restriction-skipped.txt'), encoding='utf-8').read().splitlines()
rows = []
for i, ln in enumerate(lines, 1):
    m = re.search(r'Disabled mixin (\S+) due to (.*)$', ln)
    name, reason = (m.group(1), m.group(2)) if m else (ln, '')
    if 'required mod' in reason:
        mod = reason.replace('required mod ', '').replace(' not found', '')
        verdict = 'Fabric 专属（合规）—— `%s` 不是 NeoForge mod' % mod
    elif 'required mixin class' in reason:
        verdict = '级联（合规）—— 依赖条目已被跳过'
    elif 'version predicates' in reason:
        pr = re.search(r'\[(.*)\]$', reason)
        verdict = '版本谓词（合规）—— `%s` 不含 1.21.1' % (pr.group(1) if pr else '?')
    else:
        verdict = '未知原因 —— 需人工判读'
    rows.append('| %d | `%s` | %s | %s |' % (i, name.replace('carpettisaddition.mixins.', ''), reason.replace('|', r'\|'), verdict))

MIX10 += '\n'.join(rows) + '\n'
append('MIXIN-LOG.md', '## 11. `@Restriction` 跳过名单判读', MIX10)
print('ROWS = %d' % len(rows))
