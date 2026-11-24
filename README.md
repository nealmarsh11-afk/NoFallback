# NoFallback

一个用于 **MIUI / HyperOS** 的 Xposed 模块：让系统不再把热点频段**强制改写回 2.4GHz**。它不改变你选的频段，也不预设必须是 5GHz。

使用的框架：**Vector**（兼容经典 Xposed API 93+），LSPosed 亦可。

---

## 它解决什么问题

5GHz 热点本身是可用的：即使 Wi-Fi 处于关闭状态，系统也能以 5GHz 启动 SoftAP。

问题出在启动前的那道「频段可用性判定」。系统会取一次 Wi-Fi 芯片当前可见的信道列表，与请求频段求交集；当 Wi-Fi 未开启、未关联，或扫描结果为空时，这个列表取不到，判定即失败。失败之后系统不只是拒绝这一次请求——它会把持久化配置里的频段**改写为 2.4GHz**，然后按 2.4GHz 重建热点。

于是用户看到的现象是：

- 在设置里选了「5.0 GHz 频段」，重开热点又回到 2.4GHz；
- 热点日志里出现 `No suitable 5GHz channel corresponding to the band.`。

## 原理

在 `system_server` 中 hook 系统 Wi-Fi 服务的频段判定方法：

```
com.android.server.wifi.MiuiWifiService.isSapSupportedBand(int) : boolean
com.android.server.wifi.AmlMiuiWifiServiceImp.isSapSupportedBand(int) : boolean
```

直接返回 `true`，跳过「扫描到的信道是否匹配」这一步。上层拿到 `true` 后按**你请求的频段**继续启动 SoftAP；请求的是 5GHz 就在 5GHz 起，请求的是 2.4GHz 仍在 2.4GHz 起——模块只取消那次判定，不改频段。

目标类位于 `/system_ext/framework/miui-wifi-service.jar`，由独立 `ClassLoader` 加载，不在 `system_server` 的 `PathClassLoader` 里。因此模块通过 hook `com.android.server.SystemServiceManager.startServiceFromJar`，在该调用发生时取到关联的 `ClassLoader`，再装载钩子（`ServiceJarHook`）。

## 测试环境

本模块在以下设备上验证通过：

| 项目 | 值 |
|---|---|
| 机型 | Redmi Note 11T Pro+（小米） |
| 型号 | 22041216C |
| 代号 | xaga |
| 系统 | HyperOS 2.0.12.0.ULOCNXM（Android 14 / SDK 34） |
| Root | Magisk 31.1 + Zygisk Next 1.4.5 |
| 框架 | Vector 2.2 (3080) |

## 构建

需要 JDK、Android SDK build-tools、aapt2、d8、zipalign、apksigner。

```bash
./build.sh
```

脚本会编译 `src/`（编译期符号来自 `stubs/`，运行时由框架提供），打包 `classes.dex` 与 `assets/xposed_init`，签名输出 `NoFallback-1.2.apk`。

## 许可

MIT

模块在清单里用 `xposedscope` 声明了默认作用域（`android`，即系统框架 / `system_server`）。LSPosed 安装后会据此预置作用域；若所用版本仍显示为空作用域，手动勾选「系统框架」一项即可。
