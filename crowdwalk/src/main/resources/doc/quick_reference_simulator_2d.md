# クイック・リファレンス

## 2D シミュレーションビューア(2D シミュレータ)の起動方法

### 1. コマンドラインから直接起動する方法

カレントディレクトリを CrowdWalk/crowdwalk として `sh quickstart.sh <プロパティファイル> -g2` または `sh quickstart.sh <プロパティファイル> --gui --2` を実行します。  
<br />
例)
~~~
[user@hostname ~/CrowdWalk/crowdwalk]$ sh quickstart.sh sample/basic-sample/properties.json -g2
~~~

### 2. マップエディタから起動する方法

マップエディタの `2D Simulate` ボタンをクリックすると 2D シミュレーションビューアが起動します。  

## メニュー

|---|---|
| File ||
| Close (Ctrl + W) | シミュレーションビューアを終了してウィンドウを閉じる |
| View ||
| Centering              | マップの中央を表示する(リプレイモードでは無効) |
| Centering with scaling | 全体が画面に収まるスケールでマップの中央を表示する(リプレイモードでは無効) |
| To the origin          | 原点座標(0, 0)を表示する(リプレイモードでは無効) |
| Show nodes             | ノードを表示する |
| (Show node labels)     | ノードラベルを表示する |
| Show links             | リンクを表示する |
| (Show link labels)     | リンクラベルを表示する |
| Show areas             | 災害エリアを表示する |
| (Show area labels)     | エリアラベルを表示する |
| Show background image  | グループ別に背景画像のON/OFFを切り替える |
| Help ||
| Quick reference | クイック・リファレンスを表示する |
| About version | CrowdWalk のバージョン(Git のコミット情報)を表示する |

## シミュレーション画面

|---|---|
| マウス操作 ||
| 右 / 左クリックでドラッグ  | 画面をスクロールする |
| ホイール                   | 表示を拡大・縮小する |
| Ctrl + ホイール | 表示を拡大・縮小する(1/10 ステップ) |
| Shift + ホイール | 表示を回転する |
| Ctrl + Shift + ホイール | 表示を回転する(1/10 ステップ) |
| パーツを選択して左クリック | Status タブにパーツの状態を表示する |

※リプレイモードではパーツの状態表示以外の操作は無効となります。

## スタートボタンパネル

|---|---|
| ![Start](jar:file:./build/libs/crowdwalk.jar!/img/start.png)　ボタン | シミュレーションを開始する |
| ![Pause](jar:file:./build/libs/crowdwalk.jar!/img/pause.png)　ボタン | シミュレーションを一時停止する |
| ![Step](jar:file:./build/libs/crowdwalk.jar!/img/step.png)　ボタン   | シミュレーションを1ステップ(1秒)進める |
| wait スクロールバー | シミュレーションのステップ毎の待ち時間(ミリ秒)を設定する |

## Control タブ

|---|---|
| シナリオ ||
| enabled  | イベントを発生状態にする |
| disabled | イベントが発生していない状態にする |
| auto     | 時間通りにイベントを発生させる |
| Pause ||
| time           | 時刻の設定によりシミュレーションを一時停止する。23:59:59 まで設定可能 |
| elapsed        | 経過時間(秒)の設定によりシミュレーションを一時停止する。最大3日分の秒数まで設定可能 |
| disabled       | 一時停止機能を無効にする |
| 設定フィールド | シミュレーションを一時停止させる時刻(または経過時間)を設定する。値を直接キー入力した場合は最後に Enter が必要 |
| Reset ||
| Reset ボタン   | シミュレーションビューアを再起動する |

## View タブ

|---|---|
| agent size スクロールバー               | エージェントの直径(pixel)を設定する |
| Show nodes                              | ノードを表示する |
| Show node labels                        | ノードラベルを表示する |
| Show links                              | リンクを表示する |
| Show link labels                        | リンクラベルを表示する |
| Show areas                              | 災害エリアを表示する |
| Show area labels                        | エリアラベルを表示する |
| outline                                 | 災害エリアの全範囲のアウトラインを表示する |
| Show agents                             | エージェントを表示する |
| Show agent labels                       | エージェントラベルを表示する |
| Show background image                   | 背景画像を表示する |
| Show background map                     | 背景地図を表示する |
| Show polygons                           | ポリゴンを表示する |
| Show the sea                            | 海面を表示する |
| Record simulation screen                | シミュレーションのスクリーンショットを記録する |
| Change agent color depending on speed   | 移動速度に応じてエージェントの表示色を変更する |
| Drawing agent by triage and speed order | 重要なエージェントほど上になる様に表示する |
| Show status                             | シミュレーション画面上にステータスラインを表示する |
| Show logo                               | AIST ロゴを表示する |
| View-calculation synchronized           | 表示の更新が完了するのを待ってから次のステップに進む |
| Exit with simulation finished           | シミュレーション終了と共にウィンドウを閉じる |
| Add centering margin                    | Centering with scaling を実行する際に余白を付加する |
| Centering ボタン                        | マップの中央を表示する(リプレイモードでは無効) |
| Centering with scaling ボタン           | 回転を初期化し、全体が画面に収まるスケールでマップの中央を表示する(リプレイモードでは無効) |
| To the origin ボタン                    | 原点座標(0, 0)を表示する(リプレイモードでは無効) |

## Camera タブ

|---|---|
| Replay チェックボックス | リプレイモードにする |
| Record ボタン           | 現在のシミュレーション内時刻と視点を記録する |
| Load ボタン             | シミュレーション内時刻と視点が保存されたファイルを読み込む |
| Save as ボタン          | 記録したシミュレーション内時刻と視点をファイルに保存する |

## Status タブ

|---|---|
| Mode ラジオボタン | 状態表示をおこなうパーツの種類を選択する |
