# Compliance harness

This module is a throwaway adapter that runs tomlkt against the
[toml-test](https://github.com/toml-lang/toml-test) conformance suite. It is not
part of the published library and is not on the `:core` dependency path.

`src/main/kotlin/harness/Main.kt` is a small JVM program that speaks the
toml-test protocol:

- **decode** (default): reads TOML on stdin, writes toml-test "tagged" JSON on
  stdout.
- **encode**: reads tagged JSON on stdin, writes TOML on stdout.

On any failure the process exits non-zero, which is how toml-test asserts that
`invalid/` inputs are rejected. Input bytes are decoded as strict UTF-8, so
malformed UTF-8 and surrogate code points are rejected (the `encoding/*` cases).

## Prerequisites

Install the `toml-test` runner (Go toolchain required):

```bash
go install github.com/toml-lang/toml-test/cmd/toml-test@latest
# the binary lands in $(go env GOPATH)/bin, usually ~/go/bin/toml-test
```

## Build the harness

`installDist` produces a launcher script under
`build/install/compliance/bin/compliance`:

```bash
./gradlew :compliance:installDist
```

Re-run this after any change to `:core` or the harness so the launcher picks up
the new classes.

## Run the suite

The harness takes the direction (`decode` / `encode`) as its first argument.
Point toml-test at it and select the TOML version with `-toml`:

```bash
HARNESS=compliance/build/install/compliance/bin/compliance

# Decoder (parser) tests
toml-test -toml 1.1.0 "$HARNESS" decode

# Encoder (writer) tests
toml-test -toml 1.1.0 -encoder "$HARNESS" encode
```

Swap `-toml 1.1.0` for `-toml 1.0.0` to test against TOML 1.0.0 instead. Run a
single case with `-run`, e.g. `-run valid/datetime/local-time`.

## Current status (TOML 1.1.0)

- **decode:** all `valid/` and `invalid/` cases pass.
- **encode:** every `valid/` case is emitted as conformant TOML. Four cases
  still report as failures, but each is a limitation of the bundled toml-test
  reference parser, which cannot decode its own canonical `.toml` for these TOML
  1.1 features:
  - `valid/string/escape-esc` (`\e`)
  - `valid/string/hex-escape` (`\xHH`)
  - `valid/datetime/no-seconds` (seconds-less time)
  - `valid/inline-table/newline` (newlines / trailing commas in inline tables)

  tomlkt's output for all four is valid TOML 1.1; the comparison fails inside the
  harness ("BUG IN TEST CASE"), not in tomlkt.
