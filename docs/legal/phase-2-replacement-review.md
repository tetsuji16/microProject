# Phase 2 replacement review

## Reviewed scope

This review covers the Phase 2 changes made in this repository:

- branding and distribution aliases now point to `microProject` assets;
- the retired update checker, startup registration, cloud-help, donation, and
  orphaned demo classes were removed;
- reports/import/export boundary checks were added without changing format
  identifiers or compatibility packages.

## Results

- No active source reference remains for `UpdateChecker`, `DonateDialog`,
  `UserInfoDialog`, `EclipseMain`, or `TestFrame`.
- The former PayPal asset `payPalDonate.gif` was removed from the source tree and
  its `paypal.donate` resource mapping was removed.
- `images.properties` resolves the active logo aliases to `microproject-logo.png`.
- No OpenProj header was restored in this Phase 2 work. Therefore no file was
  treated as proven ProjectLibre-free based on a header-only change.
- No replacement/legacy implementation pair is retained for the removed
  features; compile and acceptance tests passed after deletion.

## Deliberately retained

ProjectLibre wording in CPAL/Exhibit headers, license pages, compatibility keys,
format identifiers, and historical translation bundles remains under REVIEW.
Those resources are not claimed to be removed by this Phase 2 slice because
their legal or compatibility status requires hunk-level and legal review.
