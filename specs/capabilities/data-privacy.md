# 隐私与数据处置规范 / 高频陷阱清单

> 用法：改动清单、存储、锁、清除数据之前，对照本文件。
> 本文只管"数据能不能离开设备、能不能被看见"。日期语义在 `local-data-model.md`，产品承诺在 `PRODUCT.md`，决策背景在 `docs/adr/0002`。
> **本文件的每一条都由 `scripts/check-privacy-guards.py` 在提交时强制检查（L1），除标注 ⚠️ L3 的条目。**

## 1. 网络（最高频 —— 务必先看）

- ✅ `AndroidManifest.xml` 不声明任何联网相关权限（`INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`、`CHANGE_NETWORK_STATE`、`ACCESS_FINE/COARSE_LOCATION`）。
  - ❌ 声明 `INTERNET` → 应用立刻具备外传健康数据的能力，产品承诺变成一句空话（脚本会直接 fail）。
- ✅ 不引入任何具备上报能力的 SDK（崩溃收集、分析、广告、A/B 实验）。
  - ❌ 引入任一者 → 即使不主动调用，也可能默认开启遥测。
- ✅ 新增任何依赖前检查其 manifest 是否自带 `INTERNET`（部分 HTTP 客户端会合并权限）。
  - ❌ 只删自己的权限声明，忽略库合并进来的 → 最终 APK 仍有联网权限。

## 2. 备份与快照

- ✅ `android:allowBackup="false"`，且不配置 `dataExtractionRules` / `fullBackupContent` 白名单。
  - ❌ 允许备份 → 健康数据随系统自动备份到云端，绕过"零网络"承诺。
- ✅ 应用锁开启时，Activity 窗口加 `FLAG_SECURE`。
  - ❌ 不加 → 最近任务列表缩略图直接暴露日历上的经期标记。
- ✅ 锁屏界面不显示任何日期或症状内容（只显示"输入 PIN"）。
  - ❌ "为你显示 3 月 12 日的记录" → 未解锁即泄漏。

## 3. 日志与异常

- ✅ `app/src/main` 内**完全不使用** `android.util.Log`、`println`、`System.out`、`printStackTrace`。
  - ❌ `Log.d(TAG, "$date $flow")` → logcat 任何本机应用可读（Android 10 前无需权限）。
- ✅ 调试靠调试器断点，不靠日志。
  - ❌ "临时加个日志回头删" → 回头往往不会删；脚本会拦住。
- ✅ 异常消息可含类型与状态码，不含日期、症状、PIN。
  - ❌ `error("bad pin: $pin")` → 异常消息会出现在崩溃对话框/系统日志中。

## 4. 凭据（应用锁）

- ✅ PIN 只存 `PBKDF2WithHmacSHA256` 的哈希 + 每设备随机 salt + 迭代次数，存于 DataStore。
  - ❌ 明文 / Base64 / MD5 / 可逆加密 → 拿到设备文件即可还原 PIN。
- ✅ salt 由 `SecureRandom` 生成，长度 ≥ 16 字节；迭代次数 ≥ 120000。
  - ❌ 固定 salt → 彩虹表可用；迭代次数过低 → 暴力破解成本低。
- ✅ 每次解锁用**常量时间比较**（`MessageDigest.isEqual`）。
  - ❌ `hashA == hashB` → 理论上的时序侧信道。
- ✅ 连续 5 次失败后锁定 30 秒；失败次数不持久化（重启即重置，避免把自己锁死）。
  - ❌ 把锁定状态持久化 → 用户重启后仍被锁，且没有恢复途径。
- ✅ 遗忘 PIN 的唯一恢复路径是**清除数据**，UI 必须明示后果。
  - ❌ 预留"万能码"或密钥托管 → 后门就是漏洞。
- ⚠️ L3（未自动化）：PIN 不做生物识别替代通道，也不做"记住 N 天"；这条靠人工 review。

## 5. 数据主权

- ✅ "清除全部数据"必须是真删除：经期日、症状条目、锁设置一次清空，无软删除、无回收站。
  - ❌ 打 `deleted=true` 标记 → 数据仍在设备上，与"清除"的字面承诺不符。
- ✅ 清除前二次确认，文案含"不可恢复"。
- ✅ 用户输入决定一切：不做基于麦克风/位置/通讯录的任何推断。
- ⚠️ L3（未自动化）：不申请任何敏感权限（相机、麦克风、通讯录、位置、身体传感器）。

## 提交前快速自检

- [ ] 本次改动没有让任何健康数据出现在日志、异常、Intent extra、通知文案中
- [ ] 清单权限集合与上一版**完全一致**（新增任何权限都要走 Grill 路径）
- [ ] `allowBackup=false` 未被改动
- [ ] 锁相关改动仍用 PBKDF2 + SecureRandom salt + 常量时间比较
- [ ] 清除数据仍然覆盖所有表与设置
- [ ] `python scripts/check-privacy-guards.py` 输出 PASS
