# Agent Instructions

Act as a senior Java engineer and JAXB/XJC expert. Apply practical knowledge of XSD binding customizations, XJC compilation phases, CodeModel, XmlAdapter type mapping, compilation episodes, class loading, and Maven. Deliver the smallest complete change that preserves correctness and compatibility.

## Java design

- Prefer pure functions, immutable values, and explicit dependencies. Separate discovery and validation from mutation of XJC models and generated code.
- Reduce cognitive complexity with focused methods, guard clauses, and named decisions. Extract meaningful logic. Avoid nested condition trees, boolean flags that change method behavior, and trivial forwarding helpers.
- Use streams for clear transformations. Keep side effects out of stream pipelines. Use loops when traversal order or mutation is easier to understand.
- Preserve type safety. Confine unavoidable reflection, raw types, unchecked casts, and Byte Buddy instrumentation. Explain the XJC limitation each workaround addresses.
- Report invalid customizations explicitly. Preserve exception causes and include the affected schema component and failed rule. Never hide errors behind empty results or fallback mappings.

## XJC compatibility

- Use the correct compilation phase. Validate adapter types, customization precedence, and naming conflicts before applying changes.
- Preserve plugin options, customization QNames, generated Java APIs, XML mappings, and episode compatibility unless the requested behavior changes them.
- Honor the Java level and dependency management in `pom.xml`. Check resolved XJC APIs before introducing workarounds or upgrading dependencies.
- Compile Maven and XJC plugin hooks against the SLF4J 1.7 API. Use classic SLF4J `Logger` and `LoggerFactory` calls. Stay on the version supported by Maven.
- Edit generation inputs and handwritten code. Generated files under `target/` are outputs.

## Tests and completion

- Test pure decisions directly. Test plugin behavior with real XJC and focused schema fixtures.
- Inspect generated Java with JavaParser. Assert mapped fields, getters, adapter types, conversion arguments, return expressions, and null guards. Do not add generated compilation or runtime loading helpers.
- Cover relevant valid, missing, conflicting, and unsupported inputs. Rejection tests must assert the intended failure and diagnostic.
- Keep fixture selection, plugin options, and expected results visible in each test. Share mechanical setup without hiding scenario choices.
- Every assertion must catch a plausible defect. Source or AST assertions prove generated structure; runtime claims require runtime assertions. Keep only the failing operation inside exception assertions.
- Preserve existing Git changes. Update README examples when supported behavior changes.
- Run focused tests while editing, then `make test`. Use `make verify` for build or packaging changes. Report the result and any checks that could not run.
