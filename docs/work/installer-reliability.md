# Installer reliability work

Original intake baseline: `df5d55462013637196d041bd90cd5b31c0735d5a`; integration baseline: `6d3fc473c`.

| Acceptance | State and evidence |
| --- | --- |
| Standard-user update succeeds and failed updates preserve launch | Open. Packaged launchers disable in-app updates and releases publish no update feed. MSI is machine-wide by default; if the updater is re-enabled, it writes to the manifest's app directory, and `ConfigurationGenerator` embeds the build machine's absolute app path. Existing installed clients must be considered before changing feed format or installation scope. |
| Windows code signing for distributed installers and binaries | Open. No Authenticode signing step in release workflow; a trusted signing identity and timestamp service must be configured and verified. update4j signing is separate. |
| Upgrade, repair, removal and associations preserve existing installations | Open. Verify against a previously released MSI and record its UpgradeCode before choosing an explicit UUID or changing installation scope. |
| EXE and MSI use matching launch, update policy and associations | Implemented in `build.gradle.kts`; packaging and physical installation verification remain open. EXE is not currently a release asset. |

Next slice: establish a reproducible previous-release MSI upgrade/repair/uninstall matrix on an isolated Windows machine, identify its UpgradeCode and install scope, and design a signed update path that works for existing clients without writing into protected directories. Then implement and test migration before enabling publication. WiX and dotnet are not available on the intake machine; do not install over a user's existing application as a test.
