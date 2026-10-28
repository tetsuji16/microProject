# Hosted GUI shell interruption

State: mitigation implemented; hosted confirmation pending
Baseline HEAD: `5e9d49ee68f8a3e79e89cdf5f81a58d7ba329e93`

## Evidence and cause

- Hosted stable full run [40665140195](https://github.com/tetsuji16/microProject/actions/runs/40665140195), candidate `8b911fa8f71505fb804328bf786ce28b5a169176`, recorded `ShellExperienceHost` / `Windows.UI.Core.CoreWindow` / `Windows Shell Experience Host` at `664,0,360,720`, overlapping the test window. Its screenshot shows the Notifications/Focus flyout.
- The earlier hosted failure [40413593220](https://github.com/tetsuji16/microProject/actions/runs/40413593220) recorded Explorer's `TopLevelWindowForOverflowXamlIsland` system tray overflow window during a separate GUI test.
- The affected routes do not use the taskbar. Evidence supports transient notification-area flyouts as the shared interruption class; the specific OS/runner trigger is unknown. Toast delivery is the controllable antecedent, not a proven explanation for every flyout.

## Mitigation

The hosted GUI runner attempts to set the dedicated, short-lived account's `HKCU\Software\Microsoft\Windows\CurrentVersion\PushNotifications\ToastEnabled` to `0` and verifies the effective value before any gate leg. A denied write or failed readback is recorded as a warning and in the step summary, then the gate continues with the foreground-overlap detector and Robot input block active. Local runs are unchanged. A successful readback records only the requested toast-notification preference; it does not prove that notification-center or tray-overflow flyouts are prevented, and the trigger for those shell windows remains unknown.

## Verification

- `python -m unittest scripts.tests.test_release_workflow_policy` — passed on the initial integration commit.
- PowerShell parser validation of `.github/scripts/run-gui-acceptance.ps1` — passed on the initial integration commit.
- Hosted full run `40675661733` (`ac3e092b`) — failed before launching the GUI suite: runner denied `New-Item` for the HKCU PushNotifications key (`Unauthorized operation`, script line 30). Build and preflight checks passed; failure diagnostics were uploaded. The next run must determine whether the GUI gate runs with the warning fallback and whether the original shell interference recurs.
