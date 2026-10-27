Carpet TIS Addition (NeoForge port) 1.82.0
==========================================

Fabric 模组 Carpet TIS Addition 1.82.0 移植到 NeoForge 1.21.1 的非官方版本：
规则、命令、日志器与上游逐条对齐（587 条 mixin / 12 条 client mixin 与官方 jar 比对过），业务逻辑不动。

资产说明
--------
* TIS-Addition-NeoForge-1.21.1-single-jar.zip  —— 推荐。解压后只有 2 个 jar：
  carpet 前置 + 本模组（conditional-mixin 与 tiny-mappings-parser 已用 JarJar 内嵌）。
* TIS-Addition-NeoForge-1.21.1.zip            —— 三 jar 版（把两个库拆成独立 jar，方便整合包统一管理）。
* TIS-single-jarjar.jar                      —— 单文件版里的那个模组 jar 本身（给只想替换主 jar 的人）。

环境
----
Minecraft 1.21.1 / NeoForge 21.1.x / Java 21；
服务端与客户端都需要 carpet 1.4.147+（NeoForge 移植版）。

升级提示
--------
如果你的整合包在启动时报
  java.lang.module.ResolutionException: Modules carpet_tis_addition and conditional_mixin
  export package me.fallenbreath.conditionalmixin.api.mixin to module ...
说明用的是把 conditional-mixin 拍平进主 jar 的旧包装。本版已改为独立模块，删掉 mods 里所有旧的
carpet-tis-addition-*.jar 再放本版本即可。

验收
----
NeoForge 21.1.264 专服：Done、ERROR 0、apply failed 0、applied 437 条 mixin；
缺 conditional_mixin 时按预期报 "requires conditional_mixin 0.6.4 or above"。

许可
----
本移植版 LGPL-3.0；上游 Carpet TIS Addition by Fallen_Breath/TISUnion（LGPL-3.0）；
Carpet 上游 MIT，NeoForge 端口 by chililisoup MIT；conditional-mixin LGPL-3.0-only；
tiny-mappings-parser Apache-2.0。详见包内 THIRD-PARTY-NOTICES.txt。
