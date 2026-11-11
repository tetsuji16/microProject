# Translation key audit

`client.properties` contains runtime-generated families (for example
`Field.*`, `Bar.*`, and `Category.*`) that cannot be identified by searching
for literal calls alone. The supported audit command classifies keys before a
translation is removed:

```text
python scripts/translation_key_audit.py --output %TEMP%\translation-key-audit.tsv
```

The report has four categories:

- `literal`: a production call uses the exact key;
- `dynamic`: the key belongs to a known runtime-generated family;
- `resource-or-text`: the key occurs in a production resource or format text;
- `candidate`: no production reference was found by the conservative scan.

Only `candidate` keys are eligible for manual removal. Before removing one,
check plugin bundles, reflection/configuration lookups, and every locale file.
The reviewed removal list is kept in
`scripts/translation_key_prune_allowlist.txt`. Apply it with

```text
python scripts/prune_translation_keys.py --apply
```

The pruner detects UTF-8 versus legacy Windows code-page bundles and preserves
their original encoding and newline convention. Keys not in the allowlist are
never removed automatically. The current audit has zero remaining candidates
after removing the reviewed legacy keys.

## Locale files and maintenance scope

`Messages` loads `com.microproject.strings.client` through the standard
`ResourceBundle` lookup, so only files named `client.properties` or
`client_<language>[_<COUNTRY>].properties` can ever be resolved. Numbered
copies such as `client_es2.properties` and editor temporary files
(`.!NNNN!client_*.properties`) are unreachable at run time and must not be
committed.

Maintenance scope:

- English (`client.properties`) and Japanese (`client_ja.properties`) are
  maintained. Every key added to the English bundle must be added to the
  Japanese bundle in the same change, and the two key sets must stay equal.
- All other locale bundles are kept on a best-effort basis. Keys missing from
  them fall back to English through the `ResourceBundle` parent chain; a
  functional change is not required to update them.

### Japanese values intentionally identical to English

The following 27 keys have the same value in both bundles. They are not
translation gaps:

| Keys | Reason |
| --- | --- |
| `Collaboration.ExternalChangeTitle`, `Text.ApplicationTitle`, `Text.ShortTitle`, `PageSetupDialog.PrinterPDFService` | Product name `microProject` |
| `File.microsoft`, `File.microsoftXML`, `File.planner` | Names of other products and their file formats |
| `EarnedValueCalculator.SPI`, `EarnedValueCalculator.CPI`, `EarnedValueCalculator.CSI`, `Field.acwp`, `Field.bac`, `Field.bcwp`, `Field.bcws`, `Field.cpi`, `Field.csi`, `Field.eac`, `Field.spi`, `Field.tcpi`, `Field.vac`, `Field.wbs`, `Field.rbsCode` | Earned-value and structure abbreviations used as-is in Japanese project management |
| `Format.htmlLines`, `Format.htmlEmphasizedWords`, `Format.htmlTooltip` | HTML markup templates without words |
| `Summary.AND` (and the two-letter `Summary.OR`) | Logical rollup operators, written as-is in Japanese |
| `Field.debugDependencyOrder` | Debug-only field name |

Re-check the list by comparing the two bundles as UTF-8 text whenever keys
are added; a new identical value needs either a translation or a row here.
