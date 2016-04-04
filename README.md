# boot-protobuf

Boot tasks for fetching google protobuf protoc binary and compiling proto file.

## Current version:
[](dependency)
```clojure
[boot-protobuf "0.1.0"] ;; latest release
```
[](/dependency)

## Usage

[](require)
```clojure
(require '[boot-protobuf :refer [compile-protobuf-java]])
```
[](/require)

`compile-protobuf-java` task do following jobs.

1. Fetch google protobuf protoc(protobuf compiler) binary files.
2. Caching protoc binary file for next trampoline.
3. Execute protoc for compiling `*.proto` file to java.
4. `add-resource` above generated java source file.

```clojure
(boot
 (compile-protobuf-java :proto "message.proto")
 (javac :options ["-Xlint:none"])
 (sift :include #{#".*/.*.java$"} :invert true))
```
