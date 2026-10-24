# Issue #906 — Remove unused server DTOs, registration strings and legacy registration checks

State: IMPLEMENTED — frozen local verification passed; final CI/closure status in [integration record](https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503)
Issue: https://github.com/tetsuji16/microProject/issues/906
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: concurrent #904/#905/#902 changes present; preserved

## Scope and compatibility boundary

Remove the old `LicenseDialog` registration gate, preference writes, and
`projectlibre.validation` system-property bridge. Keep the dialog reachable
from `HelpDialog`; preserve its license/third-party notices and local behavior.
Review apparently unused `server.data`/`server.access` types without changing
POD serialized classes based only on absence from the fixed fixture corpus.
Remove only nonserialized helpers with a complete zero-caller/zero-registration
search. The all-locale resource-key inventory is investigation only until
#904/#905 finish. Do not touch `server.access.ErrorLogger` while #905 owns its
migration.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Legacy registration validation state and `projectlibre.validation` bridge removed; Help dialog remains | pending | `LicenseDialog` simplified to unconditional local notice display; `HelpDialog` caller retained; focused test pending |
| A2 | Candidate `server.data`/`server.access` deletion review respects POD and live local callers | pass | serializable candidates retained; two nonserialized orphan conversion-hook types removed |
| A3 | Unused resource keys removed across locale bundles; ambiguous dynamic key retained with evidence | pass for reviewed keys | `Field.partner` and `PartnerInfo` compatibility retention decision complete; see rationale below |
| A4 | Focused tests and source/status review | pending integrated gate | scoped diff whitespace/source-reference check passed; `LicenseDialogResourceTest` reported passing by #907, parent integrated gate remains |

## Type deletion review

Repository-wide Java/XML/resource symbol search found low direct-reference
counts for `ExtendedDistributionData`, `ByteArrayObject`, `CompanyData`,
`GroupData`, `RoleData`, `LockException`, and `UserData`. The resource classes include serialization UIDs and some expose
`SerializedDataObjectFactory`; POD object streams are read through
`SerializeUtil`/`SafeObjectInput`. `PodDescriptorInventoryTest` covers the
checked-in corpus only, and #902 explicitly records that it does not exhaust
user POD files. Therefore no candidate serialized type is removed or altered
in this slice. `ErrorLogger` has active callers in core and exchange; its
`server.access` package migration remains owned by #905.

`TypeSystemConverter` and `TypeSystemConverterFactory` were the bounded
nonserialized-helper exception: repository-wide production source/resource
search found no caller, implementation, or shipped `TypeSystemConverter` meta
key; only the factory's own reflective lookup referenced them. Both orphan
classes were removed. Neither has a serial UID or appears in the fixed POD
descriptor inventory; serialized DTOs were untouched.

## Resource-key review

Removed confirmed unused `LoginDialog.*`, `Login.error`, `Login.tooManyBad`,
`OpenProjectDialog.*`, `Message.serverUnreachable`,
`Text.notAvailableInProjectLibre`, `tip.1`–`tip.9` name/description entries,
and both `PageSetupDialog.SaveWebPDFSettings` entries from all 41 tracked
`client*.properties` locale bundles, including `nativeCodePage` variants.
Production Java/XML/config searches found no literal references and no
`tip.`, `Text.`, `Message.`, login, or open-dialog key construction that could
reach those keys. `tip.12.*` remains untouched. Hidden `.!*client*.properties`
files are tracked editor backups, not ResourceBundle locales, and were not
edited.

`Field.partner` is retained in every locale as a dynamic field-ID label bridge:
generic field-name lookup calls `Messages.getString(id)`, including for a
Field ID restored from saved project/workspace metadata even when it is not in
the current `FieldDictionary`. `PartnerInfo` is a Serializable type with an
explicit UID, and `Environment.partnerId` is retained as legacy identity state.
Preserve these keys/types as persisted compatibility vocabulary, not as an
active connected-service feature; this is a completed preservation decision,
not pending #905 caller work.

## Latest evidence

- Production edits remove preference and system-property validation handling;
  `HelpDialog` still calls `LicenseDialog.showDialog(owner)`.
- Locale scan: 41 tracked bundles, 1,129 obsolete property lines removed;
  follow-up `rg` found no remaining requested keys, and scoped `git diff --check`
  passed.
- Changed dialog cancellation/close behavior requires the physical Help →
  License route evidence in the GUI quality gate; that evidence is pending.
- Focused verification command: `./gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.LicenseDialogResourceTest" --console=plain --no-daemon --max-workers=1`; #907 reports the focused test passing in the shared candidate.
- Final integrated UI/core and parent acceptance gate remain with Sol; do not claim issue #906 complete until that gate is recorded.

## Final integration gate

The frozen candidate passed architecture/naming gates, maintenance 20/20,
source inventory, clean build/installDist (2,880 tests; no failures/errors/skips),
and GUI source compilation. This supersedes earlier slice-time pending local
checks above. Candidate SHA, Windows Robot outcomes and closure are maintained
in the linked integration record; physical acceptance is required before close.
