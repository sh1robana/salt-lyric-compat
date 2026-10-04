# v0.1.0 — FLAC 歌词时间戳兼容（公开测试版）

解决部分 FLAC 内嵌歌词使用冒号时间标记，而播放器未识别这种写法，导致时间标记直接显示、歌词无法同步的问题。例如，将 `[01:02:61]` 在读取时转换为 `[01:02.61]`。不修改音乐文件，使用时无需联网。

## 修复前后的效果

**修复前：时间标记直接显示在歌词正文中。**

![修复前的歌词显示效果](https://raw.githubusercontent.com/sh1robana/salt-lyric-compat/main/docs/images/lyric-before.png)

**修复后：当前歌词恢复高亮，并显示译文。**

![修复后的歌词显示效果](https://raw.githubusercontent.com/sh1robana/salt-lyric-compat/main/docs/images/lyric-after.png)

以上为同一首歌的截图；播放时的同步滚动已在 Windows 1.18.5 中验证。

## 下载与安装（Windows）

已验证 **Salt Player for Windows 1.18.5**。

1. [下载模组安装 ZIP](https://github.com/sh1robana/salt-lyric-compat/releases/download/v0.1.0/plugin-local.salt.lyriccompat-0.1.0.zip)，保留 ZIP 文件，不要解压。
2. 打开播放器，进入 **设置 → 创意工坊 → 模组管理**。
3. 点击右上角的 **导入**图标，选择下载的 `plugin-local.salt.lyriccompat-0.1.0.zip`。
4. 找到 **FLAC Lyric Timestamp Compatibility**（v0.1.0 在播放器中显示的名称），点击该行右侧向下箭头，选择 **启用**并确认。状态应显示 **已启动（STARTED）**。
5. 先切换到另一首歌，再切回问题歌曲，查看歌词是否随音乐高亮和滚动。如果提示重启，重启后再播放。

**请勿导入带 source 的 ZIP 或“源码”（Source code），它们是开发用的源文件。普通用户无需运行任何 `.ps1` 脚本。** SPMOD 附件与安装 ZIP 的内容相同，供支持该扩展名的导入器使用；在已验证的 1.18.5 中请选择 ZIP。

如果已安装早期本地试包，可继续使用；如果导入时提示同 ID 已存在，先禁用并删除旧模组，再导入发布包。遇到问题可在模组管理中禁用，重新打开歌曲后恢复默认处理。

## 验证与限制

- Windows 1.18.5 实测：问题歌曲恢复同步；正常原文及翻译正常；禁用后重现原问题；重启继续生效。
- 605 首 FLAC 及边界测试：624 项断言通过，43 首可转换，3,664 个时间标记。
- 仅处理符合模式的 FLAC 内嵌歌词，不处理 MP3、WAV 等其他音频格式，也不能解决所有歌词显示问题。
- 其他 Windows 版本及 Linux 版未验证；当前安装包不适用于安卓。与其他歌词来源模组共用需单独验证。

[使用说明](https://github.com/sh1robana/salt-lyric-compat#readme) · [反馈问题](https://github.com/sh1robana/salt-lyric-compat/issues)

作者：**[sh1robana](https://github.com/sh1robana) 和 [Codex](https://github.com/codex)**。许可证：MIT。
