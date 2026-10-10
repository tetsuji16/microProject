# ベータ前テスト

ベータでは機能変更に応じたunit/headless回帰を残し、代表的な利用経路だけを実Robot GUI smokeで確認する。GUI smokeは21メソッド・9クラスに限定し、`forkEvery=1`によるSwing状態の分離を保つ。Gradleのクラスfile includeで対象クラスだけを探索し、JUnit filterで指定メソッドだけを実行する。既存unit testは削除せず、GUI workerに誤って混在するクラス探索と不要なJVM起動を抑える。

| 層 | 実行範囲 | 主な確認 |
| --- | --- | --- |
| PR/push | 全module unit/headless、architecture・script checks、GUI smoke | 個別変更の回帰と代表的な物理操作 |
| ベータ候補 | PR相当の検証にGUI smokeを必須化 | 表計算入力、Task追加/削除、Gantt編集、Ganttバー端の期間変更とバー間依存link、Ribbon Link/Unlink、Undo/Redo・MPO reload、CCPM適用/Undo/Redo・MPO reload |
| 週次監査 | GUI full suiteとlocale/DPI matrix | smoke外のGUIクラス、翻訳と高DPI表示 |
| release | 全unit/headless、配布物build/import検証、GUI smoke | 配布物と主要操作の最終回帰 |

Coverageの境界: GUI smokeは21メソッド・9クラス（`forkEvery=1`の最大9クラスworker候補）。Task追加は物理popup Insertでの複数行挿入、Undo/Redo後の新規行に対する物理キー入力とMPO reloadまで確認する。削除はRibbon経路で削除保存後のMPO再読込とUndo/Redoを確認する。全UI unit suiteは約171のテストクラスを従来どおり実行し、GUI smokeのために間引かない。full GUI suiteは全GUI acceptance classを個別JVMで実行する。週次fullはベータsmokeの代替ではなく、広い画面・locale検証を補う。

Gantt操作のRobot証拠はベータsmokeに実バー端ドラッグ、バー間依存link、物理vertical-wheel scrollを含める。zoom/horizontal-scrollはRobot旅程が未整備で、headless fixtureの範囲を明示して補う。`GanttWheelZoomTest.ctrlWheelKeepsTheCursorDateAnchored`と`plainWheelOnSynchronizedGanttAdvancesOnlyOneOwnerStep`は合成wheel eventをGanttへdispatchし、scale/viewportと同期paneへのwheel ownerを検証する。`ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored`は4000px幅のscaled componentとviewport初期x=200を使い、表示中の左端日付がorigin変更後も保たれることを検証する。これらはheadless UI/component fixtureであり、物理デスクトップ入力によるzoomまたは水平 scrollbar 操作の証拠ではない。
