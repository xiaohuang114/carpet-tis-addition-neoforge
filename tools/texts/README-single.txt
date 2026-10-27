Carpet TIS Addition (NeoForge port) 1.82.0 —— 单文件版安装包
================================================================

这个包和"三 jar 版"（dist\TIS-Addition-NeoForge-1.21.1.zip）是**同一个 mod 的两种包装**，
功能完全一样，只是本 mod 的 jar 里额外塞了两个库（JarJar 嵌套），所以你可以少拷两个文件。

--------------------------------------------------------------
一、怎么装（只有两步）
--------------------------------------------------------------
1. 解压本包，得到 mods 目录，里面正好 2 个 jar：

     mods\carpet-1.4.147+v1.0.8-251027.jar                    （前置，必需）
     mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar      （本 mod，已内含两个库）

2. 把这两个 jar 一起放进 .minecraft\mods（服务端放同名目录）。启动即可。

就这么多。不需要判断 conditional-mixin 放不放，也不需要额外拷什么。

--------------------------------------------------------------
二、里面那两个库是什么（它们已经在这个 jar 内部）
--------------------------------------------------------------
  conditional-mixin-neoforge-0.6.4.jar   modId conditional_mixin，LGPL-3.0-only
  tiny-mappings-parser-0.3.0+build.17.jar modId tiny_mappings_parser_lib，Apache-2.0

启动日志里会看到它们是被"嵌套加载"的（不是散在 mods 里）：

  Found mod file "conditional-mixin-neoforge-0.6.4.jar" [parent: carpet-tis-addition-...jar, locator: jarinjar]
  Found mod file "tiny-mappings-parser-0.3.0+build.17.jar" [parent: carpet-tis-addition-...jar, locator: jarinjar]

NeoForge 会把它们当**独立模块**加载，所以不会出现"两个 mod 导出同一个包"的
java.lang.module.ResolutionException（这正是 S9 修掉的那个崩溃）。

--------------------------------------------------------------
三、整合包里已经有 conditional-mixin 怎么办
--------------------------------------------------------------
什么都不用做。实测（NeoForge 21.1.264）：外部那份（例如 TweakerMore 自带的 jar-in-jar）
满足条件时，NeoForge 会自动跳过 jar 内嵌的那份，日志里出现一行 WARN 而不是报错：

  [JarSelector/]: Attempted to select a dependency jar for JarJar which was passed in as source:
  conditional_mixin. Using Mod File: ...\mods\conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar

结果：Done，无 Duplicate mods、无 ResolutionException。

--------------------------------------------------------------
四、注意
--------------------------------------------------------------
* 本包与"三 jar 版"是同一个 mod（modId carpet_tis_addition、同一个版本号）。
  两个包**不要混用**：主 jar 同名，混拷会变成两份同名 mod 文件。
  已经从三 jar 版升级过来的话，把 mods 里那两个额外 jar 删掉，换成这一个即可。
* 必需环境：Minecraft 1.21.1 + NeoForge 21.1.x + Java 21。
* 启动器显示：PCL2 读 mod 名字只认 META-INF/mods.toml（不认 neoforge.mods.toml），
  本版 jar 里已补了这份兼容文件，所以在 PCL2 列表里显示为
  "Carpet TIS Addition (NeoForge port)" 而不是文件名；图标仍是 PCL2 的默认立方体
  （它只从 CurseForge / Modrinth 拉图标，不读 jar 内 logoFile）。
  刷新后仍是文件名的话，重启一次 PCL2（它缓存 mod 列表）。
* 缺 carpet 前置时的报错（原文）：
    Mod carpet_tis_addition requires carpet 1.4.147 or above
    Currently, carpet is not installed
* 许可：本 mod LGPL-3.0；conditional-mixin LGPL-3.0-only；tiny-mappings-parser Apache-2.0；
  carpet（chililisoup 的 NeoForge fork）MIT。详见三 jar 版包内的 THIRD-PARTY-NOTICES.txt。
