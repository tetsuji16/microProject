# XML/JRXML element review queue

This is a review grouping for the element-level rows in
`projectlibre-delta-items.csv`. It does not classify an element as a
ProjectLibre delta by filename or by the fact that it was added after OpenProj.

Current inventory (2026-08-14):

| Group | Elements | Default handling |
|---|---:|---|
| `modules/projectlibre_exchange/testdata/New Product.xml` | 11,351 | REVIEW as an exchange fixture; require round-trip/fixture provenance before any deletion |
| `projectlibre_build/build.xml` and module `build.xml` files | 762 | REVIEW build behavior; separate branding/package nodes from toolchain nodes |
| Core configuration XML (`configuration.xml`, `view.xml`) | 122 | REVIEW persistence/UI configuration; preserve OpenProj-compatible nodes until behavior tests exist |
| Eclipse/settings XML | 6 | REVIEW development metadata; remove only if untracked and unused |
| `projectlibre_build/resources/projectlibre.xml` | 6 | REVIEW product/distribution metadata; compare with current Gradle packaging |
| Other XML/JRXML elements | 4 | REVIEW individually |
| **Total XML_NODE rows** | **12,251** | |

## Comparison stages

| Stage | Elements |
|---|---:|
| `OPENPROJ_TO_INITIAL` | 11,413 |
| `MULTI_STAGE` | 709 |
| `INITIAL_TO_1_9_8` | 129 |

The large `OPENPROJ_TO_INITIAL` group is not automatically ProjectLibre-owned:
the initial commit also contains imported upstream, fixture, build, and
namespace changes. Each row remains `REVIEW` until its node behavior and source
history are identified.

## Acceptance for one XML element

An element can become `VERIFIED` only after its OpenProj node, ProjectLibre
baseline node, and current node are compared; the current behavior or fixture
role is recorded; and the relevant configuration, report, or round-trip test
passes. An unused ProjectLibre-only node is deleted without replacement. A
required node is independently reimplemented only from its public behavior and
fixture, not copied from the ProjectLibre implementation.
