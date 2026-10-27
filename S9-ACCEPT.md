# S9 验收报告 — 拆掉「拍平 conditional-mixin」，消除模块包冲突并重出 dist

- 轮次：S9（单目标：B-S9-1）
- 工作目录：`D:\Minecraft-mod-development\TIS-NeoForge`
- 结论：**T1 / T2 / T3 / T4 / T5 全部达成**，附 3 处「按实测修正的 spec 偏差」、1 处「spec 增项（必需）」、1 处「新发现异常 B-S9-2（已记录，非阻塞）」
- 所有结论均附命令输出；本文件里出现的数字都能用文中命令重跑复现

---

## 0. 结论摘要

| 目标 | 状态 | 一句话证据 |
|---|---|---|
| T1 拆包 | ✅ | 主 jar 顶层段只剩 4 个（`META-INF` / `assets` / `carpettisaddition` / `carpet-tis-addition.mixins.json`），`assert-absent me net` → `RESULT: PASS`、exit=0 |
| T2 补依赖 | ✅ | 主 jar 的 `neoforge.mods.toml` 末尾新增 `[[dependencies.carpet_tis_addition]] modId="conditional_mixin" type="required" versionRange="[0.6.4,)"`；与模块 jar 的 `modId` 同名 |
| T3 三环境 | ✅ | A 失败（`requires conditional_mixin`）／B `Done (7.536s)` 且 ERROR 0、apply-failed 0、applied 437／C 三组（C1、C1b 复现冲突；C2、C3 无 `ResolutionException`） |
| T4 重出 dist | ✅ | `dist\TIS-Addition-NeoForge-1.21.1.zip` = **2933347 bytes** / `37D87A0B…61E1`，6 条目，四组件哈希已重算（见 §5） |
| T5 记录 | ✅ | `PORT-STATE.md` §16、`BLOCKERS.md` §19（B-S9-1 已修 + B-S9-2 记录）、`RELEASE-NOTES.md` 兼容性表新增 21.1.252 行 |

### 0.1 spec 与实际的三处偏差（按实测取值，均已在本文件留证）

| 项 | spec 写的是 | 实测是 | 依据 |
|---|---|---|---|
| 模块 jar 的 license | MIT | **LGPL-3.0-only** | 官方 neo 产物 `META-INF/neoforge.mods.toml` 第 3 行 `license = "LGPL-3.0-only"`，§2 有全文 |
| 模块 jar 的类数 | `me/fallenbreath/**` 27 类 | **18 个 .class** | 27 是 **Fabric 产物**的数字（含 `api/util/fabric/VersionCheckerImpl`、`fabric/ConditionalMixinFabric`）；neo 产物用 `api/util/neoforge/VersionCheckerImpl` + `neoforge/ConditionalMixinNeoForge` 替换，故 18 |
| `net/fabricmc/loader/**` shim | 要打进模块 jar | **不打** | 官方 neo 产物不含 `net/**`（`net/ entries = 0`）。fabric-loader shim 是 S2 为「Fabric 版 conditional-mixin」自造的兜底，它本身就是把 `net.fabricmc.loader.*` 拍平进主 jar 的第二个冲突源；换成官方 neo 产物后不需要它，`compat-src/fabric-loader-shim/` 只作历史取证保留 |

### 0.2 spec 的一处增项（必需，不增就会坏 T3-B 的 ERROR 0）

`dist` 必须多发一个 `mods\tiny-mappings-parser-0.3.0+build.17.jar`。理由（§4.4 有反证）：
`StackTraceDeobfuscator` 在 `EnvironmentUtils.isMinecraftObfuscated()==true` 时会起 `TISCM Mapping` 线程读 yarn 映射，
而 `TinyMappingV2Reader` 依赖 `net.fabricmc.mapping.reader.v2.*`。
S9 把 `net/**` 从主 jar 里拿掉后，**这个 jar 若不发出去，专服仍能启动、`latest.log` 仍 0 ERROR**，但 stdout 会抛
`NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`（实测原文 §4.4）。所以它不是可选件。

### 0.3 新发现异常 B-S9-2（已记录，非阻塞，不影响 mixin 行为）

`[TISCM] Disabled mixin` 行数从 S8 的 **72** 降到 S9 的 **21**，少掉的 51 条全部是
`due to mod minecraft@1.21.1 does not matches version predicates [...]` 这一类。
**mixin 应用集合没有因此变多**（§4.2 差集证明这 51 条在 S9 里也照样没被应用），
所以这是「诊断信息缺失」，不是「限制失效」。成因定位到 conditional-mixin 官方 neo 产物与 Fabric 产物的差异，
需要下一轮源码级排查，已记入 `BLOCKERS.md` B-S9-2。**本报告不把它写成"已修"。**

---

## 1. T1 拆包：`stripConditionalMixin` → `packageConditionalMixin`

### 1.1 `build.gradle` 改动（`build.gradle:90-153` 整段重写）

删掉的任务：`genConditionalMixinShimMeta`、`compileFabricLoaderShim`、`stripConditionalMixin`、`strippedConditionalMixinJar`
（`Select-String -Path build.gradle -Pattern 'stripConditionalMixin|genConditionalMixinShimMeta|compileFabricLoaderShim'` → 0 命中，仅注释里提到）。

新增（普通任务，**不是 `Copy`**）：

```groovy
def vendoredConditionalMixinNeo = file('libs/conditional-mixin-neoforge-0.6.4.jar')
def packageConditionalMixin = tasks.register('packageConditionalMixin') {
    inputs.file(vendoredConditionalMixinNeo)
    outputs.file(target)                       // build/libs/conditional-mixin-neoforge-0.6.4.jar
    doLast { java.nio.file.Files.copy(...REPLACE_EXISTING) ; println "[TIS-S9] conditional-mixin module jar: ..." }
}
```

> **踩坑记录（可复现）**：初版写成 `tasks.register('packageConditionalMixin', Copy) { into build/libs }`，Gradle 9.5.1 直接拒绝：
> `Gradle detected a problem with the following location: 'D:\Minecraft-mod-development\TIS-NeoForge\build\libs'`
> `Task ':compileJava' uses this output of task ':packageConditionalMixin' without declaring an explicit or implicit dependency`
> —— 因为 `build/libs` 同时是 `compileJava` 的输入目录。改成普通任务 + 显式 `inputs`/`outputs` + `Files.copy` 后通过。

`releaseJar` 现在只剩 `from sourceSets.main.output` + `exclude 'me/**'` + `exclude 'net/**'`，
原来的 4 个 `from`（shim 目录、`conditional-mixin-lib.zipTree`、`tiny-mappings zipTree`）与 3 个 `dependsOn` 全部删除。

### 1.2 构建

```
> .\gradlew.bat --offline --console=plain stripTinyMappings packageConditionalMixin releaseJar
> Task :releaseJar
[TIS-S9] releaseJar = carpet-tis-addition-1.82.0+neoforge.1.21.1.jar
[TIS-S9]   不含 me/** 与 net/**；conditional-mixin 走独立模块 conditional-mixin-neoforge-0.6.4.jar
[TIS-S9]   tiny-mappings 走独立模块 tiny-mappings-lib.jar
[TIS-S9] produced D:\Minecraft-mod-development\TIS-NeoForge\build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  (1689654 bytes)
BUILD SUCCESSFUL in 22s
```

`build\libs` 现状：

| 文件 | 字节 |
|---|---:|
| `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` | 1 689 654（旧 1 804 524，−114 870） |
| `conditional-mixin-neoforge-0.6.4.jar` | 28 507 |
| `tiny-mappings-lib.jar` | 83 140 |

### 1.3 验收断言（逐条打印，不用管道猜）

```
> java tools\ZipArchive.java tops build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar
== top-level segments of build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar ==
  META-INF
  assets
  carpet-tis-addition.mixins.json
  carpettisaddition
-- entries = 1645, distinct top segments = 4: [META-INF, assets, carpet-tis-addition.mixins.json, carpettisaddition]

> java tools\ZipArchive.java assert-absent build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar me net
== assert-absent on build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar ==
   forbidden top-level segments: [me, net]
   offending segments found   : (none)
   RESULT: PASS - none of the forbidden top-level segments exist
exit=0
```

`tools\ZipArchive.java` 是本轮新增的单文件工具（JDK 21 直接 `java tools\ZipArchive.java` 运行，不产生 class 文件），
四种模式：`list`（逐条打印 序号/路径/压缩后/原始/crc）、`tops`（顶层段集合）、`assert-absent`（命中即 exit 1）、`dump`（打印单条目文本）。
它不做任何过滤/截断，`me/`、`net/` 是否存在的证据就是上面 `offending segments found` 那一行。

### 1.4 模块 jar 内容（决定 spec 偏差 2、3 的原始输出）

```
> java tools\ZipArchive.java tops build\libs\conditional-mixin-neoforge-0.6.4.jar
== top-level segments of build\libs\conditional-mixin-neoforge-0.6.4.jar ==
  LICENSE_conditional-mixin
  META-INF
  architectury_inject_build_common_bf6521456969452ea3deb82c7024fa40_cd5ee14119dea8e201755fe2677ef52aeffce4062066ad5d02e50975163b2726conditionalmixincommon064devjar
  assets
  me
  pack.mcmeta
-- entries = 39, distinct top segments = 6: [...]

> (ZipArchive list 后统计) me/**.class entries = 18 ; net/ entries = 0
```

18 个类（逐个打印过）：
`ConditionalMixinMod`、`api/annotation/{Condition,Condition$Type,Restriction}`、
`api/checker/{RestrictionChecker,RestrictionCheckers}`、
`api/mixin/{AnnotationCleaner,ConditionTester,RestrictionCheckFailureCallback,RestrictiveMixinConfigPlugin}`、
`api/util/{VersionChecker}`、`api/util/neoforge/VersionCheckerImpl`、
`impl/{AnnotationCleanerImpl,MemorizedRestrictionChecker,SimpleRestrictionChecker,SimpleRestrictionChecker$1,SimpleRestrictionChecker$Result}`、
`neoforge/ConditionalMixinNeoForge`。

---

## 2. T2 依赖声明

### 2.1 主 jar 的 `META-INF/neoforge.mods.toml`（dump 尾部原文）

```
ordering="NONE"
side="BOTH"

# S9: conditional-mixin 现在是独立模块（modId 全小写下划线 = fabric 侧 "conditional-mixin"）。
# 主 jar 不再拍平 me/fallenbreath/**，故必须显式声明依赖，缺失时 FML 会在 mod 解析阶段报
# "requires conditional_mixin" 并拒绝启动（这是预期行为，见 S9-ACCEPT.md T3-A）。
[[dependencies.carpet_tis_addition]]
modId="conditional_mixin"
type="required"
versionRange="[0.6.4,)"
ordering="NONE"
side="BOTH"
```

### 2.2 modId 一致性（spec 要求「modId 与 fabric 侧 modId 一致」——实测是**归一化后一致**）

| 侧 | 声明 | 原文 |
|---|---|---|
| Fabric 产物（`libs\conditional-mixin.jar`，S8 及以前打平进主 jar 的那份） | 主 id `conditional-mixin`，**provides** `conditional_mixin` | `"id": "conditional-mixin"` / `"provides": [ "conditional_mixin" ]` / `"version": "0.6.4"` / `"license": "LGPL-3.0-only"` |
| 官方 neo 产物（本轮 vendored 进 `libs\`，原样 28 507 字节） | 主 id 直接是 `conditional_mixin` | `modId = "conditional_mixin"` / `version = "0.6.4"` |

主 jar 声明的是 `conditional_mixin`（下划线），与 neo 产物**字面完全相同**；与 fabric 侧的 *provides* 名相同，
与 fabric 侧的*主 id* 不同（短横线在 NeoForge loader 4 非法，必须归一化）。这也是为什么整合包里
TweakerMore 的 jar-in-jar 能让两者互相满足依赖。

### 2.3 模块 jar 的 toml 全文（顺带证明 license 偏差）

```
modLoader = "javafml"
loaderVersion = "[1,)"
license = "LGPL-3.0-only"

[[mods]]
modId = "conditional_mixin"
version = "0.6.4"
displayName = "conditional mixin"
authors = "Fallen_Breath"
description = "Use annotation to conditionally apply your mixins"
logoFile = "assets/conditionalmixin/icon.png"

[[dependencies.conditional_mixin]]
modId = "neoforge"
type = "required"
versionRange = "[20.5,)"
ordering = "NONE"
side = "BOTH"
```

---

## 3. T3 三环境验收（运行台：`_s8_srv`，NeoForge **21.1.264** 专服 + Java 21，日志全部 `Get-Content -Encoding UTF8` 读）

驱动脚本：`tools\s9-run-env.ps1 -Env <名> -Mods '<令牌表>' -Expect done|fail`，每个环境清空重建 `_s8_srv\mods` → 删 `latest.log` → 启服 → 见 `Done (` 后 2s 发 `stop` → 归档到 `_s9_logs\<Env>.latest.log`。
令牌：`@carpet`=carpet-1.4.147、`@tis`=本轮新主 jar、`@tisold`=S8 放行版旧 jar(1 804 524，拍平版)、`@cm`=本轮模块 jar、`@cmext`=从实例 `tweakermoreforge` 内嵌 jar 解出的同一份(字节相同)、`@tiny`=tiny-mappings-lib。

### 3.0 总表（同一脚本对 7 份归档日志的机械统计）

```
ENV      verdict   ResEx  Done   ERROR
S9A      fail      0      0      3
S9B      Done      0      1      0
S9B2     Done      0      1      0
S9C1     fail      1      0      1
S9C1b    fail      1      0      1
S9C2     Done      0      1      0
S9C3     Done      0      1      0
```

### 3.1 A：只有 `carpet` + 本 mod（**预期失败**，用户要求的原文）

`-Env S9A -Mods '@carpet,@tis' -Expect fail` → `SERVER DID NOT REACH Done`，`_s9_logs\S9A.latest.log`（7 390 B）：

```
21: [main/ERROR] [net.neoforged.fml.loading.ModSorter/LOADING]: Missing or unsupported mandatory dependencies:
22: 	Mod ID: 'conditional_mixin', Requested by: 'carpet_tis_addition', Expected range: '[0.6.4,)', Actual version: '[MISSING]'
25: [main/FATAL] [net.neoforged.fml.ModLoader/CORE]: Error during pre-loading phase: Mod carpet_tis_addition requires conditional_mixin 0.6.4 or above
26: Currently, conditional_mixin is not installed
30: [main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
32: - Mod carpet_tis_addition requires conditional_mixin 0.6.4 or above
33: Currently, conditional_mixin is not installed
```

第 17 行的 Mod List 只有 `Carpet TIS Addition` / `Carpet` / `Minecraft` / `NeoForge` 四项，
即 `conditional_mixin` 是**唯一**缺失项 → 依赖声明精确、没有误伤其它依赖。**这就是 T3-A 要求的预期行为。**

### 3.2 B：加 `conditional-mixin` + `tiny-mappings`（**预期 Done**）

`-Env S9B -Mods '@carpet,@tis,@cm,@tiny' -Expect done`：

```
75: [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (7.536s)! For help, type "help"
```

`tools\s8-count.ps1` 双口径统计（`latest.log` 与 `debug.log` 同时喂）：

```
FILE _s9_logs\S9B.latest.log  (15779 bytes)
  1) TIS applied (only 'from carpet-tis-addition.mixins.json into') : 0
  2) Disabled mixin ('[TISCM] Disabled mixin')                      : 21
  3) ERROR/FATAL/apply-failed                                       : 0 / 0 / 0
FILE _s9_logs\S9B.debug.log  (773027 bytes)
  1) TIS applied : 437
  2) Disabled mixin : 21
  3) ERROR/FATAL/apply-failed : 0 / 0 / 0
     all mixin configs seen in this log : carpet-tis-addition.mixins.json=437 carpet.mixins.json=162 neoforge.mixins.json=2
```

- `latest.log` 里 applied=0 是**日志级别**造成的（专服把 `Mixing` 打在 DEBUG，只进 `debug.log`），不是没应用 —— 这个口径陷阱 S8 已记录，见 `RELEASE-NOTES.md` §6.2。
- stdout 另查 `Exception in thread|NoClassDefFound|ClassNotFound` → **0**（这条必须查，§4.4 证明了有些故障只出现在 stdout）。
- `[TISCM Mapping/INFO] ... Yarn mapping file yarn-1.21.1+build.3-v2.tiny loaded` 出现 → `tiny-mappings-parser` 独立模块生效。

### 3.3 C：模拟整合包（用户要求「不再出现任何 ResolutionException」）

**C1（先复现旧故障）** `-Env S9C1 -Mods '@carpet,@tisold,@cm,@tiny'` → exit=1，`S9C1.latest.log`：

```
25: [main/ERROR] [cpw.mods.modlauncher.ModuleLayerHandler/]: Error while resolving modules.
26: java.lang.module.ResolutionException: Modules carpet_tis_addition and tiny_mappings_parser_lib export package net.fabricmc.mappings.model to module carpet
```

**C1b（复现用户实例那条 `me.fallenbreath` 冲突）** `-Env S9C1b -Mods '@carpet,@tisold,@cm'` → exit=1，`S9C1b.latest.log`：

```
23: [main/ERROR] [cpw.mods.modlauncher.ModuleLayerHandler/]: Error while resolving modules.
24: java.lang.module.ResolutionException: Module conditional_mixin contains package me.fallenbreath.conditionalmixin.api.annotation, module carpet_tis_addition exports package me.fallenbreath.conditionalmixin.api.annotation to conditional_mixin
```

> 用户实例里那条（`D:\PCl2\...\logs\latest.log:454`）是同一机制的另一种表现：
> `Modules carpet_tis_addition and conditional_mixin export package me.fallenbreath.conditionalmixin.api.mixin to module jecharacters`
> —— 报哪个包（`api.mixin` 还是 `api.annotation`）、conflict 报给谁（`jecharacters` 还是对方模块），取决于**哪一侧先被加载**，同源问题。

**C2（本轮新包 + 两份相同的 conditional_mixin）** `-Env S9C2 -Mods '@carpet,@tis,@cm,@tiny,@cmext'` → **Done**：

```
22: conditional mixin 0.6.4 (conditional_mixin)      <-- Mod List 里只有这一行
75: [Server thread/INFO] [...] Done (8.094s)! For help, type "help"
```
`ResolutionException` 计数 = **0**。两份同 modId 同版本的文件被 FML 静默去重。

**C3（版本不同的重复，自造 0.6.5 fixture：把官方 0.6.4 的 toml `version` 改成 `0.6.5` 重打包）**
`-Env S9C3 -Mods '@carpet,@tis,@cm,@tiny,<绝对路径>\conditional-mixin-neoforge-0.6.5.fixture.jar'` → **Done**：

```
22: conditional mixin 0.6.5 (conditional_mixin)
56: conditional_mixin (version 0.6.4 -> 0.6.5)
79: [Server thread/INFO] [...] Done (9.028s)! For help, type "help"
```
`ResolutionException` 计数 = **0**，FML 取高版本。

> **实测结论 vs 用户 spec 措辞**：spec 要求 README 写「两个都放会 Duplicate mods」。实测在 NeoForge **21.1.264** 上
> 不会崩：同版本静默去重（C2）、异版本取高版本（C3）。README 按实测写，同时保留「老版本 FML 会报 Duplicate mods、别依赖这个行为」的告警，
> 并把「已有则不放 / 没有才放」作为主规则 —— 用户真正要传达的运维结论（不要多放）没有变。

### 3.4 B2 反证：故意不给 `tiny-mappings-parser`（证明它必需）

`-Env S9B2 -Mods '@carpet,@tis,@cm'` → 仍是 **Done (7.211s)**、`latest.log` ERROR 0/0/0 —— 但 stdout：

```
 94: Exception in thread "TISCM Mapping" java.lang.NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor
 99: Caused by: java.lang.ClassNotFoundException: net.fabricmc.mapping.reader.v2.TinyVisitor
```

这就是 §0.2 那条 spec 增项的证据：**只看 `latest.log` 会漏掉它**。

### 3.5 与 S8 的「同量级」论证（同机、同 NeoForge、同脚本，用仍在磁盘上的 S8 日志）

用户要求的基线是"S8 的 464"（那是**客户端/长会话**口径）。本轮取同一台 `_s8_srv` 上 S8 的**短跑专服**日志 `build\s8\srv-run2-debug.log`（443 行 / 438 唯一类）做同口径比较：

| | S8 | S9 |
|---|---:|---:|
| `Mixing ... from carpet-tis-addition.mixins.json into` 行数 | 443 | 437 |
| 唯一 mixin 类 | 438 | 432 |
| only-in-S8（唯一类集合差） | — | **8** |
| only-in-S9（唯一类集合差） | — | **2** |

only-in-S8 的 8 条全部是「需要玩家/指令才会加载目标类」的项：
`command.raid.RaidAccessor`、`command.raid.RaidMixin`、`command.raid.RaidWithIdAndWorldMixin`、`command.lifetime.spawning.raid.RaidMixin`、
`command.lifetime.spawning.breeding.VillagerBreedTaskMixin`、`logger.raid.RaidMixin`、
`logger.microtiming.events.blockupdate.ChainRestrictedNeighborUpdaterMixins$StateReplacementEntryMixin`、`rule.chatMessageLengthLimitUnlocked.StringHelperMixin`。
only-in-S9 的 2 条：`command.lifetime.deathdamage.VehicleEntityMixin`、`rule.optimizedHardHitBoxEntityCollision.EntityTrackingSectionMixin`。
→ 差集与「无玩家 headless 短跑」的加载顺序一致，与本轮改动无关；**同口径 437 vs 443 = 同一量级**。

（S8 那份 461 行的长跑日志已被 log4j 轮转覆盖：`_s8_srv\logs\debug-1..5.log.gz` 现在是我这几轮的 437 行日志，因此本报告不引它作原始证据，只引 `RELEASE-NOTES.md` §6.1 与 `S8-ACCEPT.md` §1.2 的历史记录。）

### 3.6 附：`Disabled mixin` 21 vs 72（= B-S9-2，如实记录）

对 `build\s8\srv-run2-debug.log` 与 `_s9_logs\S9B.debug.log` 提取 `[TISCM] Disabled mixin <mixin> due to <reason>` 按 reason 分组：

```
S8 72 条：mod minecraft@1.21.1 does not matches version predicates[...] = 53
          required mod lithium not found = 6 / malilib = 4 / fabric-transfer-api-v1 = 3 / mctester = 2 / minihud = 1 / tic_tacs = 1
          required mixin class ... disabled = 1 / tester result = false = 1
S9 21 条：required mod lithium not found = 8 / malilib = 4 / fabric-transfer-api-v1 = 3 / mctester = 2 / minihud = 1 / tic_tacs = 1
          required mixin class ... disabled = 1 / tester result = false = 1
          （minecraft 版本谓词类 = 0）
```

也就是说：**mod 存在性判定照旧在工作（21 条与 S8 的那 19 条一一对应），只有「minecraft 版本谓词」这一类不再产生诊断行。**

这些 mixin 是否仍在 S9 里被跳过？以 `carpet.tweaks.command.tickWarpMaximumDuration.TickCommandMixin` 为例（目标类 `TickCommand` 在启动期必然加载）：

```
S8: 17:49:42.344 [INFO] [CarpetTISAdditionMixinPlugin]: [TISCM] Disabled mixin carpettisaddition.mixins.carpet.tweaks.command.tickWarpMaximumDuration.TickCommandMixin ...
    17:49:49.619 [TRACE] [mixin/]: Added class metadata for net/minecraft/server/commands/TickCommand to metadata cache
S9:  19:39:26.489 [TRACE] [mixin/]: Added class metadata for net/minecraft/server/commands/TickCommand ...
    （没有任何 Mixing ... tickWarpMaximumDuration ... 行，也没有任何 Disabled 行）
```

其它 6 条同目标类别的 mixin（`logger.tickwarp.TickSpeedMixin`、`rule.tickCommandEnhance.TickCommandMixin` 等）在 S9 里照常 `Mixing ... into net.minecraft.server.commands.TickCommand`。
→ **该 mixin 在 S9 里既没被应用、也没被报告**；结合 §3.5 的 applied 集合差集（这 51 条一条都没进 applied 集合），可判定**行为等价、诊断丢失**。

成因初判（下一轮验证）：`RestrictiveMixinConfigPlugin.shouldApplyMixin` → `RestrictionCheckers.memorized().checkRestriction(className)`
→ `SimpleRestrictionChecker.getRestrictionAnnotation(...) throws ClassNotFoundException` / `checkConditions`
→ 版本谓词分支走 `VersionChecker.doesModVersionSatisfyPredicate(...)`；
S8 用的是 Fabric 产物（`api/util/fabric/VersionCheckerImpl` + 我们自造的 fabric-loader shim），S9 换成 neo 产物（`api/util/neoforge/VersionCheckerImpl`）。
差异出现在这条链的异常/回调处理上（回调没被调用 ⇒ 没有 `Disabled` 行），**不在 mixin 应用结果上**。列入 B-S9-2，状态「记录，待查」，不作为本轮的「已修」项。

---

## 4. 断言复跑命令（一条命令得到 §3.0 那张表）

```powershell
foreach ($e in 'S9A','S9B','S9B2','S9C1','S9C1b','S9C2','S9C3') {
  $t=[System.IO.File]::ReadAllText((Resolve-Path "_s9_logs\$e.latest.log"))
  "{0}: ResolutionException={1}  Done={2}  ERROR={3}" -f $e,
     ([regex]::Matches($t,'ResolutionException')).Count,
     ([regex]::Matches($t,'Done \(')).Count,
     ([regex]::Matches($t,'\bERROR\b')).Count
}
```

---

## 5. T4 重出 dist

### 5.1 结构（`tools\make-dist-zip.ps1`，显式条目名，不用 `CreateFromDirectory` 的路径推断）

```
> powershell -NoProfile -ExecutionPolicy Bypass -File tools\make-dist-zip.ps1
[TIS-S9] + README.txt  (11595 bytes)
[TIS-S9] + THIRD-PARTY-NOTICES.txt  (3921 bytes)
[TIS-S9] + mods/carpet-1.4.147+v1.0.8-251027.jar  (1498683 bytes)
[TIS-S9] + mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  (1689654 bytes)
[TIS-S9] + mods/tiny-mappings-parser-0.3.0+build.17.jar  (83140 bytes)
[TIS-S9] + optional/conditional-mixin-neoforge-0.6.4.jar  (28507 bytes)
[TIS-S9] dist zip: ...\dist\TIS-Addition-NeoForge-1.21.1.zip  (2933347 bytes)
[TIS-S9] sha256  : 37D87A0B00DF62CA70F7039E06F427A21966CEF3B1FC059AC94F86163617F1E1
exit=0
```

脚本会先检查 `dist\` 里**没有未登记文件**（防止把旧 zip 打进新 zip —— 上一版就是这么坏掉的，见 §5.4），再逐条写入。

### 5.2 zip 内条目（`java tools\ZipArchive.java list`）

```
   1  README.txt                                      comp=   5094  size=   11595  crc=679062A8
   2  THIRD-PARTY-NOTICES.txt                         comp=   1454  size=    3921  crc=9EAE3F6F
   3  mods/carpet-1.4.147+v1.0.8-251027.jar           comp=1417449  size= 1498683  crc=8B98B5C1
   4  mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  comp=1416060  size= 1689654  crc=78E00E3D
   5  mods/tiny-mappings-parser-0.3.0+build.17.jar     comp=  70026  size=   83140  crc=B3070776
   6  optional/conditional-mixin-neoforge-0.6.4.jar    comp=  22366  size=   28507  crc=0E3F5323
-- total entries = 6, uncompressed bytes = 3315500
```

### 5.3 四个（实为五个）文件的字节数与 SHA-256

| 文件 | 字节 | SHA-256 |
|---|---:|---|
| `mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` | 1 689 654 | `941E9C8AA8AA6D8236EED34243AE948B258453E94A2D615401F72316A5150228` |
| `mods\carpet-1.4.147+v1.0.8-251027.jar` | 1 498 683 | `6A81DB1098A92D7239DAA992B8508DAEF7EB2528C4F7C9024AE3A81A34259F5C` |
| `mods\tiny-mappings-parser-0.3.0+build.17.jar` | 83 140 | `B00E557A16CD0EA44BFCC693C90A9DE6D1DB2F8961A0165A57E77121F6AFDD3F` |
| `optional\conditional-mixin-neoforge-0.6.4.jar` | 28 507 | `36A570A455A20F3657A3DCCA64FBF135A629BE8C8DD64D2505FFBE3D098F9750` |
| `README.txt` | 11 595 | `04187EC8A3D1F098FC9DFEF80E280784FEC09F2C7B6F6030591584C2A367EAC6` |
| `THIRD-PARTY-NOTICES.txt` | 3 921 | `76E8F6AFCFD54032902E66E193DD94FD5181BB5B78C0BCD528C627B97019C9FA` |
| **`TIS-Addition-NeoForge-1.21.1.zip`** | **2 933 347** | **`37D87A0B00DF62CA70F7039E06F427A21966CEF3B1FC059AC94F86163617F1E1`** |

> 用户 spec 说「四个文件」，实际包里是 **6 个条目**：多了 `THIRD-PARTY-NOTICES.txt`（spec 要求新增）
> 与 `mods\tiny-mappings-parser-*.jar`（§0.2 论证的必需增项）。

### 5.4 打包踩坑（留档，避免下次重犯）

1. `[System.IO.Compression.ZipFile]::CreateFromDirectory($dist)` 会生成带 `dist\` 前缀的条目（.NET 的 base-dir 推断行为），本报告这一版改用**显式条目名**。
2. 临时 zip 一定不能写在 `dist\` **里面**：会 `文件 ... 正由另一进程使用`，并产出一个只含 2 个 txt 的 6 808 B 坏包（`E25D91C6…B8179`）。
3. 同理，`CreateFromDirectory` 扫描目录时旧 zip 还在，会被当成成员打进去 —— `tools\make-dist-zip.ps1` 的「未登记文件即拒绝打包」就是防这个。

### 5.5 `README.txt` 与 `THIRD-PARTY-NOTICES.txt` 要点

`README.txt`（11 595 B，九节）新增/修改：

- 一节：**三个** `mods\` jar（本 mod + carpet + tiny-mappings-parser）+ `optional\` 的 conditional-mixin；
- **三节「conditional mixin 判断方法」**（spec 要求的核心）：① 已有则不放（并给出 Mod List 里 `conditional mixin 0.6.4 (conditional_mixin)` 这一行的样子，指出整合包里 TweakerMore 的 jar-in-jar 就是"已有"的常见来源）；② 没有才放（给出 `Missing or unsupported mandatory dependencies` + `requires conditional_mixin 0.6.4 or above` 原文，出现才把 `optional\` 那份复制进去）；③ 不要两边都放，并如实写 C2/C3 的实测结果（同版本去重、异版本取高版本），同时警告「老版本 FML 会报 Duplicate mods，别依赖此行为」；④ 附 B-S9-1 的 `ResolutionException` 原文作背景；
- 四节：少了 `tiny-mappings-parser` 的 `NoClassDefFoundError` 原文 + 「这条只在 stdout，不在 latest.log」提示；
- 七节：`Disabled mixin` 口径改成「专服 Done 时刻 21 条（S9 实测）／客户端与长会话更多（S8 记录 72~74）」，并注明随已加载类增长；
- 八节：四个组件 jar 的 size + SHA-256。

`THIRD-PARTY-NOTICES.txt`（3 921 B）：五组件 + 汇总表。
| 组件 | 版本 | 许可证 | 依据 |
|---|---|---|---|
| 本 mod（TIS NeoForge 移植） | 1.82.0+neoforge.1.21.1 | LGPL-3.0 | 与上游一致 |
| Carpet: NeoForged（chililisoup） | 1.4.147+v1.0.8-251027 | **MIT** | 该 jar `neoforge.mods.toml`：`license="MIT"`、`issueTrackerURL="https://github.com/chililisoup/neoforge-carpet/issues"`（§附录 A 有 dump） |
| Carpet 上游（gnembon） | 派生来源，不直接含 jar | MIT | 上游仓库 |
| conditional-mixin（Fallen_Breath） | 0.6.4 | **LGPL-3.0-only** | §2.3 全文；并注明「用户资料常写 MIT，实际声明是 LGPL-3.0-only」 |
| tiny-mappings-parser（FabricMC） | 0.3.0+build.17 | **Apache-2.0** | 该 jar `neoforge.mods.toml`：`license="Apache-2.0"` |

---

## 6. T5 文档回写

| 文件 | 改动 |
|---|---|
| `PORT-STATE.md` | 新增 `## 17. S9 归档`（`:822` 起）：拆包前后对照表、`packageConditionalMixin` 的 Gradle 校验坑、三环境表、dist 清单、S9 自检、S9 遗留 |
| `BLOCKERS.md` | 新增 `## 19. S9 结论`（`:329` 起）：`B-S9-1`（**已修**，含现象/根因/本地复现/五步修法摘要/验收/证据边界）、`B-S9-2`（记录，待查）、`19.4 下一步（仅一条）` |
| `RELEASE-NOTES.md` | §1 产物换成 S9 数值（1 689 654 B / `941E9C8A…` / 1645 条目）+ 新增「S9 起主 jar 不再内嵌任何第三方类」段；§2 兼容性表新增 `加载器（21.1.252，S9 新增）` 行；§2 新增「S9 起另外两件」表（tiny-mappings 必须放 / conditional-mixin 已有则不放）；§3 安装包结构换成 6 条目 + 手工安装改三个 jar + 删掉「已内嵌三块依赖」的旧断言；§4 差异表「依赖嵌入」「FabricLoader API」两行改写；§5.3 Disabled 口径改写（21 与 74 的来源）；§6.3 新增 S9 三环境摘要表；§7 许可改引 `THIRD-PARTY-NOTICES.txt` 五组件 |

### 6.1 「NeoForge 21.1.252 实测可加载至模块解析阶段」的证据边界（如实标注）

- **能证明的**：用户整合包实例（`D:\PCl2\.minecraft\versions\DIS机械动力航空学6.0.10-1.3.2\`，NeoForge 21.1.252 + Sinytra Connector）
  的 `logs\latest.log:453-454` 显示：本包被 mod 发现/依赖解析接受，一路走到 `cpw.mods.modlauncher.ModuleLayerHandler.buildLayer`（modlauncher 11.0.5）
  才因包冲突失败 —— 即 **21.1.252 上确实加载到了模块解析阶段**。
- **不能证明的（所以不写）**：修复后的包在 **21.1.252** 上能 `Done`。本轮所有修复验证都在 **21.1.264 专服**完成（`_s8_srv`），
  实例侧没有 headless 复现条件。21.1.252 与 21.1.264 同属 21.1.x 且 FML/modlauncher 同代，冲突机制（模块导出重复包）与版本无关 ——
  这一点由 C1b 在 21.1.264 上**复现出同类 `ResolutionException`**、再由 C2/C3 在同版本上**验证消失**来支撑。

---

## 7. 自检四铁律（本轮实测，**无新增**）

命令（PowerShell 5.1 的 `Select-String` 默认**大小写不敏感**，会把 `mapToDouble` 里的 `ToDo` 当成 `TODO` 命中，所以第三条必须加 `-CaseSensitive`）：

```powershell
$files = Get-ChildItem src -Recurse -File
($files | Select-String -Pattern 'require\s*=\s*0' -CaseSensitive).Count
($files | Select-String -Pattern '@Overwrite'      -CaseSensitive).Count
($files | Select-String -Pattern 'TODO|FIXME|UnsupportedOperationException' -CaseSensitive).Count
($files | Select-String -Pattern 'net\.fabricmc'   -CaseSensitive).Count
```

| 检查 | 本轮 | 基线 | 结论 |
|---|---:|---:|---|
| `require\s*=\s*0` | **4** | 4 | 未新增 |
| `@Overwrite` | **2** | 2 | 未新增 |
| `TODO\|FIXME\|UnsupportedOperationException` | **21** | 21 | 未新增（不加 `-CaseSensitive` 会误报 22） |
| `net\.fabricmc` | **8** 行 / 4 文件 | 8 | 未新增 |

`carpet-tis-addition.mixins.json` 未改动（`ConvertFrom-Json` 读出）：`required = True`、`package = carpettisaddition.mixins`、
`compatibilityLevel = JAVA_21`、`plugin = carpettisaddition.utils.mixin.CarpetTISAdditionMixinPlugin`、
`mixins = array[587]`、`client = array[12]`、`injectors = {defaultRequire=1}`、`overwrites = {requireAnnotations=True}`。

本轮**没有改任何 `src/main/java` 业务逻辑**：`build.gradle`（任务重写）、`src/main/resources/META-INF/neoforge.mods.toml`（+1 依赖块）、
`tools/`（新增 3 个脚本/工具）、`dist/`（打包物与两份 txt）、三份文档 —— 就这些。

---

## 8. 未完成 / 边界（不掩盖）

| 项 | 状态 |
|---|---|
| 客户端实机（进世界、`/carpet`、HUD Tab 列）本轮**未跑** | 沿用 S7/S8 记录；S9 改动只涉及打包方式与依赖声明，不改行为（§3.5 差集支撑） |
| NeoForge **21.1.252** 上修复后复测 | **未做**（实例侧无 headless 条件），见 §6.1 证据边界 |
| B-S9-2（51 条版本谓词诊断行消失） | **已记录未修**，需源码级排查 conditional-mixin neo 产物的注解读取/回调路径 |
| `util.teleport_hint` 运行期取证 | 仍为静态验证（S8 遗留，客户端 tooltip） |

---

## 附录 A：carpet fork 的许可证证据

```
> java tools\ZipArchive.java dump libs\carpet.jar META-INF/neoforge.mods.toml
modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
issueTrackerURL="https://github.com/chililisoup/neoforge-carpet/issues"
[[mods]]
modId="carpet"
version="1.4.147+v1.0.8-251027"
```

`libs\carpet.jar` 顶层段 = `META-INF` / `assets` / `carpet` / `carpet.mixins.json`（765 条目）。

## 附录 B：本轮新增/修改的文件

| 路径 | 说明 |
|---|---|
| `build.gradle:90-153` 区段 | `packageConditionalMixin` 取代 `stripConditionalMixin`；`releaseJar` 去掉 `me/**`、`net/**` 与三个 `from` |
| `build.gradle` `dependencies` / `genTinyMappingShimMeta` | 编译依赖切到 `libs/conditional-mixin-neoforge-0.6.4.jar`；tiny 模块改名 `tiny-mappings-parser (runtime library)` |
| `src/main/resources/META-INF/neoforge.mods.toml` | 新增 `[[dependencies.carpet_tis_addition]] modId="conditional_mixin"` 块（45 → 54 行） |
| `libs/conditional-mixin-neoforge-0.6.4.jar` | 新 vendored 官方产物（28 507 B，原样未改） |
| `tools/ZipArchive.java` | 新增：jar 条目 `list` / `tops` / `assert-absent` / `dump` |
| `tools/s9-run-env.ps1` | 新增：专服三环境驱动（建 mods、启服、判 Done、归档日志） |
| `tools/make-dist-zip.ps1` | 新增：dist 打包（显式条目名 + 未登记文件即拒绝） |
| `dist/README.txt` / `dist/THIRD-PARTY-NOTICES.txt` | 重写 / 新建 |
| `dist/mods/*`、`dist/optional/*`、`dist/TIS-Addition-NeoForge-1.21.1.zip` | 重新组装 |
| `PORT-STATE.md` / `BLOCKERS.md` / `RELEASE-NOTES.md` | 回写（§6） |

---

## 9. 追加实验（S9b）：能不能"合回一个 jar"？

问题：S9 把 conditional-mixin / tiny-mappings 拆成独立 jar 后，安装变成 3 个 jar。能不能合回去，还只有一个文件？

结论：**能，但只能走 JarJar（嵌套 jar），不能走"拍平类"**。

- 「合回去」= 把 class 拍平进主 jar（S8 做法）→ 只要存在另一个提供同包的 mod 就必崩（§3.3 C1b 已复现）。
- 「合回去」= 把 jar 放进 `META-INF/jarjar/` 并由 `META-INF/jarjar/metadata.json` 声明（TweakerMore 用的就是这招）→ FML 在加载时把它们拆成**独立 module**，包不会重复导出，一个文件即可。

实验工具：`tools\make-jarjar.ps1`（往发布 jar 里追加 `META-INF/jarjar/metadata.json` + 两个库 jar）。
产物：`_s9_payload\TIS-single-jarjar.jar`，**1 782 804 bytes**，SHA-256 `170D1B48073D320F19E0A545D803713C432E47E07A64237B78EDDB4128469724`。

### 9.1 只放 carpet + 这一个 jar（`-Env S9J1`）

```
[detected] [19:04:04] [Server thread/INFO] [minecraft/DedicatedServer]: Done (4.787s)! For help, type "help"
== env S9J1 : mods ==
   carpet-1.4.147+v1.0.8-251027.jar                                       1498683 bytes
   TIS-single-jarjar.jar                                                  1782804 bytes
```

`_s9_logs\S9J1.latest.log`（15 755 B）：

```
   9: Found mod file "TIS-single-jarjar.jar" [locator: {mods folder locator ...}, reader: mod manifest]
  13: Found mod file "conditional-mixin-neoforge-0.6.4.jar" [parent: TIS-single-jarjar.jar, locator: jarinjar, reader: mod manifest]
  14: Found mod file "tiny-mappings-parser-0.3.0+build.17.jar" [parent: TIS-single-jarjar.jar, locator: jarinjar, reader: mod manifest]
  21: 		conditional mixin 0.6.4 (conditional_mixin)
  24: 		tiny-mappings-parser (runtime library) 0.3.0 (tiny_mappings_parser_lib)
  51: [TISCM Mapping/INFO] ... Yarn mapping file yarn-1.21.1+build.3-v2.tiny loaded
```

计数：`ERROR=0 FATAL=0 ResolutionException=0 Duplicate mods=0 Done=1`。
→ 两个库确实由 **locator: jarinjar**（嵌套）加载成独立 module，主 jar 自己仍不含 `me/**`、`net/**`。

### 9.2 这一个 jar + 整合包里另有一份 conditional-mixin（`-Env S9J2`）

```
[detected] [19:04:50] [Server thread/INFO] [minecraft/DedicatedServer]: Done (4.316s)! For help, type "help"
== env S9J2 : mods ==
   carpet-1.4.147+v1.0.8-251027.jar                                       1498683 bytes
   TIS-single-jarjar.jar                                                  1782804 bytes
   conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar                      28507 bytes
```

`_s9_logs\S9J2.latest.log`（16 201 B）：

```
  11: [main/WARN] [net.neoforged.jarjar.selection.JarSelector/]: Attempted to select a dependency jar for JarJar which was passed in as source: conditional_mixin. Using Mod File: ...\mods\conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar
  15: Found mod file "tiny-mappings-parser-0.3.0+build.17.jar" [parent: TIS-single-jarjar.jar, locator: jarinjar, reader: mod manifest]
  22: 		conditional mixin 0.6.4 (conditional_mixin)
```

计数：`ERROR=0 FATAL=0 ResolutionException=0 Duplicate mods=0 Done=1`。
→ JarJar 发现外部已有一份满足 `[0.6.4,)` 的 `conditional_mixin`，就**跳过解包内嵌那份**（那条 WARN 就是它），不报 Duplicate、不报冲突。

### 9.3 三种方案对比（可直接决策）

| 方案 | 文件数 | 用户整合包里已有 conditional-mixin | 代价 |
|---|---|---|---|
| A 拍平（S8） | 1 | ❌ `ResolutionException` 必崩 | 已被 §3.3 C1b 证伪 |
| B JarJar 单文件（§9） | 1（carpet 除外） | ✅ `Done`，自动跳过内嵌副本 | 主 jar 变大（1 689 654 → 1 782 804 B）；嵌套库更新需重打包；FML 需在 `jarjar` 解包目录落盘 |
| C 拆分（S9 现状，已发） | 3 + 1 optional | ✅ `Done` | 用户要拷 3 个 jar，README 需讲放置规则 |

若采用 B：把 `tools\make-jarjar.ps1` 的逻辑并进 `releaseJar`（`jarJar` 配置或 `from` + `metadata.json`），dist 里 `mods\` 只留 carpet + 单文件即可；`optional\` 那份也不再需要。
## 10. S9c：换掉 mod 图标并重出 dist

### 10.1 做了什么
用户的反馈：S9 那版图标是「现代扁平卡片（圆角渐变 + 版本号横幅）」，风格不适合 MC mod；同时 S9 一直沿用上游 TIS 的旧图标没换过。
本轮改为 **Minecraft 原生像素画**路线：32×32 逻辑画布、整数 NEAREST 放大到 512、平涂色板（禁用渐变/投影/横幅）。
新脚本 `tools\make-icon2.py` 出 4 个候选（`build\icon-variants.png`）：

| 变体 | 内容 | 32px 可读性 |
|---|---|---|
| A | 羊毛棋盘底 + 像素 `TIS` + 青色横线（沿用上游棋盘母题） | 字母清晰 |
| **B（采用）** | 青绿地毯 + 织纹 + 琥珀流苏 + 大字 `TIS` | 地毯轮廓清晰 |
| C | 命令提示符 `> _` + 小字 `TIS` | 符号清晰、字偏小 |
| D | 上游 `C` 字形重绘 + 青色强调条 | `C` 清晰 |

采用 B：`src\main\resources\assets\carpettisaddition\icon.png`（512×512，1 827 B，`CACA60171FD3FFF3EE610DFE87EDF90278CA9AB2B0E126DBEAF9C729341ACCBC`）。
旧的两版都留档：`tools\assets\icon-upstream-tis.png`（上游 512×512，16 882 B）、`tools\assets\icon-v1-card.png`（本轮作废的扁平卡片版，84 243 B）。

### 10.2 重建与验证（命令输出）
```
[TIS-S9] produced ...\build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  (1674134 bytes)
BUILD SUCCESSFUL in 2s

  1638  assets/carpettisaddition/icon.png   comp=588  size=1827  crc=3682DBFA
== top-level segments ==
  META-INF / assets / carpet-tis-addition.mixins.json / carpettisaddition
== entries = 1645, distinct top segments = 4
```
主 jar 1 689 654 → **1 674 134 B**（`722A60C1078DD85E380FB31677F2FD83E20B355747152CE3BC751ADCB193BA31`），条目数仍是 1645、顶层段仍是 4 个（`me/**`、`net/**` 仍不存在，§1 的断言继续成立）。

跑一轮专服确认「发出去的就是测过的」（`_s9_logs\S9ICON.latest.log` 15 754 B，Mods = carpet + 单文件 jar）：
```
 13: Found mod file "conditional-mixin-neoforge-0.6.4.jar" [parent: TIS-single-jarjar.jar, locator: jarinjar, reader: mod manifest]
 14: Found mod file "tiny-mappings-parser-0.3.0+build.17.jar" [parent: TIS-single-jarjar.jar, locator: jarinjar, reader: mod manifest]
 73: [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (3.904s)! For help, type "help"
```
计数：`ERROR=0 FATAL=0 ResolutionException=0 Duplicate mods=0 Done=1`。

### 10.3 重出后的 dist（全部重算）
| 文件 | 字节 | SHA-256 |
|---|---|---|
| `dist\TIS-Addition-NeoForge-1.21.1.zip` | 2 917 758 | `D52FA3B72F7DC1B5A3E2C3E202C3623C76B8948B044971C9AC58C960B3ABB897` |
| `dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip` | 2 913 077 | `20039133471AAF1A3F840115B02AF6CC9334B9AEDF94D7EBC6506291FB1628B9` |
| `mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` | 1 674 134 | `722A60C1…B193BA31` |
| `mods\carpet-1.4.147+v1.0.8-251027.jar` | 1 498 683 | `6A81DB10…A34259F5C` |
| `mods\tiny-mappings-parser-0.3.0+build.17.jar` | 83 140 | `B00E557A…21F6AFDD3F` |
| `optional\conditional-mixin-neoforge-0.6.4.jar` | 28 507 | `36A570A4…098F9750` |
| `README.txt` | 11 696 | `4050C08FC26B014D57797BD12CC63B8E211EBBE6D1ECAB6B29A6AB722F2C0E27` |
| `THIRD-PARTY-NOTICES.txt` | 3 921 | `76E8F6AFCFD54032902E66E193DD94FD5181BB5B78C0BCD528C627B97019C9FA` |

`dist\README.txt` §8 的校验值已同步成新数值（旧值 1 689 654 / `941E9C8A…` 作废）。
打包脚本顺带修了两处：`make-dist-zip.ps1` 的默认路径改为从 `$PSScriptRoot` 推导（原先硬编码旧工程路径 `D:\Minecraft-mod-development\TIS-NeoForge`，目录迁移后会直接抛 `PathNotFound`），「未登记文件」守卫改为忽略顶层 `*.zip`（否则单文件版 zip 会被当成脏文件、脚本 `exit 2` 连三 jar 版一起不产）。

### 10.4 一处真实踩坑（记录，避免重犯）
`tools\make-icon2.py` 初版把画布写成 `list[list]` 却用 `px[x, y]` 索引 → `TypeError: list indices must be integers or slices, not tuple`；改成稀疏 `Grid(dict)` + `__missing__` 返回底色后正常。另外 `edit` 工具删掉换行导致一次 `SyntaxError: invalid syntax`，已修。

### 10.5 S9d：定稿为变体 D（用户选定）（S9d 阶段值，终值见 §11.5）
用户选定 **D**（骨白大写 `C` + 青色 `TIS`，沿用上游棋盘底）。D 在定稿前改了两轮：
1. 初版 `C` 16×17 偏左 + 嘴里一条悬空青色横杠 + 底部 3×5 小字 → 构图失衡、横杠像贴纸；
2. 二版把 `C` 缩到 16×15 居中、去掉横杠，底部换 5×7 字体放大 2 倍 → 文字被底边裁掉（`22+10 > 31`）；
3. 定稿：`C` 16×15 居中（`x0=8, y0=2`，笔画 3，四角削掉 1px 去硬角），`TIS` 用 5×7 字体 ×1 倍、青色、`y=20`，正好 `20..26` 落在 `frame` 内。

当前图标：`src\main\resources\assets\carpettisaddition\icon.png`，1 885 B，`B093B4E2FDE93C182992F53D5523BBCD6762B7B2A19780A0232914A1B7F7C3C5`。

重出后的数值（覆盖 §10.3）：
| 文件 | 字节 | SHA-256 |
|---|---|---|
| `dist\TIS-Addition-NeoForge-1.21.1.zip` | 2 917 847 | `E3A830BA8855839D6921C1262DA51B9A85538002DB0F00EAEB7CBC3E7B75A5FE` |
| `dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip` | 2 913 174 | `7C106ABF5EB7BC26919F8CFAF3081269F125825F769B8E45315A21E99412BACF` |
| `mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` | 1 674 227 | `4FE83BAE384266FD6A63FF596E491E0339E210781512116126F88173D556D805` |
| `README.txt` | 11 696 | `46D9A2A0279681392D593A0D2C15F61D9EC491739E8F1D264F9ADB99BA70BE84` |
| 单文件版主 jar（`_s9_payload\TIS-single-jarjar.jar`） | 1 767 377 | `FAB32BD2E6B5C9B6372799117E32C566040CCF56B1B2B00CDC5B2975C403965C` |

主 jar 校验：`1638  assets/carpettisaddition/icon.png  comp=681  size=1885  crc=3B875D11`，`entries = 1645`，顶层段仍是 `[META-INF, assets, carpet-tis-addition.mixins.json, carpettisaddition]`（`me/**`、`net/**` 依旧不存在）。
专服复验（`_s9_logs\S9ICON2.latest.log` 15 755 B）：`Done (5.632s)`，`ERROR=0 FATAL=0 ResolutionException=0 Duplicate mods=0`。

## 11. S9e：让 PCL2 / HMCL 能显示 mod 名字（补 `META-INF/mods.toml`）

用户反馈（复现图）：PCL2 社区版的 mod 列表里本 mod 显示成文件名 `carpet-tis-addition-1.82.0+neoforge.1.21.1` + 默认立方体图标。

### 11.1 根因（双证据）

* **源码**：`tsxc-github/PCL2-CE` @ `mzmc-dev`，`Plain Craft Launcher 2/Modules/Minecraft/ModLocalComp.vb`（104 893 B）：
  * `:636` `mcmod.info` → `:709` `fabric.mod.json` → `:769` `quilt.mod.json` → **`:810` `Dim TomlEntry As ZipArchiveEntry = Jar.GetEntry("META-INF/mods.toml")`**，成功即 `GoTo Finished`；失败落到 `:906` `Log(ex, "读取 mods.toml 时出现未知错误…", LogLevel.Developer)`。
  * 全文没有 `neoforge.mods.toml` 任何分支。
* **二进制**：`D:\PCl2\PCL2_CE（社区版）.exe`（5 261 312 B，未压缩）UTF-16LE 字符串扫描：`neoforge.mods.toml` **0 命中**；`mods.toml` 2 命中，上下文 `…META-INF/mods.toml.=.".'''.true.false.displayURL.dependencies.{0}.mandatory.side.versionRange.读取 mods.toml 时出现未知错误。…`；`logoFile` **0 命中**（→ 启动器不读 jar 内图标）。
* **为什么别的 mod 有名字/图标**：PCL2 用文件指纹查 CurseForge / Modrinth 缓存（`%TEMP%\PCL\Cache\LocalMod.json`，值里是 Modrinth Project 的 Slug/LogoUrl 等）；我们的自建 jar 两侧都未收录，只能回落文件名。`PCL\Log-CE1.log:71` `共有 108 个 Mod，其中 6 个需要联网获取信息` = 本地解析成功的占多数。

### 11.2 迷你解析器的要求（决定兼容文件怎么写）

`:817-902`：去 `#` 注释与空行 → 行首 `[` 且行尾 `]` 为段落头（`Trim("[]")`，故 `[[mods]]` → `mods`）→ 含 `=` 为键值（`"…"` / `'''…'''` / true|false / 数字 / 原样）→ 取段落名 `mods` 且必须有 `modId`，再读 `displayName` / `description` / `version`（`:886-891`）；**`displayURL` 与 `authors` 只从第一个段落（根，键名 `""`）读（`:892-893`）**。`ModId` setter（`:196-209`）要求长度>1、非纯数字、不含 `name`/`modid` 子串 —— `carpet_tis_addition` 通过。

### 11.3 做法与产物

新增 `src\main\resources\META-INF\mods.toml`（377 B，无 `#` 注释；`displayURL`/`authors` 放根段落）：

```toml
displayURL="https://github.com/TISUnion/Carpet-TIS-Addition"
authors="Fallen_Breath, TISUnion"

[[mods]]
modId="carpet_tis_addition"
version="1.82.0"
displayName="Carpet TIS Addition (NeoForge port)"
logoFile="assets/carpettisaddition/icon.png"
description="Carpet TIS Addition 的 NeoForge 1.21.1 移植版。一堆 Carpet 的扩展功能、调试工具与性能剖析器。"
```

FML 侧只读 `neoforge.mods.toml`，两份共存无害（实测，见下）。构建 `.\gradlew.bat --offline --console=plain packageConditionalMixin stripTinyMappings releaseJar`：
`[TIS-S9] produced …\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  (1674635 bytes)`；条目 1646，两条 toml：`META-INF/mods.toml comp=296 size=377`、`META-INF/neoforge.mods.toml comp=996 size=1826`。

### 11.4 验证

| 项 | 命令 / 日志 | 结果 |
|---|---|---|
| 专服加载（含两份 toml） | `tools\s9-run-env.ps1 -Env S9B3 -Mods '@carpet,@tis,@cm,@tiny' -Expect done` | `Done (5.381s)`；`S9B3.latest.log:19` 单条 `Carpet TIS Addition (NeoForge port) 1.82.0 (carpet_tis_addition)`；`ERROR / FATAL = 0 / 0`（全文 16 066 B） |
| 预演 jar（同内容，早一步验证） | `-Env S9PCLT` | `Done (6.148s)`，无 `Duplicate mods` |
| PCL2 解析模拟 | `python tools\pcl-parse-sim.py <jar>` | 旧 S8 jar：`FAIL  META-INF/mods.toml not present -> PCL2 falls back to filename`；新主 jar / 单文件版：`RESULT: PCL2 will show 'Carpet TIS Addition (NeoForge port)'` |
| 单文件版内嵌 | `ZipArchive list` + 模拟脚本 | `META-INF/mods.toml size=377` 在 `META-INF/jarjar/` 之外的主 jar 根上，解析结果同上 |

### 11.5 最终产物（覆盖此前各节里的旧值）

| 文件 | 字节 | SHA-256 |
|---|---|---|
| `build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` = `dist\mods\…` | 1 674 635 | `AF5DB35435006C4A0DC0FEB635B035A179B0CDB7E2D411DB57EFA40CB862CB17` |
| 单文件版主 jar `_s9_payload\TIS-single-jarjar.jar` | 1 767 785 | `C61AE6DC0867A3EA04B4852170688C15A1781994BC1A641155BF526452792917` |
| `dist\TIS-Addition-NeoForge-1.21.1.zip` | 2 918 691 | `4618BB193F4842E780D4F2F6FAF76DC5CA4A87CFABD463D9A5685114E9D2559D` |
| `dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip` | 2 913 744 | `57B23673345D5C44927CFFDB09795210398EBBB72BE35F5DD50DBB610DEE089E` |
| `dist\README.txt`（新增第十节「PCL2 里为什么之前只显示文件名」） | 12 907 | `9ED02DEC4CB029B243A2A40645040BEAEB3E6D89D6CD62DF9DFC4594DC7A66FC` |
| `dist\THIRD-PARTY-NOTICES.txt` | 3 921 | `76E8F6AFCFD54032902E66E193DD94FD5181BB5B78C0BCD528C627B97019C9FA` |

三 jar 版 zip 六条目：`README.txt` 12 907 / `THIRD-PARTY-NOTICES.txt` 3 921 / `mods/carpet-1.4.147+v1.0.8-251027.jar` 1 498 683 / `mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` 1 674 635 / `mods/tiny-mappings-parser-0.3.0+build.17.jar` 83 140 / `optional/conditional-mixin-neoforge-0.6.4.jar` 28 507。单文件版 zip 三条目：`README.txt` 3 786 / carpet 1 498 683 / 主 jar 1 767 785。

### 11.6 边界

* PCL2 的**图标**仍显示默认立方体：它只从 CF/Modrinth 拉图标，不读 `logoFile`。要显示自定义图标只能发布到这两个平台。
* 用户实例里已把两份旧 jar 换成新单文件版（`D:\PCl2\.minecraft\versions\DIS机械动力航空学6.0.10-1.3.2\mods\` 与 `…\versions\1.21.1-NeoForge_21.1.264\mods\`，均 1 767 785 B，旧文件移到各实例 `_mod-backup\`）；PCL2 需重启或刷新一次，因为它缓存了 mod 列表。
* 仅覆盖 PCL2 CE（社区版 exe 与 `mzmc-dev` 源码）；HMCL 未取源码核对，仅按同一 mini-parser 行为推断（`mods.toml` 是 HMCL 也支持的老格式）。
