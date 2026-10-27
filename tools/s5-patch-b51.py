# -*- coding: utf-8 -*-
"""S5 收尾补丁：把 B-S5-1 的"运行期才暴露"事实补进 PORT-STATE §13.4 与 BLOCKERS B-S5-1。"""
import io, sys

def patch(path, old, new, must=1):
    s = io.open(path, encoding='utf-8').read()
    n = s.count(old)
    if n != must:
        print('FAIL %s: anchor count=%d (expect %d)' % (path, n, must))
        return False
    s = s.replace(old, new)
    io.open(path, 'w', encoding='utf-8', newline='').write(s)
    print('OK   %s (+%d bytes)' % (path, len(new) - len(old)))
    return True

ok = True

# ---- PORT-STATE §13.4：补一条"运行期才暴露"的证据 ----
old1 = '- `run\\logs\\latest.log`：`[*/ERROR]` 9 条 + `[FATAL]` 1 条'
new1 = ('- **B-S5-1 的运行期形态**（`_run_s5.log:1016`）：`InjectionError: Critical injection failure: Argument modifier method '
        'registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; … (0/1) succeeded. Scanned 0 target(s). No refMap loaded.` —— '
        '`require=1` 下 mixin 按设计抛异常，栈顶在 `MinecraftServer.lambda$spin$2`（服务器 tick 线程），'
        '即 `/player Steve spawn` 触发 `GameProtocols.<clinit>` 时才炸。**S4 的 `_run_s4_final.log` 里这一串是 0 次，不是已通过，而是那轮 `GameProtocols` 从未被加载、注入器从未执行**；S5 用真实用例把它挖了出来。\n'
        '- `run\\logs\\latest.log`：`[*/ERROR]` 9 条 + `[FATAL]` 1 条')
ok &= patch('PORT-STATE.md', old1, new1)

# ---- BLOCKERS B-S5-1：补运行时形态 + 为何 S4 没发现 ----
old2 = '- 影响链：'
new2 = ('- 运行时形态（`_run_s5.log:1016`，`require=1` 下按设计抛异常而非只打日志）：\n'
        '  ```\n'
        '  Caused by: org.spongepowered.asm.mixin.injection.throwables.InjectionError: Critical injection failure: Argument\n'
        '  modifier method registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; in carpet-tis-addition.mixins.json:network.\n'
        '  CustomPayloadC2SPacketMixin from mod carpet_tis_addition failed injection check, (0/1) succeeded. Scanned 0 target(s).\n'
        '  No refMap loaded.\n'
        '    at ...MinecraftServer.lambda$spin$2(MinecraftServer.java:267)\n'
        '  ```\n'
        '- 为什么 S4 的四个致命串计数是 0：S4 的验收只跑到 `Done` 就退出，全程无玩家/payload 交互，'
        '`GameProtocols`（`CustomPacketPayload.codec` 的宿主类）从未被加载，该 mixin 的注入器从未执行 → 缺陷处于潜伏态。'
        'S5 发 `/player Steve spawn` 后 `GameProtocols.<clinit>` 首次触发，注入器第一次执行即 0 目标。**S4 的 0 = 未触发，不是通过。**\n'
        '- 影响链：')
ok &= patch('BLOCKERS.md', old2, new2)

sys.exit(0 if ok else 1)
