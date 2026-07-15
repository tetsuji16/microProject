# ProjectLibre 全体リファクタリング計画

最終更新: 2026-07-02

## Summary

- Maven のビルド定義は既に repo から除去済みで、正本は root `settings.gradle.kts` と `modules/*/build.gradle.kts` の Gradle マルチモジュール構成である。
- `pom.xml` は repo 内に存在せず、`settings.gradle.kts` では `projectlibre_contrib` / `projectlibre_core` / `projectlibre_application` / `projectlibre_ui` / `projectlibre_exchange` / `projectlibre_reports` の 6 サブプロジェクトを定義している。
- Gradle 移行そのものは完了済みであり、残タスクは legacy module 相当の撤去、すなわち vendored code・bridge jar・旧 packaging artifact の除去である。
- 目的は「独自実装・同梱 jar・同梱第三者ソース」を減らし、有名な Maven Central 配布モジュールと JDK 標準 API に寄せること。
- 既存の大量ワークツリー変更は巻き戻さず、`modules/` 配下の現行 Gradle 構成を正とする。
- 最初の実装単位は依存関係の整理に限定し、業務ロジックの巨大分割は test 追加後に段階実施する。
- `.pod` など既存ファイル形式の後方互換は破らない。

## Legacy Removal Scope

- 削除対象の「古いモジュール」は Gradle サブプロジェクトではない。`projectlibre_contrib` を含む 6 サブプロジェクトは現行構成として維持する。
- 今回の legacy 対象は次の 3 系統に限定して扱う。
- vendored code: `modules/projectlibre_exchange/src/main/java/net/sf/mpxj` と `modules/projectlibre_ui/src/main/java/org/pushingpixels` が主対象であり、`modules/projectlibre_ui/src/main/java/org/apache/batik` は ProjectLibre 名前空間への移設と旧ディレクトリ削除まで完了済み
- legacy binary payload: `modules/projectlibre_contrib/lib` に残る bridge jar。現時点の最重要は root `projectLibreLegacyBridgeJars` に残る `modules/projectlibre_contrib/lib/mpxj-10.11.0.jar` であり、JasperReports compiler 互換 jar は build classpath と repo 同梱 payload から除去済みである。
- packaging artifacts: `isolated-build/` 配下の旧配布成果物、旧 classpath script、現行 Gradle 配布フローの正本ではない補助物

## 直近の進捗

- `net.sf.mpxj.projectlibre` 由来の XLSX 補助クラスは `com.projectlibre1.exchange.xlsx` へ移設済み。
- `System.out/err` と `printStackTrace` の生存箇所は、`core` / `exchange` / `ui` / `reports` / `contrib` の現行 Java ソースから除去済み。
- `ResourceMappingForm`、`ProjectData`、`AssignmentData`、`IncrementalData`、`TaskData`、`SessionFactory` の raw collection を段階的に generics 化済み。
- `ProjectData` の public collection 境界を `DataObject` ベースに整理し、`Serializer` / `ServerLocalFileImporter` / `DataUtil` の参照を型付きキャストへ寄せ済み。
- `Serializer.buildStructure()` / `saveTasks()` / `serializeProject()` の boundary を typed collection / typed resource map に寄せ、project serialization の raw boundary をさらに縮小済み。
- `Serializer.deserializeProject()` の resource / task boundary を整理し、`enterpriseResources` 参照と `setEnterpriseResources()` の typed boundary を追加整備済み。
- `ResourcePool.getResourceList()` を `List<Resource>` に寄せ、`SpreadSheet` の resource selection boundary をそれに合わせて修正済み。
- `ResourcePool` の resource lookup / dirty propagation loops を typed iterator / for-each に整理済み。
- `TimesheetEntryPane` の selected resource boundary を `List<Resource>` に寄せ、timesheet resource resolution の raw object list を減らし済み。
- `ServerLocalFileImporter` の resource/task read boundary を `DataObject` 経由に寄せ、pod import の raw collection cast をさらに減らし済み。
- `MSPDISerializer` と `ProjectLibreXlsxWriter` の resource traversal を `Resource` ベースに寄せ、`ResourcePool.getResourceList()` 変更の波及を整理済み。
- `MutableNodeHierarchy` と `AssignmentFormat` の `getResourceList()` 参照を typed に寄せ、resource pool boundary をさらに整理済み。
- `MutableNodeHierarchy` の内部 traversal / removal helpers を generics 化し、hierarchy helper の raw collection をさらに削減済み。
- `AssignmentService` の assignment/resource traversal を typed iterator / typed `Set<Resource>` に寄せ、assignment creation path の raw collection をさらに削減済み。
- `HasAssignmentsImpl` の assignment traversal / rollup helpers を typed iterator に寄せ、assignment container の raw iterator をさらに削減済み。
- `TimeDistributedDataConsolidator` の cost/work/baseline helpers を typed collection 化し、time-distribution aggregation の raw iterator をさらに削減済み。
- `RenderingUtils` の desktop hint boundary を typed `Map` / typed iterator に寄せ、UI utility の raw map handling を削減済み。
- `GraphModel` の local search lists を typed `List<GraphicNode>` に寄せ、graph cache search の raw list handling を削減済み。
- `DataUtils` の node / iterator helpers を typed collection に寄せ、utility boundary の raw iterator をさらに削減済み。
- `ClassUtils` の raw `Class` / `Class[]` signatures を整理し、utility boundary の型安全性をさらに高め済み。
- `StartupFactory` の credentials / plugin opts boundary を typed map に寄せ、startup flow の raw map handling をさらに削減済み。
- `ResourceView` の child traversal を typed `List<Node>` に寄せ、resource view の raw child iteration をさらに削減済み。
- `ServerLocalFileImporter` の distributions / existing resources boundary を型付きに寄せ、余分な明示キャストを削減済み。
- `Serializer` の task / resource collection access を private typed helper に寄せ、repeated unchecked cast を局所化済み。
- `Project` に `getTaskList()` の typed bridge を追加し、`MSPDISerializer` と `ProjectLibreXlsxWriter` の task traversal をそれ経由に寄せ済み。
- `CriticalPath` / `ProjectMergeService` / `MutableNodeHierarchy` の task boundary を `Project.getTaskList()` へ寄せ済み。
- `Serializer` の resource / task 並び替えと task hierarchy 印字を typed helper 化し、`Collections.sort((List<?>)...)` と `TreeSet` への unsafe cast を削減済み。
- `CommonTable` の `Vector` 境界を typed helper に寄せ、`DefaultTableModel` 生成の raw cast を局所化済み。
- `ReferenceNodeModelCache` の child / edge access を generics 化し、UI cache の raw list 境界をさらに削減済み。
- `NodeModelCache` / `ViewNodeModelCache` / `VisibleElements` を typed return に寄せ、spreadsheet/cache boundary の raw list 境界をさらに削減済み。
- `ResourcePool.getChildrenResoures()` を `List<Resource>` に寄せ、resource tree boundary の raw list を削減済み。
- `SelectionNodeEvent` と `TaskSnapshotBackup` を typed boundary に寄せ、event / snapshot の raw collection 境界をさらに削減済み。
- `NodeList` を `ArrayList<Node>` に寄せ、`nodeListToImplList()` を typed helper に整理済み。
- `CommonTransform` / `NodeSorter` を typed collection に寄せ、transform parameter / subtransform boundary を整理済み。
- `Project.getRootTasks()` / `getRootResources()` を typed copy に寄せ、`NodeList` の戻り値変更波及を閉じた。
- `NodeModelUtil` の dump / extract / cache helper を generics 化し、grouping model の内部 raw collection をさらに削減済み。
- `MutableNodeHierarchy` の child traversal / search helper を generics 化し、hierarchy 内部の raw collection をさらに削減済み。
- `Serializer.markAncestorsOfDirtyTasksDirty()` も `Project.getTaskList()` へ寄せ、task bridge の利用をさらに広げ済み。
- `ResourcePool.userResources()` を typed for-each に寄せ、resource list traversal の raw iterator をさらに削減済み。
- `TimesheetEntryPane.resolveResources()` を explicit for-each に寄せ、resource list boundary を明示化済み。
- `Project.tasks` と `Project.getTasks()` を `Task` ベースに寄せ、task list boundary をより型付きに整理済み。
- `PredecessorTaskList` の subproject traversal を `Project.getTaskList()` へ寄せ、task bridge の利用をさらに広げ済み。
- `DependencyFormat` の container boundary を `Project.getTaskList()` へ寄せ、task list bridge をさらに活用済み。
- `Project.getTaskOutlineRoot()` の内部参照を `tasks` へ寄せ、task list access の raw getter 依存を少し削減済み。
- `AssignmentFormat` の container boundary を typed `Collection<Resource>` で維持し、resource list bridge を正式な API として使い続ける形に整理済み。
- `Project` の timesheet helper path を `getTaskList()` ベースへ寄せ、task bridge の内部利用をさらに広げ済み。
- `ResourcePool` の `projects` は `Vector` から `List<Project>` に置き換え済み。
- `YearlessDateInputParser` は内部実装を `java.time` ベースへ寄せ、年またぎと時刻付き入力の回帰テストを追加済み。
- `TimeInputParser` も `java.time` ベースへ寄せ、`ActionJList` / `ResourceAdditionDialog` / `ActionLists` の raw collection を整理済み。
- `CellStyles` も `ActionLists` と同様に generics 化済み。
- `Messages.getTipProperties()` を bundle 直読みへ寄せ、Tip ダイアログの初期化順依存を解消済み。
- `org.jdesktop.swing.calendar.DateSpan` をローカル実装へ置換し、`jdnc-0_7-all.jar` を bridge から削除済み。
- `com.jgoodies:jgoodies-forms` へ切り替え、`modules/projectlibre_ui/src/main/java/com/jgoodies/forms/builder/DefaultFormBuilder.java` に互換 shim を置いて旧 API を維持済み。
- `modules/projectlibre_contrib/lib/jcommon.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/jlfgr.jar` を bridge と ProGuard 入力から削除済み。
- `modules/projectlibre_contrib/lib/jgoodies-binding-2.13.0.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/jgoodies-common-1.8.1.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/org-openide-util-ui-RELEASE290.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/org-openide-util-RELEASE290.jar` と `modules/projectlibre_contrib/lib/org-openide-util-lookup-RELEASE290.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/exchange/rtfparserkit.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/l2fprod-common-totd.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/jdnc-0_7-all.jar` を bridge から削除済み。
- `modules/projectlibre_contrib/lib/flatlaf-3.7.1.jar` と `modules/projectlibre_contrib/lib/flatlaf-extras-3.7.1.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/org-netbeans-swing-outline-RELEASE290.jar` を Maven 依存へ置換し、bridge から削除済み。
- `modules/projectlibre_contrib/lib/commons-collections4-4.4.jar`、`commons-lang3-3.14.0.jar`、`jackson-annotations-2.16.1.jar`、`jackson-core-2.16.1.jar`、`jackson-databind-2.16.1.jar`、`pdfbox-3.0.1.jar`、`pdfbox-io-3.0.1.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/jdnc-0_7-all.jar`、`jgoodies-binding-2.13.0.jar`、`jgoodies-common-1.8.1.jar`、`jlfgr.jar`、`l2fprod-common-totd.jar`、`org-openide-util-lookup-RELEASE290.jar`、`org-openide-util-RELEASE290.jar`、`org-openide-util-ui-RELEASE290.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/itext.jar` を `com.lowagie:itext:2.1.7` へ置換し、bridge から削除済み。
- `modules/projectlibre_contrib/lib/jcommon.jar` と `modules/projectlibre_contrib/lib/jfreechart.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/nachocalendar.jar` を `net.sf.nachocalendar:nachocalendar:0.25` へ置換し、bridge から削除済み。
- `modules/projectlibre_contrib/lib/radiance-substance-1.0.2.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/radiance-flamingo-1.0.2.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/flamingo-6.2.jar` を同梱実体から削除済み。
- `modules/projectlibre_contrib/lib/radiance-neon-1.0.2.jar`、`modules/projectlibre_contrib/lib/radiance-trident-1.0.2.jar`、`modules/projectlibre_contrib/lib/trident-6.2.jar` を Maven 依存へ置換し、bridge から削除済み。
- `modules/projectlibre_contrib/lib/jasperreports/jasperreports.jar` を bridge から外し、互換版の `net.sf.jasperreports:jasperreports:6.21.5` Maven 依存へ置換済み。
- `net.sf.jasperreports:jasperreports:6.21.5` への切り替えに合わせ、`net.sf.jasperreports.compilers.JRBshCompiler` を `JRJavacCompiler` ベースの互換 shim へ更新済み。
- `projectlibre_reports` の `ReportAdapter` / `DataSourceProvider` を JasperReports 7 系 API に追随させ、`JRDesignTextElement` への font property 直接設定と group-name ベースの setter へ移行済み。
- `ReportUtil` は bundled JRXML を JasperReports 6.21.5 にそのまま渡す形へ戻し、過剰な attribute/band 変換をやめたうえで XML loader warning のみ抑制する形に整理済み。
- 外部 Radiance 優先の方針は確定し、`radiance-neon` / `radiance-trident` の namespace 差分は local shim で吸収する前提に整理中。
- `org.pushingpixels.neon.icon.ResizableIcon` / `org.pushingpixels.trident.Timeline` / `org.pushingpixels.trident.swing.SwingRepaintCallback` の shim を追加済みで、` :projectlibre_ui:compileJava` と ribbon/icon 回帰テストは通過済み。
- Flamingo 側の icon 境界は `org.pushingpixels.neon.api.icon.NeonIcon` 前提へ寄せ、旧 `neon.icon` 参照は shim の互換面に限定済み。
- `ActionJList` の未使用 `Vector` コンストラクタを削除済み。
- `CommonTable` の `Vector` コンストラクタは型付きシグネチャを維持しつつ、内部実装を明示的キャストへ整理済み。
- `ReferenceNodeModelCache` の内部 raw collection を部分的に generics 化済み。
- `TransformComboBoxModel` と `TransformParameterDialog` の raw collection を generics 化済み。
- `ViewTransformer` と `TransformList` の内部 collection を generics 化済み。
- `ProjectFactory` の保存/クローズ用ローカル collection を `List<Project>` 化済み。
- `NodeCacheTransformer` の内部 collection と assignment キャッシュを generics 化済み。
- `AssociationList` の内部 `LinkedList` と関連ローカル collection を generics 化済み。
- `NamedList` の内部 `List` を `List<Object>` に整理済み。
- `FieldDictionary` の内部 `LinkedList` / `HashMap` と公開 getter を generics 化済み。
- `:projectlibre_core:compileJava` と `:projectlibre_ui:compileJava` で上記の generics 化を確認済み。
- `TimeSpreadSheetModel` の field / interval コレクションを generics 化済み。
- `ClassUtils` の comparator registry を null-safe helper 付きの generics 化へ整理済み。
- `Field` の comparator まわりを `Comparator<Object>` ベースに寄せ、`findAllInCollection` などの内部 collection も generics 化済み。
- `ClassLoaderUtils`、`DataSourceProvider`、`OSXAdapter` の残存デバッグ出力を logger 経由へ置換済み。
- `ReportAdapter` の残存 `System.out.println` を logger へ置換済み。
- `ReportUtil` の JRXML 読込失敗時の記録を logger へ寄せ済み。
- `ReportUtil` の古いコメント付きサンプル実装を削除済み。
- `ProjectWriterUtility` を `Supplier` ベースへ寄せ、writer 生成の反射依存を除去済み。
- `ProjectLibreXlsxWriter` の file 出力と文字列変換を `try-with-resources` / `StandardCharsets` ベースへ整理済み。
- `MPXConverter.dateToXMLString()` を `java.time` ベースへ置換済み。
- `CollaborationMetadataStore` の sidecar JSON を手書き parser/writer から Jackson へ置換し、metadata round-trip test を追加済み。
- 未使用の `XlsxPayloadUtils` を削除し、exchange の object stream 補助を 1 つ整理済み。
- `mpxj-10.11.0.jar` の除去と `net.sf.mpxj:mpxj:13.0.1` への一時置換を試行したが、forked `net.sf.mpxj` ソースが `DateHelper` などの旧 API に依存しており未完了のため、10.11 bridge は現状維持と確認済み。
- `Portfolio` の raw comparator と一時 `ArrayList` を typed に寄せ、remove project の boundary を少し整理済み。
- `AssociationComparator` を `Comparator<Association>` に寄せ、association sort boundary の raw comparator を 1 つ削減済み。
- `WorkComparator` を `Comparator<Object>` に寄せ、`Dictionary` の named item map / comparator も typed 化済み。
- `:projectlibre_core:compileJava`、` :projectlibre_exchange:compileJava`、` :projectlibre_ui:compileJava`、` :projectlibre_core:test`、` :projectlibre_exchange:test`、` :projectlibre_ui:test` が通過済み。
- `:projectlibre_core:compileJava`、`:projectlibre_contrib:compileJava`、`:projectlibre_reports:compileJava` が通過済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `jcommon.jar` 非依存化を確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `jlfgr.jar` 非依存化も確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `jgoodies-binding` と `org-openide-util-ui` 非依存化も確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `jgoodies-common` 非依存化も確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `org-openide-util` 系の非依存化も確認済み。
- `:projectlibre_ui:compileJava` で `ActionJList` / `CommonTable` の UI 境界整理を確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `rtfparserkit` 非依存化も確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `l2fprod-common-totd` 非依存化も確認済み。
- `:projectlibre_contrib:dependencies --configuration runtimeClasspath` で `jdnc-0_7-all` 非依存化も確認済み。
- `:projectlibre_ui:compileJava` で `com.jgoodies:jgoodies-forms` + `DefaultFormBuilder` shim の互換性を確認済み。
- `:projectlibre_contrib:test --tests com.projectlibre1.contrib.calendar.ContribIntervalsTest` で `DateSpan` / interval merge の回帰を確認済み。
- `:projectlibre_ui:compileJava` で `TimeSpreadSheetModel` の境界整理を確認済み。

## 修正が必要な内容リスト

- 削除対象の「古いモジュール」は Gradle サブプロジェクトではなく、vendored code・bridge jar・旧 packaging artifact を指す。`projectlibre_contrib` 自体は現行 Gradle サブプロジェクトであり削除対象ではない。
- repo 内に `pom.xml` は存在せず、Gradle 側では `settings.gradle.kts` で `projectlibre_contrib` / `projectlibre_core` / `projectlibre_application` / `projectlibre_ui` / `projectlibre_exchange` / `projectlibre_reports` の 6 サブプロジェクトを定義済みである。この構成を正本として維持する。
- 旧 Maven モジュールという表現は使わず、削除対象は「Maven Central 化されるべき vendored code と bridge artifact」で統一する。
- `modules/projectlibre_contrib/lib` の同梱 jar を段階削除する。優先順の最上位は root `build.gradle.kts` の `projectLibreLegacyBridgeJars` に残る `modules/projectlibre_contrib/lib/mpxj-10.11.0.jar` である。
- `modules/projectlibre_exchange/src/main/java/net/sf/mpxj` の同梱 MPXJ ソースを削除し、`net.sf.mpxj:mpxj:13.0.1` を利用する。
- `modules/projectlibre_ui/src/main/java/org/pushingpixels` の同梱 Flamingo/Radiance 系ソースを Maven 依存へ移す。まず現行 API 互換の `org.pushing-pixels:radiance-flamingo` 系に寄せ、`neon` / `trident` の namespace 差分は local shim で吸収する。
- `modules/projectlibre_ui/src/main/java/org/apache/batik/util/gui/resource` の UI factory 独自流用は廃止済みで、同等コードは `com.projectlibre1.menu.resource` へ移設した。`modules/projectlibre_ui/src/main/java/org/apache/batik` は repo から削除済みである。
- root `build.gradle.kts` に `projectLibreLegacyBridgeJars` として残る `modules/projectlibre_contrib/lib/mpxj-10.11.0.jar` は、完全 Gradle 移行の最重要未完了 bridge として扱う。
- `modules/projectlibre_contrib/build.gradle.kts` の `legacyBridgeJars` と `reportsBridgeJars` はどちらも空である。`jdt-compiler.jar` は削除済みで、JasperReports 本体は Maven 依存、Java compiler 側は transitive `org.eclipse.jdt:ecj` に移行済みである。
- `projectlibre_reports:test --tests com.projectlibre1.reports.adapter.ReportUtilTest` は、JasperReports 本体の Maven 化後も bundled `projectDetails.jrxml` を 6.21.5 loader で読めることを確認済みである。
- `isolated-build/` 配下の成果物と `isolated-build/projectlibre_ui/scripts/projectlibre_ui(.bat)` の旧 classpath 列挙は、現行 Gradle 配布フローの正本ではない legacy packaging artifact として扱う。既定方針は「repo 正本ではないため削除候補」である。
- root `build.gradle.kts` に `cleanLegacyPackagingArtifacts` タスクを追加し、`isolated-build/` を Gradle から明示的に掃除できるようにした。`scripts/run_projectlibre.ps1 -Clean` もこの cleanup を呼ぶ。
- `ObjectInputStream` / `ObjectOutputStream` を使う保存・同期・XLSX payload は、互換維持層を残したうえで Jackson JSON へ移行する。既存 `.pod` 読込は当面残す。
- Commons Digester ベースの XML 設定読込は、Jackson XML または JAXB のどちらかに統一する。既存設定 XML の wire shape は変えない。
- Groovy 動的コンパイルによる sorter/filter/action/style 生成は縮小し、既存設定から選択可能な固定 strategy registry へ置換する。ユーザー入力由来コードの実行は禁止する。
- `java.util.Date` / `Calendar` / `SimpleDateFormat` の新規利用を止め、境界 adapter 以外は `java.time` に寄せる。
- `System.out/err`、`printStackTrace`、手書き debug utility を SLF4J API + Logback runtime に置換する。
- `Vector`、`Hashtable`、raw `Comparator`、raw collection を標準 generics collection に置換する。
- `ClassUtils` と `Field` の comparator 周りは、残りの raw シグネチャ整理と enum-safe 化が次の小粒タスク。
- `Assignment`、`Project`、`NormalTask`、`CalendarDefinition`、`CriticalPath`、spreadsheet UI は巨大クラスのまま直接刷新せず、characterization test を先に追加してから責務分割する。

## 今回入れた依存管理の土台

- `gradle/libs.versions.toml` を追加し、主要な Maven Central 依存を一元管理する。
- root `build.gradle.kts` から暗黙の `fileTree(rootProject.file("modules/projectlibre_contrib/lib"))` を外し、Maven 依存 alias と legacy bridge jar を明示的に分ける。
- `modules/projectlibre_contrib/build.gradle.kts` の jar 展開を止め、Gradle の runtime classpath に依存解決を任せる。
- `modules/projectlibre_contrib/build.gradle.kts` の `legacyBridgeJars` と `reportsBridgeJars` は空に整理済みであり、`projectlibre_contrib` 側の local bridge classpath は解消済みである。
- 互換確認前に消さない local bridge jar は root 側 `projectLibreLegacyBridgeJars` に隔離する。ここに残る jar は「削除待ち」であり、新規追加は禁止する。
- `com.jgoodies:jgoodies-forms` は bridge jar ではなく Maven dependency に移行済み。既存 UI は `DefaultFormBuilder` shim で互換維持しつつ、徐々に新 API へ寄せる。
- `net.sf.mpxj:mpxj:13.0.1` は catalog に移行目標として定義済み。ただし現時点の `modules/projectlibre_exchange/src/main/java/net/sf/mpxj` は MPXJ 10 系の部分コピーで、13 系とは `DateHelper` / field list API が合わないため、build を保つ bridge として `mpxj-10.11.0.jar` を明示的に残す。
- JasperReports 本体は Maven dependency に移り、bundled JRXML の読込も 6.21.5 loader で安定化した。`modules/projectlibre_contrib/lib/jasperreports` に残っていた `bsh.jar` / `jasperreports.jar` / `jdt-compiler.jar` / `itext-1.3.1.jar` / `xalan.jar` は repo から削除済みである。

## Packaging Baseline

- 現行の配布正本は Gradle タスクであり、`stageAppDist`、`prepareWindowsReleaseInput`、`packageWindowsAppImage`、`packageWindowsMsi`、`packageWindowsExe`、`packageWindowsZip` を通す。
- `cleanLegacyPackagingArtifacts` は `isolated-build/` のような legacy packaging scratch output を掃除する補助タスクであり、現行配布正本の前処理として使える。
- `isolated-build/` は検証用または生成物由来の作業ディレクトリであり、配布フローの正本ではない。
- `isolated-build/projectlibre_ui/scripts/projectlibre_ui(.bat)` に残る旧 classpath 列挙は legacy packaging artifact とみなし、将来的には削除するか、Gradle 生成物であることが明確な場所に閉じ込める。
- `isolated-build/` は repo 管理対象の正本ではなく、既定方針は「削除候補」とする。必要な場合だけ監査用に一時生成し、残置を前提にしない。
- packaging の完了条件は「配布手順が Gradle タスクだけで追えること」と「repo 管理下に旧 classpath script を正本として残さないこと」である。

## Implementation Changes

- 依存管理:
  - 初期候補は MPXJ `13.0.1`、Apache POI `5.5.1`、FlatLaf `3.7.1`、PDFBox `3.0.7`、Jackson `2.22.0`、JasperReports `7.0.7`、JFreeChart `1.5.6`。
  - `projectlibre_contrib` は最終的に「必要な自前 contrib source のみ」を含む薄い module にする。
  - Maven 化済み依存は `libs.versions.toml` に置き、互換未確認の local jar は build script の bridge list にだけ残す。
  - `jgoodies-forms` は bridge jar から外し、`DefaultFormBuilder` の shim で既存の呼び出しを吸収する。

- Exchange/import/export:
  - `net.sf.mpxj` 同梱ソースへの直接変更がないか確認し、差分があれば `ProjectLibreMpxjAdapter` に移す。
  - `net.sf.mpxj.projectlibre.ProjectLibreXlsxReader` / `ProjectLibreXlsxWriter` / `SearchableInputStream` / `XlsxPayloadUtils` は fork 独自の XLSX payload を含むため、ProjectLibre 名前空間へ移設済み。
  - MPXJ 13.0.1 へ切り替えるのは、fork 内 MPXJ 10 系ソースを削除する PR で行う。10 系ソースを残したまま 13 系 jar を入れない。
  - XLSX/MPP/POD/XML smoke test を先に固定し、MPXJ/POI 依存化後も同じ sample files を読めることを確認する。

- UI:
  - Ribbon は `SwingRibbonFactory` / `ModernRibbonPanel` / `SwingRibbonModel` を adapter 境界にして、Radiance/Flamingo 依存をそこだけに閉じ込める。
  - Radiance は Maven artifact を正とし、`org.pushingpixels.neon.icon.*` / `org.pushingpixels.trident.*` は shim 経由でのみ参照する。
  - FlatLaf は Maven 依存化し、同梱 jar 参照を削除する。
  - Batik resource factory 流用はやめ、既存 menu/ribbon property を読む ProjectLibre 側 resource package へ移設済み。`TransformComboBox`、`MenuManager`、`TabbedNavigation` の `ButtonFactory` 定数参照は共有 suffix キーへ置換済み。
  - `MenuActionsMap` の不要な `ActionMap` 保持を削除し、メニュー action ルーティングの責務を軽くした。
  - `ExtMenuFactory` / `ExtToolBarFactory` / `ExtRibbonFactory` の suffix lookup 重複を `MenuLookupSupport` に集約した。
  - `MenuActionsMap` の内部 listener 追跡を typed `Map` に置き換え、`MenuManager` の不要な `rootActionMap` 保持も削除した。
  - `MenuActionsMap` の action registry を 1 つにまとめ、document action は別集合で追うようにして `menuId` と action key の二重管理を削った。
  - `GraphicManager` の未使用 `getRawAction` を削除し、`MenuManager` の tabbed-navigation 反復を typed `for-each` に整理した。
  - `TabbedNavigation` と `MenuManager` の raw collection を段階的に generics 化した。
  - テスト用 `stubActionMap` を本体側 `MenuActionMapSupport.noopActionMap()` に移し、`MenuDefinitionSupport` をデータ専用へ寄せた。
  - `ProjectMenuActionMap` と旧 Batik `ActionMap` 互換 interface の adapter を導入し、本体コードの直接依存を薄めた。resource factory は `ProjectMenuActionMap` を直接受け、変換は `BatikActionMapAdapter` に閉じ込めた。
  - `ExtButtonFactory` も `ProjectMenuActionMap` 経由に寄せ、resource factory へ adapter を差し込む形に整理した。
  - `ToolBarFactory` / `RibbonFactory` も `ProjectMenuActionMap` を直接受けるようにし、`ProjectActionMapAdapter` は不要になったため削除した。
- `pm.graphic.network.rendering.FormComponent` / `FormatSelector` を generics 化し、型安全性を上げた。
- `spreadsheet.common.transfer.NodeListTransferable` / `NodeListTransferHandler` / `TransferObject` を generics 化し、clipboard transfer 周辺の raw collection を減らした。
- `dialog.util.FieldComponentMap` / `dialog.FieldDialog` / `dialog.LookupDialog` の raw collection を整理し、`Field` / `JComponent` / `LinkedHashMap<String, Object>` ベースへ寄せた。
- `dialog.DelegateTaskDialog` / `dialog.UpdateTaskDialog` の引数を `List<?>` に寄せ、task selection 境界の raw signature を減らした。
- `:projectlibre_ui:compileJava` で上記 `dialog` 境界の generics 化を確認済み。
- `:projectlibre_ui:compileJava` で `DelegateTaskDialog` / `UpdateTaskDialog` の境界整理も確認済み。
- `pm.graphic.spreadsheet.common.CommonSpreadSheet` の `fieldArray` 境界を `ArrayList<Field>` に揃え、`SpreadSheetColumnModel` と型を一致させた。
- `:projectlibre_ui:compileJava` で `CommonSpreadSheet` の型整理を確認済み。
- `dialog.assignment.AssignmentDialog` / `AssignmentEntryPane` を `NormalTask` / `Resource` ベースに寄せ、選択・割当ダイアログの raw list 参照を減らした。
- `dialog.calendar.ChangeWorkingTimeDialogBox` を `WorkingCalendar` / `Long` ベースに寄せ、calendar selection の raw list 参照を減らした。
- `dialog.TransformParameterDialog` を `List<TransformParameter>` / `List<ExtDateField>` ベースに寄せ、transform parameter の raw iterator を減らした。
- `dialog.OpenProjectDialog` / `dialog.FindDialog` を generics 化し、project search の raw list 参照を減らした。
- `dialog.assignment.TimesheetDialog` / `TimesheetEntryPane` を `Resource` / `Assignment` / `Field` ベースに寄せ、timesheet selection の raw collection を減らした。
- `dialog.ProjectDialog` の choices / extra fields を generics 化し、project creation boundary の raw list 参照を減らした。
- `dialog.assignment.ReplaceAssignmentDialog` の返却型を `List<Resource>` に寄せ、assignment replacement boundary の raw list を減らした。
- `com.projectlibre1.exchange.ResourceMappingForm` の merge ループを generics 化し、resource mapping boundary の raw iterator を減らした。
- `pm.graphic.spreadsheet.SpreadSheet` の resource addition flow を generics 化し、resource selection/import boundary の raw iterator を減らした。
- `dialog.ResourceMappingDialog` の form access を helper 化し、resource mapping table boundary の repeated raw access を減らした。
- `pm.graphic.chart.ChartInfo` / `ChartModel` の resource list を generics 化し、chart data boundary の raw list 参照を減らした。
- `pm.graphic.chart.ChartModel` の unused raw scratch variables を削除し、chart data path のノイズを減らした。
- `dialog.ResourceMappingDialog.getBean()` を `ResourceMappingForm` へ寄せ、bean access boundary を型付きにした。
- `dialog.ResourceAdditionDialog` の `JList` を generics 化し、resource import dialog boundary の raw component type を減らした。
- `pm.graphic.spreadsheet.common.CommonSpreadSheet` の `getAvailableFields()` を `List<Field>` に戻しつつ、内部で raw lookup を吸収した。
- `pm.graphic.spreadsheet.common.CommonSpreadSheet` の `getSelectedFields()` / `getSelectableFields()` を `ArrayList<Field>` 化し、selection boundary の raw object list を減らした。
- `com.projectlibre1.algorithm.SelectFrom` / `ReverseQuery` / `Query` / `IntervalGeneratorSet` / `CollectionIntervalGenerator` を generics 化し、query / interval 周辺の raw collection をまとめて減らした。
- `CollectionIntervalGenerator` の iterator 取得を `collection.iterator()` ベースへ整理し、List 以外の collection でも自然に回る形へ寄せた。
- `com.projectlibre1.exchange.ServerFileImporter` の resource 準備境界を `List<?>` ベースへ寄せ、imported resource の raw iterator を減らした。
- `com.projectlibre1.exchange.ServerLocalFileImporter` の resource / task / distribution boundary を typed loop と安全なキャストへ寄せ、pod import の raw iterator を減らした。
- `com.projectlibre1.exchange.MicrosoftImporter` の resource mapping boundary を typed list / map に寄せ、MPX import 側の raw collection を減らした。
- `:projectlibre_exchange:compileJava` で上記 `exchange` 境界の generics 化を確認済み。
- `com.projectlibre1.server.data.Serializer` の save/build boundary を typed helper と typed collection に寄せ、project serialization の raw collection を減らした。
- `com.projectlibre1.server.data.MSPDISerializer` の resource/task boundary を typed map/list に寄せ、MSPDI export 側の raw collection を減らした。
- `:projectlibre_exchange:compileJava` で `Serializer` / `MSPDISerializer` の generics 化を確認済み。
- `:projectlibre_exchange:compileJava` で `Serializer` の sort/helper 整理を確認済み。
- `:projectlibre_ui:compileJava` で `Serializer` / `MSPDISerializer` の境界整理が UI 依存に波及していないことを確認済み。
- `:projectlibre_ui:compileJava` で `CommonTable` / `ReferenceNodeModelCache` の型整理を確認済み。
- `:projectlibre_ui:compileJava` で `NodeModelCache` / `ViewNodeModelCache` / `VisibleElements` / `SpreadSheet` の型整理を確認済み。
- `:projectlibre_core:compileJava` と `:projectlibre_ui:compileJava` で `ResourcePool` / `SelectionNodeEvent` / `TaskSnapshotBackup` の型整理を確認済み。
- `:projectlibre_core:compileJava` で `NodeList` / `CommonTransform` / `NodeSorter` / `Project` の型整理を確認済み。
- `:projectlibre_core:compileJava` で `NodeModelUtil` の型整理を確認済み。
- `:projectlibre_core:compileJava` で `MutableNodeHierarchy` の型整理を確認済み。
- `Serializer` の `setEnterpriseResources()` / `forProjectDataDo()` / `forProjectDataReversedDo()` / `createIdMap()` を typed helper に寄せ、project data traversal の raw collection をさらに減らした。
- `:projectlibre_exchange:compileJava` で上記 `Serializer` helper の型整理を確認済み。
- `Serializer.deserializeProject()` の resource/task/referring-subproject traversal を typed local collection に寄せ、deserialization path の raw iterator をさらに減らした。
- `:projectlibre_exchange:compileJava` で `Serializer.deserializeProject()` の型整理を確認済み。
- `:projectlibre_ui:compileJava` で `Serializer.deserializeProject()` の境界整理が UI 依存に波及していないことを確認済み。
- `Serializer.serialize()` / `Serializer.deserialize()` を `Collection<DataObject>` ベースへ寄せ、data object payload の raw collection を減らした。
- `Serializer.printTaskDataHierarchy()` / `buildTaskDataHierarchy()` を typed `Map<Long, Set<TaskData>>` / `TreeSet<TaskData>` ベースへ寄せ、hierarchy print の raw collection を減らした。
- `:projectlibre_exchange:compileJava` で `Serializer.serialize()` / `deserialize()` / hierarchy print の型整理を確認済み。
- `serializeIncrementalProject()` の task/resource/link diff を typed set/map へ寄せ、incremental serialization の raw iterator を減らした。
- `:projectlibre_exchange:compileJava` で `serializeIncrementalProject()` の型整理を確認済み。
- `:projectlibre_ui:compileJava` で `serializeIncrementalProject()` の型整理が UI 依存に波及していないことを確認済み。
- `IncrementalData` の assignments accessor を `Set<AssignmentData>` 化し、incremental diff の raw accessor を減らした。
- `:projectlibre_exchange:compileJava` で `IncrementalData` の accessor 整理を確認済み。
- `:projectlibre_ui:compileJava` で `IncrementalData` の accessor 整理が UI 依存に波及していないことを確認済み。
- `Serializer.deserializeProject()` の `resources` / `tasks` / `referringSubprojectTasks` を typed collection へ寄せ、deserialization path の raw collection をさらに減らした。
- `:projectlibre_exchange:compileJava` で `Serializer.deserializeProject()` の typed collection 化を再確認済み。
- `:projectlibre_ui:compileJava` で `Serializer.deserializeProject()` の typed collection 化が UI 依存に波及していないことを確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ProjectData` 境界の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Serializer` の boundary 追加整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Serializer.deserializeProject()` の追加整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ResourcePool` / `SpreadSheet` の型整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ResourcePool` 内部ループの整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `TimesheetEntryPane` の boundary 整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ServerLocalFileImporter` の read boundary 整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `MSPDISerializer` / `ProjectLibreXlsxWriter` の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `MutableNodeHierarchy` / `AssignmentFormat` の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ServerLocalFileImporter` の追加整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Serializer` helper 化を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Project.getTaskList()` bridge の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で task boundary の追加整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Serializer` の task bridge 利用追加を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `ResourcePool.userResources()` の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `TimesheetEntryPane` の explicit resource iteration 整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Project.getTasks()` の typed 化を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `PredecessorTaskList` の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `PredecessorTaskList` の backing list と iterator の generics 化を再確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `DependencyFormat` / `Project.getTaskOutlineRoot()` の整理を確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `AssignmentFormat` の resource boundary を再確認済み。
- `:projectlibre_core:compileJava`、`:projectlibre_exchange:compileJava`、`:projectlibre_ui:compileJava` で `Project` の timesheet path 整理を確認済み。
- `:projectlibre_ui:compileJava` で `assignment` / `calendar` 境界の型整理を確認済み。
- `:projectlibre_ui:compileJava` で `TransformParameterDialog` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `OpenProjectDialog` / `FindDialog` / `Timesheet` 境界の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ProjectDialog` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ReplaceAssignmentDialog` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ResourceMappingForm` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `SpreadSheet` の resource addition flow の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ResourceMappingDialog` の access helper 整理も確認済み。
- `:projectlibre_ui:compileJava` で `ChartInfo` / `ChartModel` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ChartModel` の scratch cleanup も確認済み。
- `:projectlibre_ui:compileJava` で `ResourceMappingDialog.getBean()` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `ResourceAdditionDialog` の型整理も確認済み。
- `:projectlibre_ui:compileJava` で `CommonSpreadSheet` の `getAvailableFields()` 境界整理も確認済み。
- `:projectlibre_ui:compileJava` で `CommonSpreadSheet` の selection API 整理も確認済み。
- `:projectlibre_core:compileJava` で `algorithm` 周辺の generics 化を確認済み。
- `:projectlibre_ui:compileJava` で `algorithm` 周辺の generics 化が UI 依存に波及していないことを確認済み。

- Core:
  - 設定読込、日付処理、ログ、serialization、dynamic script を先に共通 adapter 化する。
  - logging については `System.out/err` と `printStackTrace` の生存箇所を除去済み。残りは SLF4J / Logback への統一を進める。
  - DTO / session 周辺の raw collection を段階的に generics 化し、巨大クラスの責務分割前に型安全性を上げている。
  - その後、`Assignment`、`Project`、`NormalTask`、calendar、dependency/critical path を小さく分割する。
  - public API は原則維持し、変更が必要な場合は deprecated bridge を 1 リリース残す。

## 次の実装順

1. `.\gradlew.bat projects` で Gradle multi-project 構成を再確認する。
2. `.\gradlew.bat :projectlibre_contrib:dependencies --configuration runtimeClasspath` で bridge 残存を再確認する。
3. `modules/projectlibre_exchange/src/main/java/net/sf/mpxj` の vendored source と root `projectLibreLegacyBridgeJars` に残る `mpxj-10.11.0.jar` bridge を切り離し、MPXJ API 差分を ProjectLibre adapter に閉じ込める。
4. `org.pushingpixels` 配下の vendored Flamingo/Radiance ソースを Maven dependency + local shim に置き換え、同梱 UI ソースを削除する。
5. JasperReports reports bridge payload は解消済みとして維持し、再導入を防ぎつつ MPXJ / vendored source / packaging 側の残タスクへ集中する。
6. `isolated-build/` と旧 classpath script を監査し、legacy packaging artifact を削除または生成物専用領域へ閉じ込める。
7. 完了条件として「repo 内に Maven 遺産の vendored code、bridge jar、旧 packaging artifact を正本として残さない」状態を確認する。

## Validation / Acceptance

- 文書中の「現状」と「未完了」は、実際の repo 構成と一致していなければならない。
- `pom.xml` は repo に存在しないことを前提に記述する。
- `settings.gradle.kts` では 6 サブプロジェクトが定義されていることを前提に記述する。
- `modules/projectlibre_contrib/build.gradle.kts` では `legacyBridgeJars` と `reportsBridgeJars` は空であることを前提に記述する。
- root `build.gradle.kts` では `modules/projectlibre_contrib/lib/mpxj-10.11.0.jar` が `projectLibreLegacyBridgeJars` としてまだ残っていることを前提に記述する。
- `isolated-build/` には旧 classpath script が残り得るため、現行配布正本ではない legacy packaging artifact として扱う。
- 更新後の文書は、「どれが現行モジュールで、どれが削除対象の legacy か」を読み手が誤解しない構成であることを受け入れ条件とする。

## Test Plan

- `.\gradlew.bat projects` で multi-project 解決を確認する。
- `.\gradlew.bat build` を最終ゲートにする。既存設定で root test が disabled の場合も、変更対象 module の test は個別実行する。
- `.\gradlew.bat :projectlibre_exchange:test` で MPP/XLSX/POD/XML import/export の回帰を確認する。
- `.\gradlew.bat :projectlibre_ui:test` で ribbon、toolbar、spreadsheet、Gantt、dialog の既存 UI 回帰を確認する。
- `.\gradlew.bat :projectlibre_ui:compileJava` で JGoodies Forms の互換 shim を含む UI コンパイルを確認する。
- `.\gradlew.bat verifyPackagedFileImports` で packaged runtime module 制限下の import を確認する。
- `.\gradlew.bat stageAppDist` 後、`samples/Commercial construction project plan.mpp` と `samples/June_1_sample.pod` を手動起動で開く。
- 依存削除ごとに `modules/projectlibre_contrib/lib` の jar 数と配布サイズを記録する。
- `isolated-build/` に旧 classpath script や過去配布物が残る場合、それらを正本扱いしていないことを確認する。

## Assumptions

- 「古いモジュール」は Gradle サブプロジェクトではなく、vendored code・bridge jar・旧 packaging artifact を指す。
- `projectlibre_contrib`、`projectlibre_core`、`projectlibre_application`、`projectlibre_ui`、`projectlibre_exchange`、`projectlibre_reports` は現行構成として維持する。
- `isolated-build/` は生成物または旧配布検証成果物として扱い、正本ビルド定義の一部とはみなさない。
- 最新版情報は Maven Central / 公式ページ確認を前提にする。参照元: [MPXJ](https://central.sonatype.com/artifact/net.sf.mpxj/mpxj/13.0.1), [Apache POI](https://poi.apache.org/download.html), [FlatLaf](https://central.sonatype.com/artifact/com.formdev/flatlaf), [PDFBox](https://pdfbox.apache.org/download.html), [Jackson](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind), [JasperReports](https://central.sonatype.com/artifact/net.sf.jasperreports/jasperreports), [JFreeChart](https://repo1.maven.org/maven2/org/jfree/jfreechart/).
- 独自スケジューリング・クリティカルパス・カレンダー計算はプロダクト固有ロジックなので、外部ライブラリへの無理な置換ではなく、標準 API と有名ライブラリで周辺実装を置換する。
