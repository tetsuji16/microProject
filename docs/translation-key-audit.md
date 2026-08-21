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
