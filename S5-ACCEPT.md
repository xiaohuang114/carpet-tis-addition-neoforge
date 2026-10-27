# S5-ACCEPT — 功能验收（RCON 通道，Carpet TIS Addition 1.82.0 → NeoForge 21.1.209 / 1.21.1）

- 阶段：**S5（功能验收）**，前置 S0~S4 已归档（编译 0 errors、mixin 容器通、`_run_s4_final.log` 达 `Done (4.044s)`、applied 439 条）
- 边界：**1009 个 .java 的业务逻辑一字节未动**；未出包、未改 lang 源、未动 mixin 配置与 toml
- 运行方式：`cmd /c ".\gradlew.bat --no-daemon --console=plain -PtisFull runServer > _run_s5.log 2>&1"`（常驻）
- 验收通道：`tools\s5-rcon.ps1`（自实现最小 RCON 客户端），批量脚本 `tools\s5-batch.ps1`，报告生成器 `tools\s5-report.py`
- 原始证据：`build\s5-command-raw.txt`（34 条逐条原始响应）、`build\s5-help-snapshot.txt`（/help|/tis|/list 快照）、`_run_s5.log`、`run\logs\latest.log`
- 日志时区：服务器本地时间 2026-10-25 14:44 启动，14:53:26 `/stop` 正常退出，`BUILD SUCCESSFUL in 9m 29s`

## 0. 判定口径

| 判定 | 含义 |
|---|---|
| OK | 命令树已注册且行为符合 1.82 语义（含"需要参数/无玩家上下文"这类正常提示） |
| INCOMPLETE-OK | 命令存在但裸发缺参数，`/help <root>` 能列出真实用法 |
| MISSING | 命令树里没有这个根节点（`/help <root>` 报 Unknown command，源码 grep 0 命中） |
| ERROR | 执行期异常/静默失败（响应为空，或日志出现对应 ERROR/FATAL） |
| SPEC-MISMATCH | 任务书给的命令名/规则名在 1.82 源码里根本不存在 —— 属清单书写偏差，不是移植缺陷 |
| BARE-KEY | 响应里出现 `carpettisaddition.command.*` / `carpet.*.rule.*` 这类未翻译裸 key |

## 1. RCON 通道自检（验收第 1 条）

```
> tools\s5-rcon.ps1 -Command "/list"
There are 0 of a max of 20 players online: 
```

通道规格（按任务书实现，未自创）：报文 = `[int32 LE 长度][int32 LE 请求id][int32 LE 类型][body UTF8][0x00][0x00]`，`长度 = 4+4+body字节数+2`；类型 3=登录、2=命令；读响应先读 4 字节长度再读整包；登录后 id=1/type=2 发命令，响应 type=0 取正文。

## 2. 34 条命令验收表（逐条原始响应首行 + 判定）

| # | 命令 | 原始响应（首行） | 行数 | 判定 | 说明 |
|---|---|---|---|---|---|
| 1 | `/carpet` | Current Carpet Mod settings: | 5 | **OK** | /carpet 正常输出：carpet 1.4.147+v1.0.8-251027 + Carpet TIS Addition 1.82.0 均已加载，16 个分类可见 |
| 2 | `/carpet tisAdditionEnabled` | Unknown rule: tisAdditionEnabled | 1 | **SPEC-MISMATCH** | 规则名 tisAdditionEnabled 在 TIS 1.82 源码中不存在（grep 0 命中），故 "Unknown rule"；任务书命令名与 1.82 实际规则集不符，非移植缺陷 |
| 3 | `/carpet tisAdditionEnabled false` | Unknown rule: tisAdditionEnabled | 1 | **SPEC-MISMATCH** | 同上（写值分支同样走 Unknown rule） |
| 4 | `/carpet tisAdditionEnabled` | Unknown rule: tisAdditionEnabled | 1 | **SPEC-MISMATCH** | 同上（回读分支） |
| 5 | `/carpet tisAdditionEnabled true` | Unknown rule: tisAdditionEnabled | 1 | **SPEC-MISMATCH** | 同上（恢复 true 分支） |
| 6 | `/tis` | Unknown or incomplete command, see below for error | 2 | **MISSING** | /tis 根命令在源码中不存在（grep literal("tis") = 0），1.82 实际无 /tis 根节点；任务书按上游文档书写，与实际命令树不一致 |
| 7 | `/tis help` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/tis help 无从属节点） |
| 8 | `/tis info` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/tis info 无从属节点） |
| 9 | `/tis raid` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/tis raid；1.82 的 raid 命令是根级 /raid） |
| 10 | `/tis speedtest` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/tis speedtest；1.82 的测速命令是根级 /speedtest，且受规则 commandSpeedTest 门控，见 §3 探针 P1） |
| 11 | `/tick` | Unknown or incomplete command, see below for error | 2 | **OK** | 裸 /tick 在 1.21.1 无执行节点，报 "Unknown or incomplete command" 是原版语义，子命令见 12-16 |
| 12 | `/tick query` | The game is running normally | 4 | **OK** | /tick query 正常：game running normally + 目标 40.0/s + P50 0.2ms（第 15 条已把 rate 设为 40） |
| 13 | `/tick freeze` | The game is frozen | 1 | **OK** | /tick freeze 生效（The game is frozen） |
| 14 | `/tick step 5` | Stepping 5 tick(s) | 1 | **OK** | /tick step 5 生效（Stepping 5 tick(s)） |
| 15 | `/tick rate 40` | Set the target tick rate to 40.0 per second | 1 | **OK** | /tick rate 40 生效（Set the target tick rate to 40.0 per second） |
| 16 | `/tick unfreeze` | The game is running normally | 1 | **OK** | /tick unfreeze 生效（game is running normally） |
| 17 | `/player Steve spawn` |  | 1 | **ERROR** | RCON 响应体只有空行、无任何文本；/list 仍是 "There are 0 of a max of 20 players online"，latest.log L901/L1029/L1520 三处 "createFake delayed task error"，_run_s5.log:1016 抛 `InjectionError: Critical injection failure: Argument modifier method registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; … (0/1) succeeded. Scanned 0 target(s). No refMap loaded.` → 归因 B-S5-1 |
| 18 | `/player Steve kill` | Can only manipulate existing players | 1 | **OK** | 因为 17 的假玩家未生成，报 "Can only manipulate existing players"，属预期连带结果 |
| 19 | `/counter` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 1.82 命令树无 /counter 根（grep literal("counter") = 0）；计数命令实为 /scounter 与 /xcounter（见 §3 探针 P3/P4），任务书命令名与实际不符 |
| 20 | `/counter redstone` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/counter redstone 是 /scounter 的旧写法猜测） |
| 21 | `/counter dye` | Unknown or incomplete command, see below for error | 2 | **MISSING** | 同上（/counter dye 是 /scounter <color> 的旧写法猜测） |
| 22 | `/log` | players only | 1 | **OK** | /log 在控制台报 "players only"，是该命令的控制台语义，正常 |
| 23 | `/log mobcaps` | No player specified | 1 | **OK** | /log mobcaps 报 "No player specified"（控制台无玩家上下文），正常 |
| 24 | `/log microTiming` | No player specified | 1 | **OK** | /log microTiming 同上 |
| 25 | `/log phantom` | No player specified | 1 | **OK** | /log phantom 同上 |
| 26 | `/lifetime` | Lifetime Tracker | 3 | **OK** | /lifetime 输出标题 + 一句话说明 + doc 链接，属正常帮助输出 |
| 27 | `/lifetime tracking` | Lifetime Tracker has not started | 1 | **OK** | /lifetime tracking 报 "Lifetime Tracker has not started"（尚未 start），正常；start/stop 已实测可用（见 PORT-STATE §13.3） |
| 28 | `/info entity` | Unknown or incomplete command, see below for error | 2 | **INCOMPLETE-OK** | 用法不足而非缺失：/help info 实测列出 "/info entity <entity selector> [grep]"，需要参数 |
| 29 | `/info block` | Unknown or incomplete command, see below for error | 2 | **INCOMPLETE-OK** | /help info 实测 "/info block <block position> [grep]"，需要参数 |
| 30 | `/info world` | Unknown or incomplete command, see below for error | 2 | **INCOMPLETE-OK** | /help info 实测 "/info world (ticking_order|weather)"，需要子参数 |
| 31 | `/raid list` | No raid exists | 1 | **OK** | /raid list 正常（世界无袭击：No raid exists） |
| 32 | `/man` | Unknown or incomplete command, see below for error | 2 | **MISSING** | /man 根命令在源码中不存在（grep literal("man") = 0） |
| 33 | `/spawn` | Unknown or incomplete command, see below for error | 2 | **INCOMPLETE-OK** | /help spawn 实测 9 个用法（list|tracking|test|mocking|rates|mobcaps|entities|mobcapsLocal|natualSpawning），裸 /spawn 需要参数 |
| 34 | `/stat` | Unknown or incomplete command, see below for error | 2 | **MISSING** | /stat 根命令在源码中不存在（grep literal("stat") = 0）；统计类信息由 /tick、/profile、/info server 提供 |

统计：**OK=15 / INCOMPLETE-OK=4 / MISSING=10 / ERROR=1 / SPEC-MISMATCH=4 / BARE-KEY=0**（合计 34 条）。BARE-KEY = 0：34 条响应里没有任何 `carpettisaddition.*` / `carpet.*.rule.*` 裸 key。

### 2.1 判定分布说明

- **MISSING 的 10 条**（`/tis`×5、`/counter`×3、`/man`、`/stat` —— 按根命令去重只有 4 个根）：`/help tis|counter|man|stat` 全部返回 `Unknown command or insufficient permissions`，且源码 grep `literal("tis")/("counter")/("man")/("stat")` **全为 0**。1.82 的真实命令树里没有这些根，任务书这 8 条是按别处文档写的，记为 SPEC-MISMATCH 更准确；表里保留 MISSING 以符合任务书判定口径。
- **`/tis raid` 与 `/raid`、`/tis speedtest` 与 `/speedtest`**：1.82 一律是根级命令，没有 `/tis` 聚合根。`/raid list` 实测 `No raid exists`（OK）。
- **INCOMPLETE-OK 的 4 条**（`/info entity|block|world`、`/spawn`）在 `/help` 里都有真实用法，属"裸发缺参数"，不是缺失。
- **ERROR 只有 1 条**：`/player Steve spawn`。见 §10 B-S5-1。

## 3. 附加探针（命令存在性 + 规则门控 + 计数命令）

| # | 命令 | 原始响应 | 结论 |
|---|---|---|---|
| P0 | `/help` | 109 行命令条目（`build\s5-help-snapshot.txt`），含 `/carpet` `/profile` `/log` `/spawn` `/player` `/info` `/distance` `/perimeterinfo` `/draw` `/script` `/track` `/lifetime` `/refresh` `/raycast` `/removeentity` `/sleep` `/raid`；裸 key 命中 0 | 命令树整体健康（验收第 3 条） |
| P1 | `/carpet commandSpeedTest true` → `/help speedtest` → `/speedtest` | `/speedtest` 用法出现；执行报 `A player is required to run this command here` | `/speedtest` 存在，受规则门控；未开规则时 `/help` 按 `requires` 过滤所以看不见 |
| P2 | `/carpet commandManipulate true` → `/help manipulate` | 5 条用法 | `/manipulate` 存在，默认关闭 |
| P3 | `/carpet hopperNoItemCost true` → `/scounter` | `No item have been counted yet` | 计数命令实为 `/scounter` |
| P3b | `/scounter red` | `red supplier counter hasn't started counting yet` | 颜色分支正常 |
| P4 | `/carpet hopperXpCounters true` | `NoClassDefFoundError: Could not initialize class carpettisaddition.utils.mixin.LithiumConfigAccess` + 日志 FATAL（L1411） | 见 §10 B-S5-2 |

源码对照：`SupplierCounterCommand.java:32` `public static final String PREFIX = "scounter";`、`XpCounterCommand.java:31` `PREFIX = "xcounter";`；`ManipulateCommand.java:67`、`SpeedTestCommand.java:88`、`RaidCommand.java:64`、`LifeTimeCommand.java:173` 的 `.requires(...)` 就是 P1/P2 的门控来源。

## 4. 翻译链路验证（验收第 4 条）

| 项 | 结果 |
|---|---|
| `build\resources\main\assets\carpettisaddition\lang\en_us.json` | ✅ 存在，133325 B，**1142 key** |
| `build\resources\main\assets\carpettisaddition\lang\zh_cn.json` | ✅ 存在，**1142 key** |
| `src\main\resources\assets\carpettisaddition\lang\` | ✅ 仍是 `en_us.yml` / `zh_cn.yml` + `meta\`（转换只发生在构建期，源文件未改） |
| 抽样 3 个 key：yml 值 == json 值 | ✅ 3/3 相等（`tools\s5-lang-check.py`） |
| yml 展平覆盖率 | en_us：可解析 1130 简单键 + 11 block scalar，json 缺失 0；zh_cn 不一致 0；en_us 唯一的 1 处"不一致"是 `\"%1$s\"` 的转义写法差异（yml `\\"%1$s\\"` vs json `\"%1$s\"`），值语义相同 |
| 结论 | **翻译链路（S2 自造的 convertTranslations 管线）打通**，34 条命令响应里 BARE-KEY = 0 |

## 5. 记录器落盘路径（验收第 5 条）

```
> Get-ChildItem -Recurse run\config\carpettisaddition -File | Select -Expand FullName
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\lifetime\recorder_config.json
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn-1.21.1+build.3-v2.tiny
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn_version.json
```

- 目录名实测为 **`carpettisaddition`**（不是 `carpet_tis_addition`），与 S2 的 `COMPACT_NAME` 结论一致（验收第 5 条通过）。
- `/lifetime tracking start` → `Lifetime Tracker started`；`/lifetime tracking` → `Tracked 0.00 min ... No result yet`；`/lifetime tracking stop` 正常；配置落盘即上面 `lifetime\recorder_config.json`。
- `/counter redstone` 无法验证记录器（该命令不存在，见 §2）；等价物 `/scounter` 走的是内存计数 + `/log scounter`，不落盘。

## 6. `run\logs\latest.log` ERROR 清单（验收第 6 条）

- `[*/ERROR]` 行 **9 条**，另有 **1 条 `[FATAL]`**，合计 10 条，全部有明确归因，**0 条 compatibility level 之外的意外项**：

| 行 | 组件 | 摘要 | 归因 |
|---|---|---|---|
| L39 / L40 / L54 / L86 / L87 | conditional_mixin `ConditionalMixinMod` | `Failed to instantiate a ConditionTester from class carpettisaddition.utils.mixin.testers.Lithium*Tester: java.lang.reflect.InvocationTargetException` | **B-S5-2**（`LithiumConfigAccess` 静态初始化炸） |
| L901 / L1029 / L1520 | `carpettisaddition.CarpetTISAdditionMod` | `createFake delayed task error` | **B-S5-1**（假玩家创建时 `GameProtocols` clinit 失败） |
| L1411 | `net.minecraft.util.thread.BlockableEventLoop/FATAL` | `Error executing task on Server`（`/carpet hopperXpCounters true` 触发） | **B-S5-2** |

- `compatibility level` 相关：3 行命中，其中 **2 条 WARN**（L17 `carpet-tis-addition.mixins.json` `JAVA_21`、L19 `carpet.mixins.json` `JAVA_17`，均高于 Mixin 自带 max `JAVA_13`）+ 1 条 INFO（L18 `Compatibility level set to JAVA_21`）——与任务书"允许 3 条"一致。

## 7. `@Restriction` 跳过名单（验收第 7 条）

`build\restriction-skipped.txt` 已生成，**71 条**（`Select-String run\logs\latest.log -Pattern '\[TISCM\] Disabled mixin'`），分类与判读见 `MIXIN-LOG.md` §11：`required mod ... not found` 17 条、`required mixin class ... disabled` 1 条（级联）、`does not matches version predicates` 53 条。**71 条全部属"合规跳过"，没有一条是条件写错。**

## 8. 自检三条 + `net.fabricmc` 残留（验收第 8 条）

```
java files = 1009
require\s*=\s*0 : 4
@Overwrite : 2
TODO|FIXME|UnsupportedOperationException (-i) : 22
  └ -CaseSensitive : 21   （多出的 1 条是 SpeedTestPinger.java:146 "mapToDouble(Long::doubleValue)." 的大小写不敏感误报）
net.fabricmc lines : 8
(method|field)_\d+ hits : 0
```

与基线 **4 / 2 / 21**（口径：`-CaseSensitive`）完全一致，`net.fabricmc` 仍 8 行（4 处 fabric-api compat + `TinyMappingV2Reader.java:24-27`）。S5 未新增任何一条。

## 9. 八条验收对照

| # | 验收项 | 状态 | 证据 |
|---|---|---|---|
| 1 | `tools\s5-rcon.ps1` 可用，`/list` 正常回包 | ✅ | §1 原始响应 |
| 2 | 34 条命令全部有记录，BARE-KEY/MISSING 条数明确 | ✅ | §2：BARE-KEY=0、MISSING=10、ERROR=1、INCOMPLETE-OK=4、SPEC-MISMATCH=4、OK=15 |
| 3 | `/help` 条目数与运行目录无关、无裸 key | ✅ | §3 P0：109 条命令条目、裸 key 0；`/tis` 帮助条目为 0（该根不存在） |
| 4 | `en_us.json` 存在且 key ≥ 1100 | ✅ | §4：1142 key |
| 5 | 落盘路径实测 `carpettisaddition` | ✅ | §5 Get-ChildItem 输出 |
| 6 | `latest.log` ERROR 条数与原因清单 | ✅ | §6：9 ERROR + 1 FATAL，全部归因 |
| 7 | `restriction-skipped.txt` 已生成且判读入档 | ✅ | §7 + `MIXIN-LOG.md` §11 |
| 8 | 自检 4/2/21、`net.fabricmc`=8、三份状态文件回写 | ✅ | §8 + `PORT-STATE.md` §13 / `BLOCKERS.md` / `MIXIN-LOG.md` |

## 10. 阻塞项（详见 `BLOCKERS.md`）

| 编号 | 一句话 | 影响 |
|---|---|---|
| B-S5-1 | `network.CustomPayloadC2SPacketMixin` 的 `@ModifyArg`（`<clinit>` → `INVOKE CustomPacketPayload.codec(...)`）在 1.21.1 Mojmap 下 0 候选；`require=1` 下按设计抛 `InjectionError`（`_run_s5.log:1016`），`GameProtocols` clinit 随之 `ExceptionInInitializerError` | TISCM 自定义 payload 注册表为空 → `/player <name> spawn` 静默失败（假玩家链路全断） |
| B-S5-2 | `LithiumConfigAccess` 的静态字段初始化在 mixin 阶段调用 `ModList.get()`（Fabric→NeoForge 转换遗留）→ `NoClassDefFoundError` | 5 条 `Lithium*Tester` 无法实例化；`hopperXpCounters` 规则一开就 FATAL |
| B-S5-3 | 任务书 10 条命令名与 1.82 实际命令树不符（`/tis`×5、`/counter`×3、`/man`、`/stat`） | 验收清单需按 1.82 实际命令名重写，不是代码缺陷 |

### 10.1 为什么 S4 的 `_run_s4_final.log` 里看不到 B-S5-1

S4 的验收跑法是"启动到 `Done` 就退出"，整场没有任何玩家/payload 交互，`GameProtocols`（`CustomPacketPayload.codec` 的宿主类）从未被加载 → 该 mixin 的注入器从未执行，所以 S4 的 `Critical injection failure` 计数是 0。S5 一旦发 `/player Steve spawn`，假玩家创建触发 `GameProtocols.<clinit>`，注入器第一次执行就命中 0 个目标并抛 `InjectionError`。

**结论：S4 的 0 是"未触发"，不是"已通过"。** B-S5-1 是 S4 遗留的隐性缺陷，在 S5 的功能验收里被真实用例挖出来——这也是本阶段最有价值的产出之一。

## 11. 边界与遗留

- 未改动：1009 个 .java（业务逻辑）、`lang\*.yml`、`carpet-tis-addition.mixins.json`、`neoforge.mods.toml`、`libs\*`、`run\mods\carpet.jar`。
- 改过：`run\server.properties`（验收期开 RCON，结束时已 `enable-rcon=false` 并留注释）、`tools\s5-*.ps1|py`（新建）、三份状态文件、本文档。
- `CarpetTISAdditionMixinPlugin` 的日志级别保持 S4 的 `LOGGER.info`（抓跳过名单用），未回调 debug。
- 未出包（`jarJar` 内嵌 conditional-mixin 属后续阶段）。

## 12. 附录：34 条原始响应全文

```
=== CMD 1 : /carpet ===
Current Carpet Mod settings:
Carpet Mod version: 1.4.147+v1.0.8-251027
Carpet TIS Addition version: 1.82.0
Browse categories:
[Porting] [survival] [logger] [experimental] [creative] [command] [dispenser] [TISCM Network Protocol] [CARPET MOD] [feature] [optimization] [scarpet] [client] [TNT] [bugfix] [TIS]
=== END 1 ===
=== CMD 2 : /carpet tisAdditionEnabled ===
Unknown rule: tisAdditionEnabled
=== END 2 ===
=== CMD 3 : /carpet tisAdditionEnabled false ===
Unknown rule: tisAdditionEnabled
=== END 3 ===
=== CMD 4 : /carpet tisAdditionEnabled ===
Unknown rule: tisAdditionEnabled
=== END 4 ===
=== CMD 5 : /carpet tisAdditionEnabled true ===
Unknown rule: tisAdditionEnabled
=== END 5 ===
=== CMD 6 : /tis ===
Unknown or incomplete command, see below for error
tis<--[HERE]
=== END 6 ===
=== CMD 7 : /tis help ===
Unknown or incomplete command, see below for error
tis help<--[HERE]
=== END 7 ===
=== CMD 8 : /tis info ===
Unknown or incomplete command, see below for error
tis info<--[HERE]
=== END 8 ===
=== CMD 9 : /tis raid ===
Unknown or incomplete command, see below for error
tis raid<--[HERE]
=== END 9 ===
=== CMD 10 : /tis speedtest ===
Unknown or incomplete command, see below for error
tis speedtest<--[HERE]
=== END 10 ===
=== CMD 11 : /tick ===
Unknown or incomplete command, see below for error
tick<--[HERE]
=== END 11 ===
=== CMD 12 : /tick query ===
The game is running normally
Target tick rate: 40.0 per second.
Average time per tick: 0.2ms (Target: 25.0ms)
Percentiles: P50: 0.2ms P95: 0.3ms P99: 0.5ms, sample: 100
=== END 12 ===
=== CMD 13 : /tick freeze ===
The game is frozen
=== END 13 ===
=== CMD 14 : /tick step 5 ===
Stepping 5 tick(s)
=== END 14 ===
=== CMD 15 : /tick rate 40 ===
Set the target tick rate to 40.0 per second
=== END 15 ===
=== CMD 16 : /tick unfreeze ===
The game is running normally
=== END 16 ===
=== CMD 17 : /player Steve spawn ===

=== END 17 ===
=== CMD 18 : /player Steve kill ===
Can only manipulate existing players
=== END 18 ===
=== CMD 19 : /counter ===
Unknown or incomplete command, see below for error
counter<--[HERE]
=== END 19 ===
=== CMD 20 : /counter redstone ===
Unknown or incomplete command, see below for error
counter redstone<--[HERE]
=== END 20 ===
=== CMD 21 : /counter dye ===
Unknown or incomplete command, see below for error
counter dye<--[HERE]
=== END 21 ===
=== CMD 22 : /log ===
players only
=== END 22 ===
=== CMD 23 : /log mobcaps ===
No player specified
=== END 23 ===
=== CMD 24 : /log microTiming ===
No player specified
=== END 24 ===
=== CMD 25 : /log phantom ===
No player specified
=== END 25 ===
=== CMD 26 : /lifetime ===
Lifetime Tracker
A tracker to track lifetime and spawn / removal reasons from all newly spawned and removed entities
Complete doc here
=== END 26 ===
=== CMD 27 : /lifetime tracking ===
Lifetime Tracker has not started
=== END 27 ===
=== CMD 28 : /info entity ===
Unknown or incomplete command, see below for error
...nfo entity<--[HERE]
=== END 28 ===
=== CMD 29 : /info block ===
Unknown or incomplete command, see below for error
info block<--[HERE]
=== END 29 ===
=== CMD 30 : /info world ===
Unknown or incomplete command, see below for error
info world<--[HERE]
=== END 30 ===
=== CMD 31 : /raid list ===
No raid exists
=== END 31 ===
=== CMD 32 : /man ===
Unknown or incomplete command, see below for error
man<--[HERE]
=== END 32 ===
=== CMD 33 : /spawn ===
Unknown or incomplete command, see below for error
spawn<--[HERE]
=== END 33 ===
=== CMD 34 : /stat ===
Unknown or incomplete command, see below for error
stat<--[HERE]
=== END 34 ===
```
