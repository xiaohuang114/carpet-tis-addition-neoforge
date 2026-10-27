# S8 验收报告 — Carpet TIS Addition (NeoForge port) 1.82.0

- 工作目录：`D:\Minecraft-mod-development\TIS-NeoForge`
- 时间：2026-10-25 17:26 → 17:52（Asia/Shanghai）
- 本轮范围：**只做 T1–T5**。没重构、没动 1009 个 java 文件的业务逻辑、没动 `carpet-tis-addition.mixins.json`（587 mixins / 12 client / `require=true` / `JAVA_21` / md5 `63980334F58FB07D1218D070242D62DD` 全部未变）。
- 自检口径（S8 实测，与基线逐项相等，未新增）：

```
require\s*=\s*0（src/main/java + compat-src）: 4     （基线 4）
@Overwrite                                   : 2     （基线 2）
TODO|FIXME|UnsupportedOperationException      : 21    （基线 21）
net\.fabricmc（仅 src/main/java）             : 8     （基线 8；compat-src 另有 12 行属 shim 本体）
```

- 源码零改动证明：按 mtime 统计 `src\`、`compat-src\` 在 16:35（S7 出包时刻）之后被修改的文件数 = **0 / 0**；唯一晚于该时刻的非源码文件是 `build.gradle`（16:53:45，属 S7 的 `releaseJar` 任务改动，早于本轮开始时间 17:26）。
- 本轮新增/改动：`tools\s8-count.ps1`、`dist\`（README.txt + zip + mods/）、`RELEASE-NOTES.md`、`PORT-STATE.md`、`BLOCKERS.md`、`S8-ACCEPT.md`、`build\s8\`（日志与取证副本）。
- 每个结论后面标了证据类型：**【命令输出】** / **【日志原文】** / **【文件实测】**；做不到的一律写 INCOMPLETE + 缺什么。

---

## 1. T1 计数口径订正表（645 → 464）

### 1.1 新工具

`tools\s8-count.ps1`（纯 ASCII，PowerShell 5.1 兼容；`-Log 'a.log,b.log' -Out file`），对任意 NeoForge 日志打印三行：

1. 本 mod applied：**只**匹配 `Mixing ... from carpet-tis-addition.mixins.json into` 的行数
2. `Disabled mixin` 数（`[TISCM] Disabled mixin`）
3. `ERROR` / `FATAL` / `apply failed` 三类串计数

附带打印该日志里出现的所有 mixin config 及其条数（用来直接看出"本 mod vs 全场"）。

复现命令：

```powershell
cd D:\Minecraft-mod-development\TIS-NeoForge
& .\tools\s8-count.ps1 -Log '_run_s5.log,_s6_server2.log,_s6_client4.log,build\shots\s7\client-latest.log,run\logs\latest.log' -Out build\s8\count-all.txt
& .\tools\s8-count.ps1 -Log '_s8_srv\logs\latest.log,_s8_srv\logs\debug.log' -Out build\s8\count-t5.txt
```

### 1.2 五份历史日志 + 两份专服日志的重跑结果【命令输出】

| 日志 | 字节 | 本 mod applied | 唯一 mixin 类 | Disabled | ERROR/FATAL/failed | 分布（tis+carpet+neoforge） |
|------|------|----------------|----------------|----------|--------------------|-----------------------------|
| `_run_s5.log`（S5 dev 服） | 344195 | **444** | 439 | 71 | 9 / 0 / 1 | 444+162+2 |
| `_s6_server2.log`（S6 dev 服） | 191640 | **453** | 448 | 72 | 0 / 0 / 0 | 453+165+2 |
| `_s6_client4.log`（S6 dev 客） | 188289 | **464** | 459 | 74 | 0 / 0 / 0 | 464+179+2 |
| `build\shots\s7\client-latest.log`（S7 真机客） | 210101 | **464** | 459 | 74 | 0 / 0 / 0 | 464+179+2 |
| `run\logs\latest.log`（S7 dev 客） | 191750 | **464** | 459 | 74 | 0 / 0 / 0 | 464+179+2 |
| `run\logs\srv-s5-latest-baseline.log`（S5 基线） | 293474 | 444 | 439 | 71 | 9 / 1 / 1 | 444+162+2 |
| `_s8_srv\logs\debug.log`（S8 专服，跑过 raid/lifetime） | 832898 | **461** | 456 | 72 | 0 / 0 / 0 | 461+167+2 |
| `build\s8\srv-run2-debug.log`（S8 专服，短跑重启） | 791814 | **443** | 438 | 72 | 0 / 0 / 0 | 443+162+2 |
| `_s8_srv\logs\latest.log`（S8 专服 latest） | 63985 | **0** | 0 | 72 | 0 / 0 / 0 | 空 |

> 脚注1：`_s8_srv\logs\debug.log` 在服务器还在跑时被脚本读过一次，当时读到 **437 / 163 / 2**（文件 781733 B）；归档副本（835072 B，含 raid/lifetime 场景期间的追加）读数为 **461 / 167 / 2**。这正说明 applied 是**运行期计数**，随"已加载到的目标类"增长，不是清单条目数。
> 脚注2：`build\s8\nocarpet-latest.log`（删掉前置后启动）的 `3 / 3` 是**故意制造**的依赖检查 FATAL/ERROR（见 §5.4），不属于本包缺陷。
> 全部数字的原始输出：`build\s8\count-all.txt`（6 份历史日志）、`build\s8\count-t5.txt`（两轮专服 + 缺前置那轮）。

### 1.3 结论

- **645 = 464（本 mod）+ 179（carpet）+ 2（neoforge）**，是三份 mixin config 的应用行**合计**。RELEASE-NOTES 之前把它写成"本 mod applied"是错的，已改。
- `464` 是**运行期应用行数**，不是清单里的 `587`：同一份清单在不同环境/不同加载进度下数出来不同（客 464 / dev 服 453 / 生产服 461、443）。464 行对应 **459 个唯一 mixin 类**，重复来源（同一 mixin 类多个 target）：`logger.damage.LivingEntityAndPlayerEntityMixins$ApplyDamageMixin`=2、`...$DamageMixin`=3、`logger.microtiming.tickstages.scarpet.CarpetEventServer_EventMixin`=3。
- S5 独有的 4 条（`command.info.compat.lithium.ChunkTickSchedulerMixin`、`command.stop.StopCommandMixin`、`utils.entityfilter.EntitySelectorAccessor`、`utils.entityfilter.EntitySelectorMixin`）与 S7 多出的 24 条（client mixin、shapes、`command.lifetime.*`、`NetherPortal*` 等）都是**版本/规则条件不同**导致的运行期差集，不是清单改动。

### 1.4 文档订正落点（逐处改，不只改最新一行）

| 文件 | 位置 | 原内容 | 现值 |
|------|------|--------|------|
| `RELEASE-NOTES.md` | §6 表 | `mixin applied（进世界后） \| **645**` | `**464**`（459 唯一）+ 新增一行 `全场 mixin 总数 645 = 464+179+2` |
| `RELEASE-NOTES.md` | §6 表后 | — | 新增「口径订正（S8 / T1）」引述块：645 是全场、464 是运行期行数、≠ 587 |
| `RELEASE-NOTES.md` | §6.1 / §6.2 | — | 新增专服复验表 + 口径陷阱说明（专服只在 `debug.log` 有 `Mixing` 行） |
| `PORT-STATE.md` | :701（§15.4 真机表） | `\| mixin applied（进世界后） \| **645** == dev 645` | `**464** == dev 464（459 唯一）；645 是全场总数，S8 订正，见 §16.1` |
| `PORT-STATE.md` | :736（§15.7 结论） | `（645 / 74 / 12=9+2+1）` | `（**464** / 74 / 12=9+2+1；全场 645 = 464+179+2，S8 订正）` |
| `PORT-STATE.md` | §14.7 历史 444/453 | 保留 | 与新脚本口径**一致**（444/453 复核成立），未改 |
| `BLOCKERS.md` | §18.2 | — | 新增 B-S8-1：专服 `Mixing` 行只在 `debug.log` |

grep 复查：`RELEASE-NOTES.md` 全文已无"把 645 当本 mod applied"的表述（剩下出现的 645 都带"全场总数"限定）；`PORT-STATE.md` 两处已改。

---

## 2. T2 dist 清单（根治"只装一个"）

### 2.1 验收命令与输出【命令输出】

```
PS> Get-ChildItem dist\*.zip

Name          : TIS-Addition-NeoForge-1.21.1.zip
Length        : 2932578
LastWriteTime : 2026/10/25 17:47:28

PS> (zip 条目)
mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar      1804524 B  compressed=1511903
mods/carpet-1.4.147+v1.0.8-251027.jar                    1498683 B  compressed=1417449
README.txt                                                  5512 B  compressed=2780
```

三个文件都在；条目路径用**正斜杠**（用 `ZipArchive` 手工写 entry，避免 `Compress-Archive` 在 PS 5.1 下写反斜杠的兼容问题）。

### 2.2 校验值

| 文件 | 字节 | SHA-256 |
|------|------|---------|
| `dist\TIS-Addition-NeoForge-1.21.1.zip` | 2932578 | `BDE8C0B89DB859C4A06FDA25952ABC29BE2042A963D70BA26A515C96491604E4` |
| `dist\mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` | 1804524 | `0B2CA7CB21854208F88FF3ED773DBF6672A8EA7C9A124FCD02846433AB2A3EE9`（与 §1 产物同字节） |
| `dist\mods\carpet-1.4.147+v1.0.8-251027.jar` | 1498683 | `6A81DB1098A92D7239DAA992B8508DAEF7EB2528C4F7C9024AE3A81A34259F5C` |
| `dist\README.txt` | 5512 | `215519062201D453EB0144097C16C2A23F4FB3700EF87056F3108EF5F0B13B52` |

### 2.3 解压回环校验【命令输出】

`ZipFile::ExtractToDirectory` 到 `build\s8\dist-check\`：

```
README.txt                                              5512 B  sha256=215519062201D453
mods\carpet-1.4.147+v1.0.8-251027.jar                1498683 B  sha256=6A81DB1098A92D72
mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar  1804524 B  sha256=0B2CA7CB21854208
README.txt first3bytes=EF BB BF      ← UTF-8 BOM，Windows 记事本不会乱码
```

### 2.4 `README.txt` 内容覆盖（中文，5 512 B）

1. 包里有什么 + 「**mods\ 里必须两个 jar 都在，少放 carpet 那个一定启动失败**」加粗置顶
2. 安装 5 步（含 `/carpet` 期望回显、Mods 列表期望行、内嵌依赖说明）
3. 环境要求：MC 1.21.1 / NeoForge 21.1.x / Java 21 / 前置 `carpet >= 1.4.147` / BOTH
4. **缺前置报错原文**（与 §5.4 同一段，一字不改，便于用户搜索）
5. 已知限制 6 条（fabric-api 兼容项、Disabled mixin 72/74、`/speedtest` 默认关、`Client supported: no`、**HUD 在 Tab 列表而非浮层**、`teleport_hint` 需客户端 hover）
6. 两个 jar 的大小 / SHA-256 / 条目数
7. 反馈姿势：先确认两个 jar 都在，再贴 `logs\latest.log`

`RELEASE-NOTES.md` §2 顶部同时加了「⚠ 单装本 jar 必崩，报错就是上面那句，**不是 bug**」引述块（含完整 FATAL/ERROR 原文），§3 安装改成"推荐解压安装包"。

---

## 3. T3 三条文本的运行期取证结果

取证环境：生产专服 `_s8_srv`（NeoForge 21.1.264 + Java 21，mods 只有两个 release jar），通道 `tools\s5-rcon.ps1`（127.0.0.1:25575）。完整原文落在 `build\s8\t3-evidence.txt`。

| 键组 | 状态 | 运行期文本（原文） |
|------|------|--------------------|
| `command.raid.status` | ✅ **运行期验证** | `Overworld Raid Count: 1` / `- Raid #2` / `Status: ongoing` / `Center: [-319, 71, -288]` / `Bad omen level: 1` / `Waves: 1/5` / `Raiders: x5`；重启后同路径出现第二种状态 `Status: loss` |
| `tracker.lifetime.spawn_reason.trans_dimension` | ✅ **运行期验证** | `Trans-dimension (from Overworld): 1, (17.4/h) 100.0% [S]` |
| `tracker.lifetime.removal_reason.trans_dimension` | ✅ **运行期验证** | `Trans-dimension (to Nether): 1, (17.3/h) 16.7%` + `Minimum lifetime: 2 gt` / `Maximum lifetime: 2 gt` / `Average lifetime: 2.0000 gt` |
| `tracker.lifetime.removal_reason.death` | ✅ **运行期验证** | `Death (genericKill): 4, (69.0/h) 66.7%` + `Minimum lifetime: 921 gt` / `Maximum lifetime: 976 gt` / `Average lifetime: 943.2500 gt` |
| `util.teleport_hint` | ⚠️ **仅静态验证（本轮不取证）** | RCON 只有纯文本，拿不到 hover tooltip；**明确标注"需客户端手动 hover，本轮不取证"**，不伪造。静态依据：`Messenger.java:307` `tr("teleport_hint", dest)`、`:317` `tr("teleport_hint.dimension")`，`en_us`/`zh_cn` 键值 diff = 0 |

### 3.1 raid 怎么造出来的（可复现）【命令输出】

```
/execute in minecraft:overworld run forceload add -336 -288 -304 -256
  Marked 9 chunks in Overworld from [-21, -18] to [-19, -16] to be force loaded
/effect give TISBot minecraft:bad_omen 400 0
  Applied effect Bad Omen to tisbot
/execute in minecraft:overworld run tp TISBot -320 90 -272
  Teleported tisbot to -319.500000, 90.000000, -271.500000
(等 10s，假玩家落到 y=72 站进村庄 bounds -> RaidManager 起袭击)
/raid list
  Overworld Raid Count: 1
  - Raid #2
    Status: ongoing
    ...
```

`/raid list full` 追加 5 个 pillager 实体坐标（`-293.0, 62.5, -319.1` 等）。**注意**：y 悬空不算进村庄，必须真的落到村庄范围内，否则 `/raid list` 一直 `No raid exists`。

### 3.2 跨维度 + 死亡怎么造出来的（可复现）【命令输出】

```
/execute in minecraft:overworld run fill 100 70 100 101 72 100 minecraft:nether_portal
  Successfully filled 6 block(s)
/execute in minecraft:overworld run summon minecraft:zombie 100.5 70.0 100.5 {Tags:["s8probe2"]}
  Summoned new Zombie
(等 20s：非玩家实体自己穿门)
/execute in minecraft:the_nether run data get entity @e[tag=s8probe2,limit=1] Pos
  Zombie has the following entity data: [11.5d, 70.0d, 12.5d]     ← 地狱侧 1/8 缩放坐标
/execute in minecraft:overworld run kill @e[type=minecraft:zombie]
  Killed 5 entities                                                ← genericKill 死因

/lifetime zombie spawning
Nether (minecraft:the_nether)
Spawn Count: 1, (17.4/h)
Reasons for spawning
  Trans-dimension (from Overworld): 1, (17.4/h) 100.0% [S]

/lifetime zombie removal
Overworld (minecraft:overworld)
Removal Count: 6, (103.6/h)
Reasons for removal
  Death (genericKill): 4, (69.0/h) 66.7%
  Trans-dimension (to Nether): 1, (17.3/h) 16.7%
  Randomly despawn: 1, (17.3/h) 16.7%
```

### 3.3 走不通的路径（留档，避免下一轮重复浪费）

1. `/execute in the_nether run tp @e[type=zombie,...]` → `No entity was found`；改成 `as @e[...] in the_nether run tp @s` 会回 `Teleported Zombie to 0.5,70,0.5` 但**维度没变** —— 非玩家实体不吃 `tp` 换维，只有传送门 / `changeDimension` 才行。
2. `/lifetime player spawning|removal` 用 carpet 假玩家永远是 `No result yet` → 假玩家不进 lifetime 追踪。
3. `/execute in <dim> run data get entity @e[...]` 的 `in <dim>` **只改执行位置**，不改选择器作用域，用它判断实体在哪维度会误判。
4. `/say` 的广播文本 RCON 响应为空，不能用来做判定。

---

## 4. T4 HUD 归因证据（上一轮结论已证伪）

### 4.1 结论先行

**选 (B)：fork 与上游都没有"HUD 渲染调用点"这种东西。** carpet 的 HUD 不是屏幕叠加层，而是**玩家列表（Tab 面板）的表头/表尾**：`HUDController.update_hud` 把 `player_huds` 拼成 `Component`，包进原版 `ClientboundTabListPacket(header, footer)` 由服务端 `player.connection.send(...)` 下发，渲染交给原版 `PlayerTabOverlay`。所以"S7 真机看不到 HUD 浮层"= 上游行为，不是移植少了 mixin。

上一轮 `BLOCKERS.md` B-S7-2 的说法（"该 carpet fork 本身没有客户端 HUD 渲染入口 / 只有 Gui_tablistMixin 一个钩子"）**已被证伪并改写**。

### 4.2 `javap` 反编译 `carpet.logging.HUDController`【命令输出】

样本：`libs\carpet.jar`（fork，1498683 B，md5 `6A81DB10…`）解到 `build\s8\hud\fork`（716 个 class）；上游 `remapped.carpet-fabric-carpet-8f637a66-1.21-1.4.147+v240613.jar` 解到 `build\s8\hud\upstream`（715 个 class）。

```
PS> & 'C:\Program Files\Java\jdk-21.0.11\bin\javap.exe' -p -classpath build\s8\hud\fork carpet.logging.HUDController
public class carpet.logging.HUDController {
  public static void register(java.util.function.Consumer<net.minecraft.server.MinecraftServer>);
  public static void resetScarpetHUDs();
  public static void addMessage(net.minecraft.server.level.ServerPlayer, net.minecraft.network.chat.Component);
  public static void clearPlayer(net.minecraft.server.level.ServerPlayer);
  public static void update_hud(net.minecraft.server.MinecraftServer, java.util.List<net.minecraft.server.level.ServerPlayer>);
  private static void send_tps_display(net.minecraft.server.MinecraftServer);
  private static void send_counter_info(net.minecraft.server.MinecraftServer, java.lang.String);
  private static long packetCounter();
  ... (4 个 lambda: $lambda$update_hud$*, $lambda$send_tps_display$*)
  字段: HUDListeners / player_huds / scarpet_headers / scarpet_footers
}
```

- fork 与上游的**方法表 / 字段表完全一致**，都**没有任何 render 方法**（没有 `render*`、没有 `RenderGuiEvent`/`GuiGraphics` 相关签名）。
- class 字节 md5 不同（fork `FCBA45336CED3074DBCA828D03257B4C` / 上游 `3DF3F43AFE8215A8714E998B1DADCBD4`），差异来自常量池/编译器顺序，语义一致（方法集合与引用集合相同）。

### 4.3 谁调用 HUDController：字节扫描（两边同口径）【命令输出】

扫描方式（避免 PS 5.1 的两个坑：`ZipArchiveEntry.Open()` 二次读取返回 null → 先 `ExtractToFile` 落盘；`[Text.Encoding]::Latin1` 在 5.1 不存在 → 用 `GetEncoding(28591)`）：

| 扫描串 | fork（`libs\carpet.jar`） | 上游（`remapped...1.4.147+v240613.jar`） | 判定 |
|--------|--------------------------|------------------------------------------|------|
| `carpet/logging/HUDController` | `carpet/CarpetServer.class`、`carpet/logging/HUDController.class`、`carpet/logging/HUDLogger.class`、`carpet/script/external/Carpet.class` | **完全相同的 4 个类** | 一致 |
| `player_huds` | 只在 `carpet/logging/HUDController.class` | 相同 | 一致 |
| `net/minecraft/client/gui/Gui` | 只在 `carpet/mixins/Gui_tablistMixin.class` | 相同 | 一致 |
| `net/fabricmc` | **0 个类** | 6 个类（`CarpetServer`、`CarpetSettings`、`api/settings/SettingsManager`、`script/external/Carpet`、`script/external/Vanilla`、`utils/CarpetRulePrinter`） | 移植去 fabric 依赖的正常差异 |
| `HudRenderCallback` / `ClientTickEvents` / `ClientLifecycleEvents` | 0 | 0 | 排除 fabric 事件路径 |

**关键**：如果 fork 真的"掉了一条 HUD client mixin"，两边引用集合必然不同 —— 实际完全相同，所以不存在"少了一条"。

### 4.4 `carpet.mixins.json` client 段对比【文件实测】

两边都是 `"required": true`、`package carpet.mixins`、`compatibilityLevel JAVA_17`、`injectors.defaultRequire 1`。client 段 **14 条同名同集合**（仅书写顺序不同）：

```
ClientCommonPacketListenerImpl_customPacketMixin   ClientPacketListener_clientCommandMixin
ClientPacketListener_customPacketsMixin            DebugRenderer_scarpetRenderMixin
Gui_tablistMixin                                   LevelRenderer_creativeNoClipMixin
LevelRenderer_fogOffMixin                          LevelRenderer_scarpetRenderMixin
Minecraft_tickMixin                                MinecraftMixin
PistonHeadRenderer_movableBEMixin                  PlayerTabOverlayMixin
ShulkerBoxAccessMixin                              StructureBlockRenderer_mixin
```

唯一结构差异：上游末尾有 `"refmap": "fabric-carpet-refmap.json"`，fork 没有（NeoForge 移植去 refmap，见 RELEASE-NOTES §4）。14 条 client mixin 的 class 文件两边都存在（各自 md5 不同，属重编译产物）。

### 4.5 运行期佐证【日志原文】

S7 真机客户端日志 `build\shots\s7\client-latest.log`：

```
[2510月2026 16:45:02.220] [Render thread/INFO] [mixin/]: Mixing Gui_tablistMixin from carpet.mixins.json into net.minecraft.client.gui.Gui
[2510月2026 16:45:02.298] [Render thread/INFO] [mixin/]: Mixing PlayerTabOverlayMixin from carpet.mixins.json into net.minecraft.client.gui.components.PlayerTabOverlay
```

TIS 侧 4 条 mixin 同时打进 `carpet.logging.HUDController`（16:47:29 段）：`carpet.hooks.HUDControllerMixin`、`logger.mobcapsLocal.HUDControllerMixin`、`rule.HUDLoggerUpdateInterval.HUDControllerMixin`、`translations.HUDControllerMixin`；并有一条**cosmetic** WARN：`@Shadow ... carpet.hooks.HUDControllerMixin ... HUDListeners is final but shadow is not decorated with @Final`（仅提示，非 error）。

即：`Gui_tablistMixin` + `PlayerTabOverlayMixin` 都在应用，HUD 的接收端（Tab 渲染）齐活。

### 4.6 上游源码定案【文件实测】

`build\s8\HUDController.upstream.java`（从 `remapped.carpet-fabric-carpet-8f637a66-1.21-1.4.147+v240613-sources.jar` 抽出，5826 B）：

```java
:9    import net.minecraft.network.protocol.game.ClientboundTabListPacket;
:103  HUDListeners.forEach(l -> l.accept(server));
:105  Set<ServerPlayer> targets = new HashSet<>(player_huds.keySet());
:109  ClientboundTabListPacket packet = new ClientboundTabListPacket(
          scarpet_headers.getOrDefault(player.getScoreboardName(), Component.literal("")),
          Messenger.c(player_huds.getOrDefault(player, List.of()).toArray(new Object[0])));
:113  player.connection.send(packet);
```

→ 表头 = `scarpet_headers`，表尾 = `player_huds`（HUD 行）。**没有 `GuiGraphics`、没有任何叠加层绘制**。

### 4.7 排除过的位置与证据（为什么不是别的路径）

| 假设 | 排除证据 |
|------|----------|
| fabric 客户端渲染事件（`HudRenderCallback` / `ClientTickEvents` / `ClientLifecycleEvents`） | 两边 class 字节里 0 命中 |
| `Gui`/`InGameHud` 上的 HUD 渲染钩子 | `net/minecraft/client/gui/Gui` 只在 `Gui_tablistMixin` 出现；fork/上游都没有 `InGameHud` 引用 |
| TIS 自带叠加层渲染 | `src\main\java\carpettisaddition\logging\TISAdditionHUDController.java:35` 只是控制器（注册 listener），没有任何 `GuiGraphics`/绘制调用；TIS 的 4 条 HUDController mixin 也只挂 `HUDListeners.add(...)`（`carpet\hooks\HUDControllerMixin.java:45`） |
| fork 少了一条 client mixin（上一轮结论） | 4.3/4.4 双向同集合 + 4.5 两条 tab-list mixin 运行期都在应用 → 不成立 |
| `@Restriction` 把 HUD mixin 跳过了 | `[TISCM] Disabled mixin` 74 条全量清单里没有 `carpet.logging.HUDController` 相关条目；4 条 TIS 的 HUDController mixin 都记录了 `Mixing ... into carpet.logging.HUDController` |

### 4.8 若真要"屏幕浮层"（不在本轮范围，仅列方案）

那需要新写一条 client mixin 挂到 NeoForge 的 Gui 渲染事件（或 `Gui` 的 render 方法）上自绘 —— 属于**新增功能**，会动 `carpet-tis-addition.mixins.json`（S7 铁律禁止）。本轮按 (B) 收，`RELEASE-NOTES.md` §5.6 已改成"Tab 面板"表述。

---

## 5. T5 真专服验收数据

### 5.1 环境【命令输出】

- 目录 `_s8_srv`；`neoforge-21.1.264-installer.jar`（6992411 B，从 `maven.neoforged.net` 下）→ `java -jar ... --installServer` → `The server installed successfully`
- `mods\` 只有两个 **release jar**（不是 dev 环境）：

```
carpet-1.4.147+v1.0.8-251027.jar                      1498683 B
carpet-tis-addition-1.82.0+neoforge.1.21.1.jar        1804524 B
```

- `server.properties`：`enable-rcon=true` / `rcon.port=25575` / `rcon.password=tis-accept` / `online-mode=false` / `level-seed=20261025` / `difficulty=normal` / `view-distance=6` / `spawn-protection=0`；`user_jvm_args.txt` = `-Xmx3G`；`eula=true`
- 启动命令：`java -Xmx3G @libraries/net/neoforged/neoforge/21.1.264/win_args.txt nogui`

### 5.2 四条采集【日志原文】

| 采集项 | 结果 | 证据 |
|--------|------|------|
| 启动到 Done | ✅ `[Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (31.760s)! For help, type "help"`；第二轮 `Done (10.579s)!` | `build\s8\srv-with-carpet-latest.log` |
| applied 数（T1 脚本） | **461**（456 唯一，跑过 raid/lifetime）；短跑重启那轮 **443**（438 唯一） | `& .\tools\s8-count.ps1 -Log '_s8_srv\logs\debug.log'` |
| Disabled 数 | **72**（客 74；差额是两条 client-only `@Restriction(<1.16)`） | 同上 + `Select-String '[TISCM] Disabled mixin'` |
| `Mixin apply failed` / `Critical injection failure` | **0 / 0**（`latest.log` 357 行 + `debug.log` 3447 行全文扫描） | `tools\s8-count.ps1` 第③行 = `0 / 0 / 0` |

Mods 列表原文：

```
Mod List:
  Carpet TIS Addition (NeoForge port) |carpet_tis_addition |1.82.0
  Carpet: NeoForged                   |carpet             |1.4.147+v1.0.8-251027
  Minecraft                           |minecraft          |1.21.1
  NeoForge                            |neoforge           |21.1.264
[mixin/]: Compatibility level set to JAVA_21
```

### 5.3 命令原文（≥5 条 TIS 命令 + `/carpet`）【命令输出】

完整 transcript：`build\s8\t5-commands.txt`。

```
########## /carpet
Current Carpet Mod settings:
Carpet Mod version: 1.4.147+v1.0.8-251027
Carpet TIS Addition version: 1.82.0
Browse categories:
[Porting] [survival] [logger] [experimental] [creative] [command] [dispenser] [TISCM Network Protocol] [CARPET MOD] [feature] [optimization] [scarpet] [client] [TNT] [bugfix] [TIS]

########## /lifetime
Lifetime Tracker
A tracker to track lifetime and spawn / removal reasons from all newly spawned and removed entities
Complete doc here

########## /raid list
Overworld Raid Count: 1
- Raid #2
  Status: loss
  Center: [-319, 71, -288]
  Bad omen level: 1
  Waves: 1/5
  Raiders: x3
    [P] [P] [P]

########## /raid list full
Overworld Raid Count: 1
- Raid #2
  Status: loss
  Center: [-319, 71, -288]
  Bad omen level: 1
  Waves: 1/5
  Raiders: x3
  - Pillager @ [-295.0, 71.0, -282.8]
  - Pillager @ [-317.6, 62.9, -318.5]
  - Pillager @ [-288.9, 70.0, -297.6]

########## /sleep 1 s
(执行成功，无回显)

########## /lifetime zombie spawning
Lifetime Tracker has not started          ← 重启后 tracking 状态不持久，属预期

########## /lifetime zombie removal
Lifetime Tracker has not started

########## /list
There are 0 of a max of 10 players online:
```

（`/lifetime zombie spawning|removal` 的**有数据**版本见 §3.2，那是同一台服务器第一轮 tracking 打开后的输出。）

### 5.4 删掉 carpet jar 再启动：报错与用户遇到的一致【日志原文】

`build\s8\nocarpet-latest.log`（7026 B）：

```
[2510月2026 17:46:59.117] [main/FATAL] [net.neoforged.fml.ModLoader/CORE]: Error during pre-loading phase: Mod carpet_tis_addition requires carpet 1.4.147 or above
Currently, carpet is not installed
[2510月2026 17:46:59.446] [main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Mod carpet_tis_addition requires carpet 1.4.147 or above
	  Currently, carpet is not installed
```

与用户首次装机看到的报错**逐字一致**（`requires carpet 1.4.147 or above` + `Currently, carpet is not installed`），已原样写进 `dist\README.txt` 第 4 节与 `RELEASE-NOTES.md` §2 警告块。测完 `carpet-1.4.147+v1.0.8-251027.jar` 已放回 `mods\`，第二轮启动复现 `Done (10.579s)!`。

### 5.5 验收判定

| 标准 | 结果 |
|------|------|
| Done 出现 | ✅ 两轮都有（`31.760s` / `10.579s`） |
| apply failed = 0 | ✅ `latest.log` + `debug.log` 全扫为 0 |
| ERROR = 0 | ✅ `Select-String 'ERROR\|FATAL'` 计数 0；`s8-count` 第③行 `0 / 0 / 0` |
| 数据与 T1 脚本口径一致 | ✅ 全部数字都出自 `tools\s8-count.ps1`（本报告 §1.2 / §5.2 同源）；差异（461 vs 443 vs 453 vs 464）已用"运行期计数随类加载增长 + SERVER/CLIENT 环境差"解释，并在 §6.2 标明专服只在 `debug.log` 有 `Mixing` 行 |

---

## 6. 遗留 / INCOMPLETE

| 项 | 状态 | 缺什么 |
|----|------|--------|
| `util.teleport_hint` 运行期取证 | **INCOMPLETE**（本轮按约定不取证） | 缺一次真机客户端 hover 截图（`build\shots\s8\` 暂无）。RCON 通道在原理上取不到 tooltip 结构，不算"没做"，是**通道能力边界** |
| 客户端侧 HUD 面板（屏幕浮层） | 不是缺陷 | 归因已定案（§4）：carpet HUD = Tab 表头/表尾，两边同构 |
| 客户端真机重验收（S8 轮） | 未做 | 本轮 T5 只要求专服；客户端结论沿用 S7 真机日志 + 本轮静态复核 |
