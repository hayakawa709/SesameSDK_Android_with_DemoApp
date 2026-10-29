# SESAME ホーム画面ウィジェット

## 2×1・状態表示版（dev3、2026-10-29）

今回の依頼に合わせて既存Providerを2×1へ変更しました。更新APKは `3.0.266-homewidget-dev3`。同一applicationId・debug署名で上書きできます。**既存1×1ウィジェットの占有セル数はLauncherが保持するため、APK更新後はウィジェットだけを削除し「SESAME Widget Dev → SESAME 2×1」から追加し直してください。アプリ・鍵を削除する必要はありません。**

- 左: ロック／アンロックの南京錠アイコン。不明時は中立の鍵アイコン。
- 右: 鍵名、状態文字、操作中・結果の補足。鍵名が長い場合は省略。
- SDKの `CHDeviceStatus.Locked` / `Unlocked` をそのまま表示し、それ以外は「状態不明」。操作の方向や成功応答から状態を反転・推測しません。
- ロック状態はBLEで取得します。クラウドからの操作成功応答は、現在の機械状態の情報として流用しません。BLE範囲外ではクラウド操作可能でも「状態不明」になります。
- BLEスナップショットにObserverを登録し、機械状態・接続・切断の通知から表示を更新します。操作中メッセージは状態更新と独立して維持し、結果メッセージは8秒後に通常の補足へ戻します。
- 配置完了時に既存connectedDevice Foreground Serviceを開始し、有効なホームウィジェットがある間はBluetooth監視を継続します。この間は監視通知を表示し、従来より電池を消費する可能性があります。最後の有効なウィジェットを削除すると、既存の通知ウィジェット・自動解錠・実行中コマンドの必要がない限りサービスを停止します。
- サービス再起動は監視の再開だけで、過去のtoggleは再送しません。OSの強制停止・Bluetooth権限・バックグラウンド制限・通信条件によって更新が止まる可能性があります。表示は最後にSDKから得た状態で、機械状態の継続的な保証ではありません。監視の再開は通常アプリ起動またはユーザーのウィジェットタップで行えます。タップはtoggleを伴います。
- コールドスタート時の鍵名は既存の名前保存領域を参照し、新たな鍵情報の複製はしません。

追加・変更: `HomeWidgetLockState.kt`、`HomeWidgetLockStateTest.kt`、Provider、Configure Activity、SesameAppのObserver、ConnectedDeviceServiceの監視ライフサイクル、2×1レイアウトとProviderInfo、状態アイコン2種、3言語文字列、versionName。本書より下は旧版の実装・検証記録です。

dev3検証: Debug APK生成成功、JVMテスト8件成功・失敗0、Lintエラー0・警告61、`git diff --check` 成功。2×1配置・表示・実機での状態追従は、今回端末未接続のため未検証です。

実機では再配置後に鍵名・2×1サイズ・表示切れを確認してください。立ち会いで、手動の施解錠やウィジェット操作後に状態表示が追従すること、Bluetooth切断後に「状態不明」になることを確認してください。BLEが戻れば実際に受信した状態に更新されます。こちらから鍵操作は実行していません。

サイズ指定の参照: https://developer.android.com/develop/ui/views/appwidgets/layouts

## 実機での追加確認と表示名修正（2026-10-29）

ユーザーがNothing Phone上で鍵を選択し、ホーム画面の「自宅」と表示された四角いウィジェットから、**通常Activityを開かず実際の鍵が動く**ことを確認しました。こちらから実際の鍵操作は実行していません。ADBでもNothing LauncherへのProvider配置を確認しました。以下の初回作業結果にある「実機未検証」は初回APK納品時点の記録です。クラウド経路・再起動・異常系等は引き続き未検証です。

日本語の `values-ja/Sesame` がDebugの既定言語ラベルを上書きし、開発版のアプリアイコンまで「セサミ」と表示されていました。Debug Manifestに専用の翻訳されないラベル `SESAME Widget Dev` を指定して修正しました。更新APKのversionNameは `3.0.266-homewidget-dev2`、applicationIdと署名は初回APKと同一です。既存インストールへの上書き更新に対応します。設定・鍵の削除は不要です。ウィジェット操作の受信と応答を、UUIDや鍵情報を含めず `SesameHomeWidget` タグで記録します。

丸いアプリアイコンは通常画面を開きます。鍵操作には、白い鍵マーク・デバイス名・操作状況が表示された四角いホームウィジェットを使用してください。

今回の追加変更は `sesameApp/src/debug/AndroidManifest.xml`、`sesameApp/src/debug/res/values/strings.xml`、`sesameApp/build.gradle.kts`、`SesameConnectedDeviceService.kt`、本書です。

## 作業結果（2026-10-29）

- 公式 `master` の基準コミット: `56d2e922d5577ea0499326df11df8270df266c07`
- 作業ブランチ: `feature/sesame-home-widget`。コミット・pushは未実施。
- `sesameApp:assembleDebug`、`sesameApp:testDebugUnitTest`、`sesameApp:lintDebug` 成功。
- JVMテスト6件成功、失敗0件。Lintエラー0件、警告63件。
- 生成APKのパッケージ・SDK値とv2署名を検証済み。
- Nothing Phone (3)、Nothing Launcher、BLE実機、クラウドの実行試験は未実施。ADB接続端末および設定済みAVDがありません。
- 実機での配置・操作を含む最終達成条件は、まだ確認できていません。実際の施解錠コマンドは送信していません。

## 実装

標準 `AppWidgetProvider` / `RemoteViews` を使用。1×1指定、ホーム画面専用、リサイズ無効、定期更新なし。白い鍵マークと暗い背景・境界線により明暗壁紙で判別できる構成です。鍵アイコンは状態を表しません。

専用Configure Activityは初期結果を `RESULT_CANCELED` とし、既存 `restoreSaved()` で復元した機器を選択させます。設定対象のProviderを検証し、選択時に対象がまだ有効か確認します。UUID保存と初回描画の後、widget ID付き `RESULT_OK` を返します。通常Activityへ転送しません。キャンセル時の配置破棄はAndroidのWidget Hostが行います。Nothing Launcherでの実際の動作確認は必要です。

対応機器は `CHSesame2` / `CHSesame5` 実装です。両者は `lock` / `unlock` / `toggle` に対応しています。Bike、Bot、Hub、生体認証機器などは選択対象外です。登録鍵がない場合は案内だけを表示します。

`home_widgets` SharedPreferencesはwidget IDごとのUUIDだけを保存します。名前は既存BLE Controllerから取得します。削除は該当IDだけを除去し、AndroidのID再割り当て通知にも対応します。`onEnabled` / `onDisabled` 専用の常駐処理は追加していません。

タップのPendingIntentは明示的な非公開 `SesameConnectedDeviceService` を直接指定します。API 26以上は `getForegroundService`、24–25は `getService`。widget ID、固有data URI、`FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` で分離します。

サービスでは以下を検証します。

1. ホームウィジェット専用actionであること。
2. widget IDが現在のProviderに所属すること。
3. 保存済みUUIDとIntentのUUIDが一致すること。
4. operationが `toggle` であること。
5. 端末がロック中でないこと。
6. 復元した機器が対応機種であること。
7. コマンド送信直前にもwidgetの存在・設定・機種を再確認すること。

ホームウィジェット経路は既存のUUIDなし一括操作分岐に入りません。秘密鍵・認証トークンはIntentやウィジェット設定に追加しません。

既存サービスは即座にForeground化します。操作予約数を同期的に確保し、鍵の非同期復元やWeb側のリスト同期に伴ってサービスが途中終了しないようにしています。鍵復元、既存BLEスキャン開始、バックグラウンドWeb起動、最大4秒のBLE接続待ちの後、既存 `app.ble.quickCommand(uuid, "toggle")` を1回だけ呼び出します。BLEログイン済みなら既存 `home.command`、それ以外なら既存 `widgetCommand` クラウド経路を使用します。方向の独自判定やBLEプロトコル追加はありません。`sesameSdk` は変更していません。

操作全体は30秒でタイムアウト。自動再試行・反対方向への操作は行いません。失敗・時間切れのログは一般的なメッセージのみで、デバイスUUID・秘密鍵・トークン・クラウド応答本文は記録しません。

同一UUIDの重複操作は別ウィジェットからでも抑止します。実行中の35秒ガードと、終了後3秒のクールダウンを `home_widget_commands` にUUID・時刻だけで保存します。プロセス終了でもガードは残ります。異なるUUIDは独立です。ガード保存に失敗するとコマンドを送信しません。OSの強制停止解除やプロセス再起動時にコマンドを自動再送しません。

表示は「未設定」「タップで操作」「操作中」「コマンド応答あり」「失敗・要確認」「時間切れ・要確認」。応答ありは機械的な施錠完了を意味しません。通信やプロセス終了で応答が失われても、鍵側でコマンドが実行される可能性があるため、失敗・時間切れ後は現物を確認してください。再描画や再起動後は待機表示に戻り、過去の施錠状態を推測しません。

## ビルド

JDK 21、Android SDK Platform 36、Build Tools 36.0.0、インターネット接続を使用しました。Java互換17、minSdk24、targetSdk36です。依存関係は付属Gradle wrapperで取得します。SDK 36はGradle実行時に環境へ導入されました。

PowerShellでリポジトリ直下から実行します。

```powershell
$env:ANDROID_HOME = 'C:\Users\User\AppData\Local\Android\Sdk'
.\gradlew.bat :sesameApp:assembleDebug :sesameApp:testDebugUnitTest :sesameApp:lintDebug -PhomeWidgetWebOrigin=https://biz.candyhouse.co --console=plain
```

`ANDROID_HOME` は各環境のSDKディレクトリに変更してください。`local.properties` の `sdk.dir` でも指定できます。

生成先: `sesameApp/build/outputs/apk/debug/sesameApp-debug.apk`

- applicationId: `co.candyhouse.sesame2.homewidgetdev`
- 表示名: `SESAME Widget Dev`
- versionName: `3.0.266-homewidget-dev`
- versionCode: `910`
- ローカルdebug署名。配布用の公式署名ではありません。

引数を省略した `assembleDebug` は従来どおり `app.properties` の `candyhouse.sesame.web.dev`（現在 `http://localhost:3000`）を使います。今回の納品APKは上記引数で **本番Web URL** を指定して生成しました。`app.properties` 自体は変更していません。今回のチェックアウトのRelease/CI URLも本番です。貼り付け指示書にあるステージングURLとは異なります。

ローカルBiz3を利用する場合は、そのソースと開発サーバーが別途必要です。起動後 `adb reverse tcp:3000 tcp:3000` を実行します。APKにWeb画面資源は同梱されないため、初回はオンラインで起動してください。

## Firebase・認証・Play版との共存

公式公開ソースの `sesameApp/google-services.json` は `your-project-id` 等を含むサンプルです。既存ファイルを上書きしたり、新たなダミー認証情報を作ったりしていません。

この開発用DebugビルドではGoogle Services処理、アプリからのCrashlytics呼び出し、FCMトークン取得を無効にしています。プッシュ通知は動作しません。実際のFirebaseプロジェクトで開発用applicationIdを登録し、対応clientを含む正規設定を用意した場合のみ、`-PhomeWidgetFirebase=true` で有効化できます。Release/CIのFirebase設定・署名は別途正規のものが必要です。Release/CI APKは今回生成していません。

AWS Cognitoの公開設定はそのまま使用しています。Firebaseを無効にしてもCognito、Web側ログインやクラウド認証を代用したことにはなりません。

Play版をアンインストール・上書きしないでください。開発版はパッケージが異なるため並存しますが、Room DB、WebViewのログインセッション、Bluetooth等の権限は共有されません。端末上のPlay版から秘密鍵を抽出しません。

BLEを利用するための手順:

1. 開発版を起動し、必要なBluetooth・位置情報権限を許可し、Bluetoothを有効にする。
2. 開発版の通常のログイン・アカウント同期、または公式の鍵共有・受け取りフローで既存の鍵を追加する。既存の鍵を共有する場合、ロックの再登録・初期化は不要です。
3. 開発版のデバイス一覧に対象が現れ、ローカルへ鍵が保存されることを確認する。
4. Nothing Launcherからウィジェットを追加し、その機器を選択する。

完全な鍵のBLE操作は既存SDKで可能です。制限付き鍵の認証には既存の署名サービスが必要です。現在のSDKはAndroid 12以降も `ACCESS_FINE_LOCATION` を確認します。必要に応じてOSの位置情報設定も確認してください。

クラウド経路の追加条件:

- 指定したWeb URLへアクセス可能であること。
- 開発版側のWebViewで正規のログインを完了し、セッションが有効であること。
- バックグラウンドWebの `home.devices` による準備が30秒以内に進むこと。
- 対象機器のクラウド操作権限と、対応するオンラインのゲートウェイ等があること。
- パッケージ名・署名が変わった開発版からもWeb側・Cognito等が正常に認証できること。

開発版でのログインや鍵共有画面、クラウド接続の実動作は未検証です。Web側の設定や正規の認証設定が追加で必要になる可能性があります。認証回避は実装していません。

## インストールと使用

端末でAPKを開き、開発版をインストールします。またはUSBデバッグを有効にして以下を実行します。

```powershell
adb devices
adb install .\sesameApp\build\outputs\apk\debug\sesameApp-debug.apk
```

開発版の更新では同じdebug署名を使って `adb install -r ...` を実行します。Play版のアンインストールは不要です。別PCのdebug署名とは互換性がない場合があります。

ホーム画面を長押し → ウィジェット → **SESAME Widget Dev / SESAME 1×1** → 配置 → 鍵を選択。ウィジェット全体のタップで対象1台のtoggleを要求します。短時間の連打は無視されます。失敗後は必要な権限、鍵、通信、ログイン、現物を確認してください。対象を変えるには削除して再配置します。

ホーム画面専用で、ロック画面利用や認証回避機能はありません。Androidの設定画面からの「強制停止」は通常のプロセス終了とは異なり、再度アプリを手動起動するまでウィジェット操作等が制限される可能性があります。

## 実施した検証

| 検証 | 結果 |
| --- | --- |
| Debug APKビルド | 成功 |
| UUID照合、未設定、別対象、不正operationのJVMテスト | 成功 |
| 削除済みwidget・再設定後の古いIntentの検証方針のJVMテスト | 成功 |
| 同一鍵の連打、別ウィジェット、別鍵、完了後クールダウン | 成功 |
| 永続化を模したガードの再生成、保存失敗時の抑止 | 成功 |
| 全JVMテスト | 6件、失敗0、エラー0 |
| Android Lint | エラー0、警告63。レポート参照 |
| `git diff --check` | 成功 |
| APKのapplicationId / minSdk / targetSdk | 上記値をaaptで確認 |
| APK署名 | apksigner verify成功、v2署名 |
| 実機・エミュレータの計測テスト | 未実施 |

JVMテストは方針・時間制御の検証です。実際のSharedPreferences、AndroidのPendingIntent配信、Service起動、BLE・クラウド通信の成功を証明するものではありません。

Lintの警告には既存コードのものに加えて、新しいウィジェットのAPI 31用属性が旧OSで無視されること、小さい文字サイズ、KTX利用提案等が含まれます。1×1の実際のサイズ・文字切れ・拡大文字は実機調整の余地があります。

## 実機チェックリスト（すべて未検証）

実際の施解錠は所有者の立ち会いで、鍵の状態を確認できる状況で実施してください。テストのために無断で鍵を初期化・削除・登録解除しないでください。

1. Androidバージョン、Nothing OS / Launcherバージョンを記録する。
2. ウィジェット一覧表示、1×1配置、専用設定画面、選択後の残存、配置キャンセル、未登録時の案内を確認する。
3. 明暗テーマ、文字サイズ変更、表示名の省略、Nothing Launcher再起動、端末再起動後の設定を確認する。
4. 実際の鍵を確認しながら施錠→タップ→解錠、解錠→タップ→施錠を検証する。通常Activityが開かず、toggleが1回だけ送られることを確認する。
5. アプリをバックグラウンドへ移し、同じ操作を確認する。
6. OSによるプロセス終了の代替として `adb shell am kill co.candyhouse.sesame2.homewidgetdev` 後にタップする。`am force-stop` と区別する。
7. BLEログイン済み、コールドスタートからのBLE再接続、BLE到達範囲外からのクラウド操作を個別に確認する。
8. 連打、操作中のタップ、同じ鍵の別ウィジェットからのタップが追加操作にならないことを確認する。
9. 2台用にA/Bを配置し、AがBを操作せず、A削除後にもBの設定が残ることを確認する。
10. Bluetooth無効、通信なし、権限取り消し、鍵なし・通常の鍵削除後、クラウド認証失効、タイムアウトを検証する。失敗表示が成功と混同されず、自動再試行がないことを確認する。
11. 実行中にプロセス終了・widget削除した場合、UIやSDKコールバックだけで実際の機械状態を断定しないことを確認する。
12. ロック画面から配置・操作できず、端末ロック中の要求を拒否することを確認する。

ログ取得例: `adb logcat -s SesameHomeWidget SesameWidget`

Android 12以降のウィジェット操作によるFGS起動例外と、Android 14以降のconnectedDevice要件を参照し、既存type・権限を維持しました。実際のOS/Launcherの制約は上記試験で確認が必要です。

- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device
- https://github.com/CANDY-HOUSE/SesameSDK_Android_with_DemoApp

## 変更ファイル

- `sesameApp/build.gradle.kts`: 共存用Debug設定、URL引数、Firebaseの明示的opt-in、JUnitテスト依存。
- `sesameApp/src/main/AndroidManifest.xml`: Provider・専用Activity登録。
- `sesameApp/src/main/java/co/candyhouse/app/connecteddevice/SesameConnectedDeviceService.kt`: 個別UUID検証、既存quickCommand連携、状態・タイムアウト・連打抑止。
- `sesameApp/src/main/java/co/candyhouse/app/SesameApp.kt`: Firebase無効時の起動対応。
- `sesameApp/src/main/java/co/candyhouse/app/push/TopicSubscriptionManager.kt`: Firebase無効時のFCM取得抑止。
- `sesameApp/src/main/java/co/candyhouse/app/homewidget/HomeWidgetStore.kt`: UUID保存、対応機種判定。
- `sesameApp/src/main/java/co/candyhouse/app/homewidget/SesameHomeWidgetProvider.kt`: 描画・削除・復元・PendingIntent。
- `sesameApp/src/main/java/co/candyhouse/app/homewidget/SesameHomeWidgetConfigureActivity.kt`: 鍵選択と配置完了。
- `sesameApp/src/main/java/co/candyhouse/app/homewidget/WidgetCommandGate.kt`: 永続化可能なUUID別の連打抑止。
- `sesameApp/src/main/java/co/candyhouse/app/homewidget/WidgetCommandPolicy.kt`: widgetとUUIDの照合規則。
- `sesameApp/src/main/res/layout/sesame_home_widget.xml`: 1×1表示。
- `sesameApp/src/main/res/xml/sesame_home_widget_info.xml`: Widget定義。
- `sesameApp/src/main/res/drawable/home_widget_background.xml`: 背景。
- `sesameApp/src/main/res/drawable/home_widget_key.xml`: 中立の鍵アイコン。
- `sesameApp/src/main/res/values/home_widget_strings.xml`: 英語文字列。
- `sesameApp/src/main/res/values-ja/home_widget_strings.xml`: 日本語文字列。
- `sesameApp/src/main/res/values-zh/home_widget_strings.xml`: 中国語文字列。
- `sesameApp/src/debug/res/values/strings.xml`: 開発版ラベル。
- `sesameApp/src/debug/res/xml/network_security_config.xml`: localhostのincludeSubdomains属性を明示。
- `sesameApp/src/test/java/co/candyhouse/app/homewidget/WidgetCommandGateTest.kt`: 連打・永続化のJVMテスト。
- `sesameApp/src/test/java/co/candyhouse/app/homewidget/WidgetCommandPolicyTest.kt`: UUID・widget検証のJVMテスト。
- `docs/SESAME_HOME_WIDGET.md`: 本書。

依存ライブラリの追加はテスト用JUnitのみです。既存の環境ファイル・認証情報・Play版データは変更していません。
