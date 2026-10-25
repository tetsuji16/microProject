# microProject 0.1.0 stable release

microProject 0.1.0 is a tested stable release for the workflows and platforms that pass the exact-candidate release gates below. “Stable” describes that verified scope; it does not claim full compatibility with Microsoft Project desktop.

## Highlights

- Windows is the reference desktop platform. The release GUI evidence covers the Windows acceptance scope, including the full locale/scale visual matrix, alongside the applicable build, import, and packaging gates.
- MPO is microProject’s native and default save format. The release gates verify native save/reload and reading supported earlier MPO versions.
- MPP support is bounded to the tested Microsoft Project file read/interoperability cases; it is not a claim of complete MPP compatibility. POD remains a frozen legacy import format, with its serialized contract unchanged.
- Linux x64 and macOS Apple Silicon packages are optional major-update assets. Their exact-candidate package and launch evidence is recorded only after the release is published; this note does not assert that evidence in advance.

VBA/macros, the Microsoft Project object model, COM automation and add-ins, VSTO, and local Office add-ins are excluded from this release claim. MPP write interoperability and other unverified Microsoft Project behaviors remain residual compatibility limits tracked in the roadmap. Project Server, PWA, Project Online, Planner, and other connected cloud services are outside the product scope.

## Verification

Publish only after the exact candidate passes the required module tests, architecture and naming checks, full Windows GUI locale/scale suite, fixed MPP/POD import checks, Windows package creation, packaged application launch, and SHA-256 generation. Record the tested source and package evidence with the published release. Do not report Linux or macOS package evidence until those packages have been built and launched for the published release.
