## Summary

- Issue: #1364 (follow-up of #1105)

Issue #1364 asked for a visual check of the dark-theme CCPM buffer chart. The
same property is now pinned by an automated headless test, following the
existing theme-test style in `microproject_ui`:

- `CriticalChainBufferChartPanelThemeTest.darkThemePaintsSeparableBandsAndReadableLabelsOnTheSurface`
  installs the dark theme, paints `CriticalChainBufferChartPanel` into a
  `BufferedImage` through the normal paint path with a finished CCPM analysis,
  and asserts that the pixel inside each band equals the themed zone colour.
- The three band colours must be at least 40 apart in summed RGB distance, and
  each must be that far from the sampled panel surface, so buffer bands are
  separable from each other and from the background.
- `textColor()` must keep a 4.5 WCAG contrast ratio against the surface and
  against every band, so the chart labels stay readable on the dark chart.
- The existing light-theme, theme-switch and selection-outline assertions in the
  same class are unchanged.
- `docs/work/issue-1364.md` records the base commit, command and result.

No production code changes; no manual `installDist` run and no saved project,
because the check no longer depends on a screen capture.

Refs #1364

## Verification

- [x] `.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.view.views.CriticalChainBufferChartPanelThemeTest" --console=plain` → BUILD SUCCESSFUL; tests=3, skipped=0, failures=0, errors=0

## Completion criteria

- [x] 使用したコミット、サンプルのコピー、手順、結果と画面の証跡が記録され、ダークテーマでバッファ帯と表示値が識別できる。 → 画面の証跡の代わりに headless の描画テストで固定した（`darkThemePaintsSeparableBandsAndReadableLabelsOnTheSurface`）: ダークテーマで描画した各バッファ帯のピクセルがテーマのゾーン色と一致し、帯同士・帯と背景が RGB 距離 40 以上で区別でき、文字色は背景と全帯に対してコントラスト比 4.5 以上。実行結果は `docs/work/issue-1364.md` に記録（base `4e308fb40`、`.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.view.views.CriticalChainBufferChartPanelThemeTest" --console=plain` → tests=3, failures=0）。

## Risk / remaining work

- The manual dark-theme review on a running desktop is still not part of this change; the test covers the painted colours and text contrast, not font rendering or DPI scaling.
- The issue comment on #1364 and the evidence link in #1105 are recorded after the merge, so this PR only adds the test and the work packet.
