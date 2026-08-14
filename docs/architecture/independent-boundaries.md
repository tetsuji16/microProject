# microProject independent boundaries

The reports and import/export code are maintained as replaceable adapters around
the scheduling model. Their public boundary is the module dependency graph, not
the legacy package spelling.

## Reports

`projectlibre_reports` may depend on `projectlibre_core` and the bundled reporting
libraries in `projectlibre_contrib`. It must not depend on Swing UI classes,
application workflow classes, or the exchange module. The UI loads the report
view as an optional integration point.

## Import/export

`projectlibre_exchange` may depend on `projectlibre_core`,
`projectlibre_contrib`, and format libraries such as MPXJ. It owns conversion
and round-trip behavior for MPP, POD, XML, XLSX, and related formats. It must
not depend on UI, application workflow, or report classes.

## Compatibility rule

Legacy package names (`com.projectlibre1.*`, `com.projectlibre.*`) and file-format
identifiers remain compatibility boundaries. This check does not rename them or
rewrite provenance headers. It only prevents new coupling that would make the
reports or exchange adapters inseparable from the desktop UI.

Run the guard with:

```powershell
.\gradlew.bat verifyIndependentBoundaries --console=plain
```
