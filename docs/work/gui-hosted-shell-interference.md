# Hosted GUI shell interruption

State: mitigation implemented; hosted confirmation pending
Baseline HEAD: `5e9d49ee68f8a3e79e89cdf5f81a58d7ba329e93`

## Evidence and cause

- Hosted stable full run [40665140195](https://github.com/tetsuji16/microProject/actions/runs/40665140195), candidate `8b911fa8f71505fb804328bf786ce28b5a169176`, recorded `ShellExperienceHost` / `Windows.UI.Core.CoreWindow` / `Windows Shell Experience Host` at `664,0,360,720`, overlapping the test window. Its screenshot shows the Notifications/Focus flyout.
- The earlier hosted failure [40413593220](https://github.com/tetsuji16/microProject/actions/runs/40413593220) recorded Explorer's `TopLevelWindowForOverflowXamlIsland` system tray overflow window during a separate GUI test.
- The affected routes do not use the taskbar. Evidence supports transient notification-area flyouts as the shared interruption class; the specific OS/runner trigger is unknown. Toast delivery is the controllable antecedent, not a proven explanation for every flyout.

## Mitigation

The hosted GUI runner now sets the dedicated, short-lived account's `HKCU\Software\Microsoft\Windows\CurrentVersion\PushNotifications\ToastEnabled` to `0` and verifies the effective value before any gate leg. Local runs are unchanged. This records the requested toast-notification preference; reading back `0` does not prove that notification-center or tray-overflow flyouts are prevented, and the trigger for those shell windows remains unknown. The foreground-overlap detector and Robot input block remain active, with no retry, suppression, or resolution change.

## Verification

- `python -m unittest scripts.tests.test_release_workflow_policy` — pending.
- PowerShell parser validation of `.github/scripts/run-gui-acceptance.ps1` — pending.
- Hosted GUI run after this change — pending; required to establish whether the intervention addresses the recorded runner behavior.
