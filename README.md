# 猫猫物语 (NekoTale)

<div align="center">

**极简灵动 · 温暖纯净 · 贴心守护**

一款轻量、极简且注重隐私体验的 Android 定制客户端。

</div>

---

## 🌟 特性概览

- 🐱 **温暖纯净**：精心打磨的极简视觉语言，去除冗余视觉干扰与商业广告。
- ⚡ **原生体验**：深度适配 Android 11 ~ 15 与现代移动设备，流畅丝滑。
- 🔒 **隐私至上**：不采集通讯录与多余设备敏感权限，数据自主掌控。
- 🛠️ **多协议网络引擎**：内置可插拔轻量核心，支持灵活的网络接入与自适应路由。
- 🌏 **原生中文**：离线内置全量简体中文语言包，开箱即用。

---

## 🚀 编译与构建

### 环境要求

- JDK 21+
- Android SDK (API 34+) & NDK (r27+)
- Go 1.22+

### 本地编译

```bash
# 1. 编译核心依赖
cd core && go build -o ../TMessagesProj/src/main/jniLibs/arm64-v8a/libnekocore.so . && cd ..

# 2. 构建 Release APK
./gradlew assembleRelease
```

---

## 📄 开源许可与致谢

- 本项目基于开源 Telegram for Android 及开源社区组件深度定制优化。
- 遵循 GNU General Public License (GPL) 规范。
