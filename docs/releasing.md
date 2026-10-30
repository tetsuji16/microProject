# 安定版リリース手順

安定版の運用手順はここに集約します。workflow定義も参照してください:
[Release](../.github/workflows/release-and-pages.yml)、
[GUI acceptance](../.github/workflows/gui-acceptance.yml)。

## 候補とGUI確認

GUI-observableな挙動を変える修正では、PR段階で変更経路のfocused GUIを通します。docs-onlyやGUI挙動を変えないsource modernizationではfocused GUIを追加しません。安定版リリース時はexact SHAのfull gateを一度実行します。focused testが失敗したら原因を直し、該当するlocale/scaleを先に通してください。その後にfullを実行します。source-equivalentな証拠は、production・fixture・build入力の差分を調べて影響対象が変わっていない場合に限りcriterionの補助に使えます。安定版リリースのfull gateは常にその候補SHAで必要です。

full gateが失敗したら、同じ未修正SHAでfullを繰り返さず、該当artifactで原因を絞ります。修正後にfocused testを通し、新しい候補SHAでfullを一度実行します。

GUI Acceptance workflowの手動入力で、失敗したtest methodとlocale/scaleを再実行できます。入力値はworkflow定義に従います。次は日本語150%の例です。

```powershell
$branch = 'fix/calendar-exception-physical-routes'
$test = 'com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotOpensWorkingTimeDialogAndCancelsWithoutCommit'
gh workflow run gui-acceptance.yml --ref $branch `
  -f gui_suite=smoke `
  -f gui_test_filter=$test `
  -f gui_test_locale=ja `
  -f gui_test_scale=1.5
gh run list --workflow gui-acceptance.yml --branch $branch --limit 5
```

該当runのログでtestの`STARTED` / `PASSED`または失敗を確認します。完了直後に、そのrun固有のJUnit・画面・診断artifactをsnapshotします。展開日時だけを実行日時と取り違えないよう、run ID・head SHA・開始/完了時刻・取得時刻を一緒に記録し、古いarchiveを今回の証拠として引用しません。

```powershell
$runId = Read-Host '今回のrun listに表示されたrun ID'
$run = gh run view $runId --json headSha,status,conclusion,createdAt,startedAt,updatedAt | ConvertFrom-Json
$expectedSha = Read-Host '候補SHA'
if ($run.headSha -ne $expectedSha) { throw "Run SHA $($run.headSha) differs from expected $expectedSha." }
if ($run.status -ne 'completed') { throw "Run $runId has not completed." }
$capturedAt = Get-Date -AsUTC -Format o
$stamp = Get-Date -AsUTC -Format yyyyMMddTHHmmssZ
$snapshot = Join-Path $env:TEMP "gui-evidence-$runId-$stamp"
New-Item -ItemType Directory -Force -Path $snapshot | Out-Null
$artifactName = "gui-audit-evidence-$runId" # Release runでは gui-acceptance-evidence-$runId に変更
gh run download $runId --name $artifactName --dir $snapshot
if ($LASTEXITCODE -ne 0) { throw "Could not download GUI evidence for run $runId." }
[pscustomobject]@{ runId=$runId; headSha=$run.headSha; status=$run.status; conclusion=$run.conclusion; startedAt=$run.startedAt; completedAt=$run.updatedAt; capturedAt=$capturedAt; artifactPath=$snapshot } |
  ConvertTo-Json | Set-Content (Join-Path $snapshot 'run-provenance.json')
Get-ChildItem $snapshot -Recurse -File | Select-Object FullName,LastWriteTime
```

Release workflowのGUI artifact名は`gui-acceptance-evidence-$runId`、単独GUI workflowは`gui-audit-evidence-$runId`です。安定版full gateは日本語100%機能baseline（現在241件）と、ja/en・100/125/150%の5 visual legs（現在各65件）です。各JUnitで失敗・errorがないこと、skipがある場合はgateが許可する理由に該当することを確認します。PR smokeや一部focused runをfull結果として扱わないでください。

Windows runnerの表示領域は1024x768です。150%では約683x512 logical pixels、実用縦幅は約480pxです。既存calendar journeyは親dialogを650x480、子dialogを636x415に制約します。nested viewportでは20回の物理wheelで180pxしか動かず、必要な250pxに届かなかったため、bounded上限を40回にしています。解像度を変えず、物理経路・assertionをskipまたは弱めないでください。通常unit suiteにはmacOS F2 root shortcut ownershipとtimezone間のcalendar GMT変換の回帰テストも含まれます。

## 安定版を一度だけ実行

masterに統合したレビュー済みSHAを候補にします。以下はstable branchをローカルに作らず、`origin/master`のそのSHAを新規または既存stable branchへfast-forwardでpushする例です。まず同branchのrun一覧を確認し、queued/in-progressのReleaseがあれば新しい実行を開始しないでください。pushと手動dispatchを両方行うと二重run・異なるrun番号のversionになるため、どちらか一方だけ選びます。同じSHAが既にbranchにありpushで更新が起きない場合、再実行はpushではなく手動dispatchを使います。

```powershell
$branch = 'release/stable-0.0.25'
git fetch origin master
$candidateSha = (git rev-parse origin/master).Trim()
$existingRuns = gh run list --workflow release-and-pages.yml --branch $branch --limit 100 --json databaseId,status,headSha,displayTitle | ConvertFrom-Json
$activeRuns = @($existingRuns | Where-Object { $_.status -in @('queued','in_progress','waiting','requested','pending') })
if ($activeRuns.Count) { $activeRuns | Format-Table; throw 'A Release run is already queued or active for this branch.' }
git push origin "$($candidateSha):refs/heads/$branch"
gh run list --workflow release-and-pages.yml --branch $branch --commit $candidateSha --limit 5
```

pushの代わりに手動実行する場合は、既に存在するstable branchのSHAを確認してから一度だけdispatchします。Unix packageも明示的に有効にします。

```powershell
$branch = 'release/stable-0.0.25'
git fetch origin $branch
$candidateSha = (git rev-parse FETCH_HEAD).Trim()
$existingRuns = gh run list --workflow release-and-pages.yml --branch $branch --limit 100 --json databaseId,status,headSha,displayTitle | ConvertFrom-Json
$activeRuns = @($existingRuns | Where-Object { $_.status -in @('queued','in_progress','waiting','requested','pending') })
if ($activeRuns.Count) { $activeRuns | Format-Table; throw 'A Release run is already queued or active for this branch.' }
gh workflow run release-and-pages.yml --ref $branch `
  -f release_channel=stable `
  -f gui_suite=full `
  -f build_unix_packages=true
gh run list --workflow release-and-pages.yml --branch $branch --commit $candidateSha --limit 5
```

一覧からrun IDを選び、対象runのSHAが候補と一致することを確認して完了まで待ちます。

```powershell
$runId = Read-Host '今回のrun listに表示されたrun ID'
gh run watch $runId --exit-status
gh run view $runId --json headSha,status,conclusion,url,createdAt,updatedAt
```

stable branch pushではWindows・Linux x64・macOS Apple Siliconのpackage jobが同一runで動きます。full GUI、module tests、package、Windows launch/import確認、全platform packageが成功するまで公開されません。tagはworkflowがrun numberから作成するため、手動作成しないでください。

## 公開後の確認

完了runのsummaryからtagを取得し、Release APIでassetとdigestを確認します。GitHub CLI `gh release view --json`に`isLatest`はありません。latest stableは`/releases/latest`のtagと照合します。公開するのは配布パッケージだけで、チェックサムはGitHubがassetごとに記録するSHA-256 digestを使います。バイナリは通常downloadしません。

```powershell
$tag = Read-Host '今回完了したrun summaryのtag'
$release = gh api "repos/tetsuji16/ProjectLibre/releases/tags/$tag" | ConvertFrom-Json
$latest = gh api repos/tetsuji16/ProjectLibre/releases/latest --jq .tag_name
$assets = @{}; foreach ($asset in $release.assets) { $assets[$asset.name] = $asset }
$release | Select-Object tag_name,target_commitish,html_url
"latest stable: $latest"
gh api "repos/tetsuji16/ProjectLibre/git/ref/tags/$tag"
$version = $tag -replace '^v',''
$required = @('microProject-latest.msi', 'microProject-latest-app-image.zip', "microProject-$version-linux-x64.zip", "microProject-$version-macos-aarch64.zip")
$missing = @($required | Where-Object { -not $assets.ContainsKey($_) })
if ($missing.Count) { throw "Missing release assets: $($missing -join ', ')" }
$unexpected = @($assets.Keys | Where-Object { $_ -notin $required })
if ($unexpected.Count) { throw "Unexpected release assets: $($unexpected -join ', ')" }
if ($latest -ne $tag) { throw "Latest stable tag is $latest, expected $tag" }
$required | ForEach-Object { "$($assets[$_].digest)  $_" }
```

tag refがannotated tagなら、ref objectをGit tag APIで解決し、commit SHAが対象runの`headSha`と同じことを確認します。安定版ReleaseのWindows/Linux/macOS assetとAPI digestを記録します。Linux/macOS packageを生成しない安定版では、`$required`から該当2件を除いて確認します。workflow内のWindows packaged launch/import checksも成功していることを確認してください。

成功済みexact-SHA runでplatform package生成まで成功し、Releaseへのasset追加だけ漏れた場合は、同じrunの`package-linux-x64` / `package-macos-aarch64` artifactを補充元として使えます。このasset-only修復ではfull GUIを繰り返しません。補充前にrun ID・head SHA・artifact名を照合し、追加した各ファイルの名前とSHA-256を記録します。補充後のRelease API digestがartifactのSHA-256と一致することを確認し、検証結果を同じrun ID/SHAに結び付けます。

issueを閉じる場合は公開後、凍結した各acceptance criterionをevidence packetと照合します。参照issueがopenであることだけを未達の根拠にしません。
