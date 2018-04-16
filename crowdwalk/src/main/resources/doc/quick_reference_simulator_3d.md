# クイック・リファレンス

## 3D シミュレーションビューア(3D シミュレータ)の起動方法

### 1. コマンドラインから直接起動する方法

カレントディレクトリを CrowdWalk/crowdwalk として `sh quickstart.sh <プロパティファイル> -g` または `sh quickstart.sh <プロパティファイル> --gui` を実行します。  
<br />
例)
~~~
[user@hostname ~/CrowdWalk/crowdwalk]$ sh quickstart.sh sample/basic-sample/properties.json -g
~~~

### 2. マップエディタから起動する方法

マップエディタの `3D Simulate` ボタンをクリックすると 3D シミュレーションビューアが起動します。  
<br />
<small>※ シミュレーションビューアが起動すると`2D Simulate`及び`3D Simulate`ボタンは無効になりますが、シミュレーションビューアを閉じた後マップファイルまたはプロパティファイルを読み込むと再び有効になります。</small>

## メニュー

|---|---|
| File ||
| Close (Ctrl + W)  | シミュレーションビューアを終了してウィンドウを閉じる |
| Help ||
| Quick reference   | クイック・リファレンスを表示する |
| About version     | CrowdWalk のバージョン(Git のコミット情報)を表示する |

## シミュレーション画面

|---|---|
| マウス操作 ||
| 左クリックでドラッグ          | 画面の中央を基準にしてマップを回転する |
| Shift + 左クリックでドラッグ  | 画面の中央を基準にして角度制限なしでマップを回転する |
| 右クリックでドラッグ          | マップをスクロールする |
| ホイール                      | マップを拡大・縮小する |
| パーツを選択して左クリック    | Status タブにパーツの状態を表示する |

※リプレイモードではパーツの状態表示以外の操作は無効となります。

## スタートボタンパネル

|---|---|
| ![Start](jar:file:./build/libs/crowdwalk.jar!/img/start.png)　ボタン  | シミュレーションを開始する |
| ![Pause](jar:file:./build/libs/crowdwalk.jar!/img/pause.png)　ボタン  | シミュレーションを一時停止する |
| ![Step](jar:file:./build/libs/crowdwalk.jar!/img/step.png)　ボタン    | シミュレーションを1ステップ(1秒)進める |
| wait スクロールバー                                                   | シミュレーションのステップ毎の待ち時間(ミリ秒)を設定する |

## Control タブ

|---|---|
| シナリオ ||
| enabled           | イベントを発生状態にする |
| disabled          | イベントが発生していない状態にする |
| auto              | 時間通りにイベントを発生させる |
| Pause ||
| time              | 時刻を設定してシミュレーションを一時停止する。23:59:59 まで設定可能 |
| elapsed           | 経過時間(秒)を設定してシミュレーションを一時停止する。最大3日分の秒数まで設定可能 |
| disabled          | 一時停止機能を無効にする |
| 設定フィールド    | シミュレーションを一時停止させる時刻(または経過時間)を設定する。値を直接キー入力した場合は最後に Enter が必要 |
| Reset ||
| Reset ボタン      | シミュレーションビューアを再起動する |

## View タブ

|---|---|
| Vertical scale                            | 垂直方向の表示スケールを設定する |
| Agent size スクロールバー                 | エージェントの直径(m)を設定する |
| Show nodes                                | ノードを表示する |
| Show links                                | リンクを表示する |
| Show links at actual width                | 実際の道幅でリンクを表示する
| Show areas                                | 災害エリアを表示する |
| Show outline of areas                     | 災害エリアの全範囲のアウトラインを表示する
| Show agents                               | エージェントを表示する |
| Show polygons                             | ポリゴンを表示する |
| Show the sea                              | 海面を表示する |
| Show background map                       | 背景地図を表示する |
| Record simulation screen                  | シミュレーションのスクリーンショットを記録する |
| Change agent color depending on speed     | 移動速度に応じてエージェントの表示色を変更する |
| Show status                               | シミュレーション画面上にステータスラインを表示する |
| Show logo                                 | AIST ロゴを表示する |
| Exit with simulation finished             | シミュレーション終了と共にウィンドウを閉じる |
| Add centering margin                      | Centering with scaling を実行する際に余白を付加する |
| Centering ボタン                          | 回転をリセットしてマップの中央を表示する(リプレイモードでは無効) |
| Centering with scaling ボタン             | 回転をリセットして全体が画面に収まるスケールでマップの中央を表示する(リプレイモードでは無効) |

## Camera タブ

|---|---|
| Replay チェックボックス   | リプレイモードにする |
| Record ボタン             | 現在のシミュレーション内時刻と視点を記録する |
| Load ボタン               | シミュレーション内時刻と視点が保存されたファイルを読み込む |
| Save as ボタン            | 記録したシミュレーション内時刻と視点をファイルに保存する |

## Status タブ

|---|---|
| Mode ラジオボタン | 状態表示をおこなうパーツの種類を選択する |
