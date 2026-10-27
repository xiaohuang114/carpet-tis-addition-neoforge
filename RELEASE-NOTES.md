# Carpet TIS Addition (NeoForge port) 1.82.0 — 发布说明

- 上游：[Carpet-TIS-Addition v1.82.0](https://github.com/TISUnion/Carpet-TIS-Addition)（Fabric / MC 1.21.1）
- 本包：NeoForge 移植版，**行为与 mixin 清单与上游 1.82.0 完全一致**（587 条 mixins / 12 条 client mixins，逐条与官方 jar 对过 diff）

## 1. 产物

| 项 | 值 |
|----|----|
| 文件名 | `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` |
| 路径 | `D:\Minecraft-mod-development\carpet-tis-neoforge移植（已收工）\TIS-NeoForge\build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` |
| 大小 | 1 674 635 bytes（S9e 补 `META-INF/mods.toml` 后；S9d 换图标 D 为 1 674 227，S9c 为 1 674 134，S9 为 1 689 654，S8 为 1 804 524） |
| SHA-256 | `AF5DB35435006C4A0DC0FEB635B035A179B0CDB7E2D411DB57EFA40CB862CB17` |
| 条目数 | 1646（S9e 起；顶层段只有 `META-INF` / `assets` / `carpettisaddition` / `carpet-tis-addition.mixins.json` 四个） |
| 图标 | `assets/carpettisaddition/icon.png`，512×512 像素画（骨白大写 `C` + 青色 `TIS`，沿用上游棋盘底），S9d 起定稿；生成脚本 `tools\make-icon2.py`（A/B/C/D 四个候选） |
| 启动器兼容 | `META-INF/mods.toml`（PCL2 只认这个名字；NeoForge 仍只读 `neoforge.mods.toml`，两者共存不影响加载），见 §6.4 |
| modId | `carpet_tis_addition` |
| 显示名 | Carpet TIS Addition (NeoForge port) |
| 版本 | 1.82.0 |

重新构建：`.\gradlew --offline --console=plain stripTinyMappings packageConditionalMixin releaseJar`
（S9 起 `releaseJar` 只含本 mod 自身；`packageConditionalMixin` 产出 `build\libs\conditional-mixin-neoforge-0.6.4.jar`，`stripTinyMappings` 产出 `build\libs\tiny-mappings-lib.jar`）。

**S9 起主 jar 不再内嵌任何第三方类**（`exclude 'me/**'` + `exclude 'net/**'`，`java tools\ZipArchive.java assert-absent <jar> me net` → `RESULT: PASS`），
依赖一律由 `dist` 包内的独立 jar 提供，见 §3。原因：把 `me/fallenbreath/**` 拍平进主 jar 后，与整合包里由 jar-in-jar 提供的
`conditional-mixin`（同一个 modId `conditional_mixin`）在模块层**导出同一个包**，modlauncher 直接 `java.lang.module.ResolutionException`
（B-S9-1，详见 `S9-ACCEPT.md` §0/§3.3）。

## 2. 运行环境

> ### ⚠ 单装本 jar 必崩，报错就是下面这句，**不是 bug**
>
> 只把 `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` 丢进 `mods/`、不装前置 `carpet` 时，游戏在**加载期**就被 FML 依赖检查拦下退出，日志原文（生产专服实测，`build\s8\nocarpet-latest.log`）：
>
> ```
> [main/FATAL] [net.neoforged.fml.ModLoader/CORE]: Error during pre-loading phase: Mod carpet_tis_addition requires carpet 1.4.147 or above
> Currently, carpet is not installed
> [main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
> net.neoforged.fml.ModLoadingException: Loading errors encountered:
> 	- Mod carpet_tis_addition requires carpet 1.4.147 or above
> 	  Currently, carpet is not installed
> ```
>
> 这句话的意思是「前置缺失」，唯一解法是把 `carpet-1.4.147+v1.0.8-251027.jar` 放进**同一个** `mods/` 目录。为了让这件事不可能再搞错，S8 起改为发**安装包** `dist\TIS-Addition-NeoForge-1.21.1.zip`，两个 jar 已一起放在包内 `mods\`，见 §3。

| 项 | 要求 / 实测 |
|----|----|
| Minecraft | **1.21.1**（`neoforge.mods.toml` 声明 `minecraft [1.21,1.21.1]`） |
| 加载器 | **NeoForge 21.1.x**（声明 `neoforge [21.1.0,)`；真机验收用的是 **21.1.264**，FML 4.0.45） |
| 加载器（21.1.252，S9 新增） | **实测可加载至模块解析阶段**：用户整合包实例（`D:\PCl2\.minecraft\versions\DIS机械动力航空学6.0.10-1.3.2\`，NeoForge **21.1.252** + Sinytra Connector）里本包完成了 mod 发现与依赖解析，一路走到 `cpw.mods.modlauncher.ModuleLayerHandler.buildLayer`（`logs\latest.log:453` `[main/ERROR] [cpw.mods.modlauncher.ModuleLayerHandler/]: Error while resolving modules.`），才因包冲突失败（B-S9-1，S9 已修）。证据边界：这条只证明「能加载到模块解析阶段」，**修复后的包尚未在 21.1.252 上复测**；S9 的修复验证在 21.1.264 专服完成，同一冲突机制在 21.1.264 上被本地复现（`ResolutionException`）并验证消失，详见 `S9-ACCEPT.md` §3.3 / §6.1 |
| Java | 21（class 版本 65） |
| 服务端/客户端 | 两边都要装（`side="BOTH"`） |

### 必须另装 `carpet` fork（强依赖，不是可选）

本 mod 是 carpet 的扩展，**必须同时安装 carpet 的 NeoForge 版本**：

| 项 | 值 |
|----|----|
| modId | `carpet` |
| 版本要求 | `>= 1.4.147`（`neoforge.mods.toml` 里 `versionRange="[1.4.147,)"`） |
| 实测通过 | `carpet-1.4.147+v1.0.8-251027`（Mods 列表显示为 `Carpet: NeoForged 1.4.147+v1.0.8-251027`） |

只装本包不装 carpet → 加载期即被 FML 依赖检查拦下，不进游戏。

### S9 起另外两件（安装包里已提供，判断方法见下）

| 组件 | 谁在用 | 放法 |
|---|---|---|
| `tiny-mappings-parser-0.3.0+build.17.jar`（modId `tiny_mappings_parser_lib`） | 本 mod 的 `TISCM Mapping` 线程（`StackTraceDeobfuscator` → `TinyMappingV2Reader` → `net.fabricmc.mapping.reader.v2.*`） | **必须放**。S9 把 `net/**` 从主 jar 移出后，缺它会抛 `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor` —— 而且**只出现在 stdout，不进 `latest.log`**（`S9-ACCEPT.md` §3.4 有原文） |
| `conditional-mixin-neoforge-0.6.4.jar`（modId `conditional_mixin`） | 本 mod 的 `@Restriction` / `RestrictiveMixinConfigPlugin`，以及调用 `VersionChecker` 的规则判定 | **已有则不放，没有才放**（放在安装包 `optional\` 里就是为这个）。整合包里 TweakerMore 之类 mod 会用 jar-in-jar 自带同一个 `conditional_mixin`，此时再放一份属于重复；NeoForge 21.1.264 实测同版本会静默去重、异版本取高版本（`S9-ACCEPT.md` §3.3 C2/C3），但**老版本 FML 会报 `Duplicate mods`**，所以别依赖这个行为。缺它的报错原文见 §2 上方警告块的同类格式：`Mod carpet_tis_addition requires conditional_mixin 0.6.4 or above / Currently, conditional_mixin is not installed` |

## 3. 安装

**推荐路线（S8 起）：解压安装包，别手工拷单个 jar。**

```
dist\TIS-Addition-NeoForge-1.21.1.zip                       2 918 691 B
                                                          SHA-256 4618BB193F4842E780D4F2F6FAF76DC5CA4A87CFABD463D9A5685114E9D2559D
  mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar       1 674 635 B   AF5DB354…C862CB17
  mods\carpet-1.4.147+v1.0.8-251027.jar                     1 498 683 B   6A81DB10…A34259F5C
  mods\tiny-mappings-parser-0.3.0+build.17.jar                 83 140 B   B00E557A…21F6AFDD3F
  optional\conditional-mixin-neoforge-0.6.4.jar                28 507 B   36A570A4…098F9750
  README.txt                                                  12 907 B（中文，含 conditional mixin 判断方法、缺前置/缺依赖报错原文、PCL2 显示说明、已知限制）
  THIRD-PARTY-NOTICES.txt                                      3 921 B（五组件许可与依据）
```

**另一个包（S9b 起可选）：单文件版**
`dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip` = 2 913 744 B / `57B23673345D5C44927CFFDB09795210398EBBB72BE35F5DD50DBB610DEE089E`，
内含 `README.txt` + `mods\carpet-…jar` + `mods\carpet-tis-addition-….jar`（**同一个主 jar，只是再套一层 JarJar**，内部 `META-INF/jarjar/` 带 `conditional-mixin-neoforge-0.6.4.jar` 与 `tiny-mappings-parser-0.3.0+build.17.jar`，主 jar 1 767 785 B / `C61AE6DC0867A3EA04B4852170688C15A1781994BC1A641155BF526452792917`）。
两个包是同一个 mod（同 modId、同版本）的两种包装，**不能混用**；单文件版不用判断 `conditional-mixin` 放不放，实测 `Done`（`S9-ACCEPT.md` §9）。

把包内 `mods\` 整个内容（**三个** jar）放进 `.minecraft\mods`（服务端同名目录）即可；`optional\` 里的那份按上面的判断方法决定放不放。

手工安装（等价）：

1. 装好 NeoForge 1.21.1 客户端（21.1.x 任意补丁版本）。
2. 把 **三个** jar 一起放进 `.minecraft\mods`：
   - `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar`（本包）
   - `carpet-1.4.147+v1.0.8-251027.jar`（carpet fork）
   - `tiny-mappings-parser-0.3.0+build.17.jar`（运行期映射解析）
   - 另：若整合包里没有任何 mod 提供 `conditional_mixin`，再把 `optional\conditional-mixin-neoforge-0.6.4.jar` 放进去。
3. 启动。Mods 列表应看到 `Carpet TIS Addition (NeoForge port) 1.82.0`（ModID `carpet_tis_addition`，State: loaded）。
4. 游戏内 `/carpet` 应回 `Carpet TIS Addition version: 1.82.0` + 分类列表。

~~本包已内嵌三块运行期依赖~~ —— **S9 起不再内嵌**。旧写法（把 conditional-mixin / fabric-loader-shim / tiny-mappings-parser 拍平进主 jar）正是
`java.lang.module.ResolutionException: Modules carpet_tis_addition and conditional_mixin export package me.fallenbreath.conditionalmixin.api.mixin to module …` 的根因（B-S9-1）。
现在主 jar 里 `me/`、`net/` 两个顶层目录都不存在（`assert-absent` 断言 PASS），三方类一律由独立 jar 提供。

## 4. 与原始 Fabric 版的差异

| 方面 | Fabric 1.82.0 | 本 NeoForge 版 |
|------|---------------|----------------|
| modId | `carpet-tis-addition` | `carpet_tis_addition`（NeoForge loader 4 强制 `^[a-z][a-z0-9_]{1,63}$`，短横线非法） |
| 依赖嵌入 | Fabric Loader 的 jar-in-jar | **S9 起不嵌入任何第三方类**：主 jar 只有本 mod 自身（`exclude 'me/**'` + `exclude 'net/**'`），conditional-mixin（modId `conditional_mixin`，`[0.6.4,)`）与 tiny-mappings-parser 作为**独立模块 jar** 随安装包发布，靠 `neoforge.mods.toml` 的 `[[dependencies.…]]` 声明依赖。旧做法（合并 jar）会在模块层导出重复包，见 B-S9-1 |
| FabricLoader API | 真 fabric-loader 提供 | conditional-mixin 改用**官方 NeoForge 产物**（`libs\conditional-mixin-neoforge-0.6.4.jar`，28 507 B，原样未改），由它自带 `api/util/neoforge/VersionCheckerImpl`，不再需要 `net/fabricmc/loader/**` shim；主 jar 里 `net.fabricmc` 引用仅剩 8 行（与本包无关的历史调用点，数量不变） |
| 自定义网络包 | mixin 改 `CustomPacketPayload.codec` 静态列表即可 | 双轨：mixin 修静态 codec 列表 + `TISCMNetworkRegistry` 用 `RegisterPayloadHandlersEvent` 把 `tiscm:network/v1` 注册进 NeoForge payload 注册表（NeoForge 对未注册 channel 直接 disconnect） |
| refmap | 有 | **不打包 refmap**（dev 即 mojmap，`@Shadow`/目标解析走 MixinEnvironment remapper 链） |
| 翻译 | 直接带 lang JSON | 构建期由 `lang/*.yml` 经 `convertTranslations` 展平成 `assets/carpettisaddition/lang/{en_us,zh_cn}.json`（各 1142 键，与官方 jar 键集合 diff = 0） |

## 5. 已知限制（如实记录）

1. **fabric-api 相关兼容项不生效**：上游部分 `@Restriction`/兼容分支依赖 fabric-api / lithium / minihud 等 Fabric 生态 mod 的 API；这些 mod 在 NeoForge 侧不存在或 API 不同，相关兼容逻辑走不到（不影响主功能）。
2. **自检口径与上游一致，未新增技术债**（构建前后均为）：`require=0` 出现 4 次、`@Overwrite` 2 次、`TODO/FIXME/UnsupportedOperationException` 21 次、`net.fabricmc` 8 行。
3. **按上游设计跳过的 mixin 条数取决于运行场景与日志口径**：真机客户端/长会话 `[TISCM] Disabled mixin` = **74**（S7 记录，dev 同为 74）；S9 干净专服短跑到 `Done` 时是 **21**。差额来源已核实（`S9-ACCEPT.md` §3.6）：S8 的 72 条里有 **53 条**属于 `due to mod minecraft@1.21.1 does not matches version predicates [...]`（如 `[<1.16]`），S9 的 21 条全部属于「required mod 缺失 / tester result=false」这一类，且**版本谓词类在 S9 里不再打印诊断行**（mixin 是否应用的结果未变，见 B-S9-2）。用 21 还是 74 取决于你在数哪个运行、哪个日志，**不要用它判断功能是否被限制**。
4. **`/speedtest` 默认关闭**：规则 `commandSpeedTest` 默认值是 `"false"`（与上游一致），用前先 `/carpet commandSpeedTest true`。命令本身没有 `help` 子命令：裸 `/speedtest` 打印用法（`Test the network connection speed and ping. Test size limit: 10MiB; Client supported: no`），子命令只有 `download` / `upload` / `ping`。
5. **`Client supported: no`**：上游从 MC 1.20.2 起就没有客户端侧 TISCM 网络处理器（`carpet.tweaks.robustness.network.ClientNetworkHandlerMixin` 在本版本是 `@Mixin(DummyClass.class)`，源码注释 `// impl in [mc1.15, mc1.20.2)`），所以 `/speedtest ping/upload` 会回 `Client does not support TISCM speed test. Please install or update the Carpet TIS Addition mod`。这是上游行为，不是移植缺陷。
6. **HUD logger 的显示位置是玩家列表（Tab 面板），不是屏幕叠加层**：`/log <logger>` 订阅成功后，HUD 行出现在按 Tab 打开的玩家列表表尾（carpet 表头/表尾）。这是 carpet 的既有设计：`HUDController.update_hud` 把 `player_huds` 拼成 `Component`，包进 `ClientboundTabListPacket(header, footer)` 由服务端 `player.connection.send(...)` 发给客户端，渲染交给原版 `PlayerTabOverlay`。S8 对 `libs\carpet.jar`（fork）与上游 `remapped.carpet-fabric-carpet-8f637a66-1.21-1.4.147+v240613.jar` 做了同口径字节扫描 + `javap`：两边 `HUDController` 方法表完全一致（`register` / `resetScarpetHUDs` / `addMessage` / `clearPlayer` / `update_hud(MinecraftServer, List<ServerPlayer>)`，**没有任何 render 方法**），两边 `carpet.mixins.json` 的 client 段 14 条同名同集合（含 `Gui_tablistMixin`、`PlayerTabOverlayMixin`），真机日志里两条都在应用。**旧结论「fork 掉了 client mixin / 该 fork 没有渲染入口」已被证伪**，详见 `S8-ACCEPT.md` §4。carpet 侧与 TIS 侧都不存在独立的 HUD 叠加层渲染钩子，所以"屏幕上没有浮层"是上游行为，不是本移植缺陷；本包也**不通过改 587 条清单**去加（S7 铁律）。HUD 相关功能全程无异常/无崩溃。
7. **文本类验收口径（S8 更新）**：`tracker.lifetime.spawn_reason.*`、`tracker.lifetime.removal_reason.*`、`command.raid.status` 三组文本已在**生产专服**用 RCON 触发真实场景取到运行期输出（✅ 运行期验证，原文见 `S8-ACCEPT.md` §3）；仅 `util.teleport_hint`（鼠标悬停提示）属于客户端 tooltip，RCON 取不到，**本轮明确不取证**（⚠️ 仅静态验证：`Messenger.java:307` / `:317` 两个调用点 + 键值 diff=0），不再把它当成"已动态验证"。

## 6. 真机验收摘要（NeoForge 21.1.264 + MC 1.21.1，仅 carpet + 本包）

| 项 | 结果 |
|----|------|
| Mods 列表 | `Carpet TIS Addition (NeoForge port) 1.82.0`，`Version: 1.82.0`，`ModID: carpet_tis_addition State: loaded`，无红字 |
| mixin applied（进世界后，**仅本 mod**） | **464**（= 464 条 `Mixing ... from carpet-tis-addition.mixins.json into` 行 / 459 个唯一 mixin 类；dev 客户端同为 464 / 459，逐条一致，无差额） |
| 全场 mixin 总数（参考，非本 mod） | **645** = 本 mod 464 + `carpet.mixins.json` 179 + `neoforge.mixins.json` 2 |
| client mixins（12 条） | 9 applied + 2 按 `@Restriction(<1.16)` 跳过 + 1 `DummyClass` 空壳 = 12（与 dev 完全一致） |
| conditional-mixin 跳过条数 | 74（dev 74） |
| ERROR / FATAL | **0 / 0**（1015 行日志全文扫描） |
| Mixin apply failed / Critical injection failure | 0 / 0 |
| NoClassDefFoundError / NoSuchMethodError / NoSuchFieldError / ClassNotFoundException | 0 / 0 / 0 / 0 |
| `/carpet` | ✅ `Carpet TIS Addition version: 1.82.0` + 16 个分类 |
| `/info world weather` | ✅ `Rain in 118.8min`（真文本，非裸键） |
| `/speedtest`（开 `commandSpeedTest`） | ✅ 真实用法文本 |
| `/log mobcaps` + `/log` | ✅ 订阅成功、logger 列表真文本，无崩溃（面板不渲染见 §5.6） |
| `/player Alex spawn` → `kill` → `spawn` | ✅ entity id 1401 → 1420，无报错 |
| 退出 | ✅ `All dimensions are saved`，无异常 |

> **口径订正（S8 / T1）**：本 mod 自身的 mixin 应用数是 **464**，不是 645 —— 645 为**全场 mixin 总数**（本 mod 464 + carpet 179 + neoforge 2），非本 mod 的条数；早期版本这几处把 645 当成"本 mod applied"记错了，已按 `tools\s8-count.ps1` 的单一口径（只匹配 `from carpet-tis-addition.mixins.json into`）全部订正。另外 464 是**运行期应用行数**，随加载到的目标类变化，不等于 mixin 清单里声明的 587 条：客户端 464（459 个唯一 mixin 类）、dev 服务端 453（448 唯一）、**生产专服 461（456 唯一，跑过 raid/lifetime 场景）与 443（438 唯一，短跑重启），两个数都是 `logs\debug.log` 口径**。三份历史日志在同一脚本下分别为 S5 444 / S6 服务端 453 / S6 客户端 464，与旧文档记录一致。

## 6.1 干净专服复验（S8 / T5，NeoForge 21.1.264 + Java 21，mods 只有安装包里那两个 jar）

| 项 | 结果 |
|----|------|
| 服务端 | `_s8_srv`，`neoforge-21.1.264-installer.jar --installServer` 全新安装，`mods\` 仅 2 个 release jar |
| Mods 列表 | `Carpet TIS Addition (NeoForge port) 1.82.0`、`Carpet: NeoForged 1.4.147+v1.0.8-251027`、`Minecraft 1.21.1`、`NeoForge 21.1.264` |
| 启动 | ✅ `Done (31.760s)! For help, type "help"`；`RCON running on 0.0.0.0:25575` |
| mixin applied（本 mod） | **461**（456 唯一，第一轮跑了 raid/lifetime 场景后）/ **443**（438 唯一，第二轮短跑重启后）——两轮都是 `Mixing ... from carpet-tis-addition.mixins.json into` 行数。注意生产专服把这些行打在 **DEBUG** 级，只出现在 `logs\debug.log`；`logs\latest.log` 里 0 条（口径陷阱，见 §6.2）。数字随「已加载到的目标类」增长，是运行期计数，不是清单条目 |
| 全场 mixin 总数 | 630 = 本 mod 461 + `carpet.mixins.json` 167 + `neoforge.mixins.json` 2（短跑那轮为 607 = 443 + 162 + 2） |
| Disabled mixin | **72**（客户端 74；差额是两条 client-only 的 `@Restriction(<1.16)`） |
| ERROR / FATAL / Mixin apply failed / Critical injection failure | **0 / 0 / 0 / 0**（`latest.log` 357 行、`debug.log` 3447 行全文扫描） |
| `/carpet` | ✅ `Carpet Mod version: 1.4.147+v1.0.8-251027` + `Carpet TIS Addition version: 1.82.0` + 15 个分类 |
| `/lifetime` `/raid list` `/raid list full` `/lifetime zombie spawning` `/lifetime zombie removal` `/sleep 1 s` | ✅ 全部真文本（原文见 `S8-ACCEPT.md` §3 / §5） |
| 删掉 carpet jar 再启动 | ❌ 按预期失败，报错与用户遇到的一致（见 §2 警告块原文） |

## 6.2 口径陷阱：`Mixing` 行在专服只在 `logs\debug.log`

开发环境（`gradlew runServer` / `runClient`）和 S7 真机客户端的 `Mixing ... into` 行打在 **INFO**，所以 `logs\latest.log` 里能数到 464；**生产专服**把它们打在 **DEBUG**，`latest.log` 里数是 **0**，必须对 `logs\debug.log` 数。用 `tools\s8-count.ps1` 时同时喂两个文件即可看出差别：

```
& .\tools\s8-count.ps1 -Log '_s8_srv\logs\latest.log,_s8_srv\logs\debug.log'
 latest.log  -> TIS applied 0   / Disabled 72 / ERROR 0
 debug.log   -> TIS applied 461 / Disabled 72 / ERROR 0   (carpet-tis-addition 461 + carpet 167 + neoforge 2)
```

所以「专服 applied = 0」是**日志级别**造成的假象，不是 mixin 没应用。

验收截图与日志：`D:\Minecraft-mod-development\TIS-NeoForge\build\shots\s7\`（`01-mainmenu` … `23-quit-to-title`、`client-latest.log`、`client-stdout.log`）。

## 6.3 S9 三环境摘要（NeoForge 21.1.264 专服，`tools\s9-run-env.ps1`，日志归档在 `_s9_logs\`）

| 环境 | mods 组合 | 结果 | 关键日志行 |
|---|---|---|---|
| A | carpet + 本 mod（**不给** conditional-mixin） | ❌ 按预期失败 | `S9A.latest.log:21` `Missing or unsupported mandatory dependencies:`、`:22` `Mod ID: 'conditional_mixin', Requested by: 'carpet_tis_addition', Expected range: '[0.6.4,)', Actual version: '[MISSING]'`、`:25` `Mod carpet_tis_addition requires conditional_mixin 0.6.4 or above` / `:26` `Currently, conditional_mixin is not installed` |
| B | + conditional-mixin + tiny-mappings-parser | ✅ `Done (7.536s)!` | applied **437**（唯一类 432）、Disabled 21、`ERROR / FATAL / apply-failed = 0 / 0 / 0`（`S9B.debug.log` 773 027 B 全文扫描）；`TISCM Mapping` 线程加载 yarn 映射成功 |
| B2（反证） | 去掉 tiny-mappings-parser | ⚠️ 能 `Done (7.211s)` 但 stdout 抛异常 | `S9B2.stdout.log:94` `Exception in thread "TISCM Mapping" java.lang.NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`、`:99` `ClassNotFoundException: net.fabricmc.mapping.reader.v2.TinyVisitor` → 这就是 tiny-mappings-parser 必须随包发的原因 |
| C1 | 旧拍平 jar + 模块 jar | ❌ 复现冲突 | `S9C1.latest.log:26` `java.lang.module.ResolutionException: Modules carpet_tis_addition and tiny_mappings_parser_lib export package net.fabricmc.mappings.model to module carpet` |
| C1b | 旧拍平 jar + 模块 jar（不给 tiny） | ❌ 复现用户那条 | `S9C1b.latest.log:24` `java.lang.module.ResolutionException: Module conditional_mixin contains package me.fallenbreath.conditionalmixin.api.annotation, module carpet_tis_addition exports package … to conditional_mixin` |
| C2 | 新包 + 两份相同的 conditional_mixin（其中一份从 TweakerMore 的 jar-in-jar 解出） | ✅ `Done (8.094s)!` | `S9C2.latest.log:22` Mod List 只有一行 `conditional mixin 0.6.4 (conditional_mixin)`；**`ResolutionException` = 0** |
| C3 | 新包 + conditional-mixin 0.6.4 与 0.6.5 fixture 同时放 | ✅ `Done (9.028s)!` | `S9C3.latest.log:56` `conditional_mixin (version 0.6.4 -> 0.6.5)`（取高版本）；**`ResolutionException` = 0** |

同口径与 S8 对比（同一台 `_s8_srv`、同脚本）：S8 短跑 `build\s8\srv-run2-debug.log` = 443 行 / 438 唯一类，S9B = 437 / 432；
差集只-in-S8 8 条（全部需要玩家/指令才加载目标类）、只-in-S9 2 条 → 属同一量级，与 S9 改动无关（明细见 `S9-ACCEPT.md` §3.5）。
S9 全部细节（命令、断言、踩坑、证据边界）见 `S9-ACCEPT.md`。

## 6.4 启动器（PCL2 / HMCL）里显示成文件名的原因与修法（S9e）

**现象**：PCL2 社区版 mod 列表里本 mod 显示成 `carpet-tis-addition-1.82.0+neoforge.1.21.1` 两行 + 默认立方体图标，而同列表里其它 mod 都有名字/图标。

**根因**（源码 + 二进制双证据，`S9-ACCEPT.md` §11）：PCL2 CE 读 mod 元数据只走 `mcmod.info` → `fabric.mod.json` → `quilt.mod.json` → **`META-INF/mods.toml`** 四支（`Plain Craft Launcher 2/Modules/Minecraft/ModLocalComp.vb:810` `Jar.GetEntry("META-INF/mods.toml")`），**没有 `neoforge.mods.toml` 分支**；exe 字符串里也搜不到 `neoforge.mods.toml`。NeoForge 1.20.5 起把描述文件改名成 `neoforge.mods.toml`，于是未被 CF/Modrinth 收录的自制 jar 只能回落到文件名。其它 mod 能显示，是因为 PCL2 用文件指纹命中了 CurseForge / Modrinth 缓存（`%TEMP%\PCL\Cache\LocalMod.json`），不是本地解析。

**修法**：主 jar 里额外放一份 `META-INF/mods.toml`（只含 `displayURL`/`authors` 根键 + `[[mods]]` 的 `modId`/`version`/`displayName`/`description`/`logoFile`），NeoForge 仍然只读 `neoforge.mods.toml`，两份共存不影响加载。
- 复验：`_s9_logs\S9B3.latest.log` `Done (5.381s)`，Mod List 单条 `Carpet TIS Addition (NeoForge port) 1.82.0 (carpet_tis_addition)`，`ERROR / FATAL = 0 / 0`。
- 解析模拟（按 VB 逻辑逐条复刻，`tools\pcl-parse-sim.py`）：旧 S8 jar → `FAIL  META-INF/mods.toml not present -> PCL2 falls back to filename`；新主 jar 与单文件版 → `RESULT: PCL2 will show 'Carpet TIS Addition (NeoForge port)'`。
- **图标仍无法在启动器里显示自定义图**：PCL2 只从在线工程拉图标，不读 jar 内 `logoFile`（exe 里无该字符串）。要显示自定义图标必须发布到 CurseForge / Modrinth。

## 7. 许可

LGPL-3.0（与上游一致）。安装包内 `THIRD-PARTY-NOTICES.txt` 列出五个组件的许可与逐条依据：
本 mod LGPL-3.0、Carpet: NeoForged（chililisoup）**MIT**、Carpet 上游（gnembon）MIT、conditional-mixin（Fallen_Breath）**LGPL-3.0-only**、
tiny-mappings-parser（FabricMC）**Apache-2.0**。`optional\conditional-mixin-neoforge-0.6.4.jar` 内自带 `LICENSE_conditional-mixin`，原样分发。
