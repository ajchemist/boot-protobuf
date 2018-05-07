(ns boot-protobuf.core
  {:boot/export-tasks true}
  (:require
   [clojure.string :as string]
   [clojure.java.io :as jio]
   [boot.core]
   [boot.util]
   [boot.file]
   [boot.from.me.raynes.conch :as conch]
   [me.raynes.fs :as fs]
   [me.raynes.fs.compression :as fs.compression]
   )
  (:import
   java.io.File
   ))

(set! *warn-on-reflection* true)

(def version (or (boot.core/get-env :protobuf-version) "3.3.0"))

(def platform
  (let [osname (System/getProperty "os.name")
        osname (string/lower-case osname)]
    (condp #(<= 0 (.indexOf ^String %2 ^String %1)) osname
      "win"   :win
      "mac"   :mac
      "linux" :linux
      "sunos" :sunos)))

(def ^File protoc-cache-dir (jio/file (System/getProperty "user.home") ".cache" "protoc"))

(defn ^File protoc-bin-zipfile
  []
  (jio/file
   protoc-cache-dir
   (format
    "protoc-%s-%s.zip" version
    (case platform
      :win   "win32"
      :mac   "osx-x86_64"
      :linux "linux-x86_64"))))

(defn ^File protoc-unzip-dir
  []
  (jio/file protoc-cache-dir (fs/base-name (protoc-bin-zipfile) ".zip")))

(defn protoc-bin-dir
  []
  "Looks up the protoc binary folder. From protoc-3.0.0-beta-4 on, the protoc binary resides in bin/"
  (let [unzip-dir      (protoc-unzip-dir)
        protoc-bin-dir (jio/file unzip-dir "bin")]
    (if (boot.file/dir? protoc-bin-dir)
      protoc-bin-dir
      unzip-dir)))

(defn ^java.net.URL protoc-bin-url
  []
  (java.net.URL.
   (format
    "https://github.com/google/protobuf/releases/download/v%s/%s"
    version
    (.getName (protoc-bin-zipfile)))))

(defn ^File fetch-protoc-bin
  []
  (let [cachedir   protoc-cache-dir
        zipfile    (protoc-bin-zipfile)
        extractdir (protoc-unzip-dir)]
    (when-not (boot.file/file? zipfile)
      (.mkdirs cachedir)
      (boot.util/info (format "Downloading %s to %s\n" (.getName zipfile) zipfile))
      (with-open [stream (.openStream (protoc-bin-url))]
        (jio/copy stream zipfile)))
    (when-not (boot.file/dir? extractdir)
      (boot.util/info (format "Unzipping %s to %s\n" zipfile extractdir))
      (fs.compression/unzip zipfile extractdir))
    (when (boot.file/dir? extractdir)
      (let [protoc (jio/file (protoc-bin-dir) "protoc")]
        (fs/chmod "+x" protoc)))))

;; "https://github.com/google/protobuf/releases/download/v3.0.0-beta-2/protoc-3.0.0-beta-2-linux-x86_64.zip"
;; "https://github.com/google/protobuf/releases/download/v3.0.0-beta-2/protoc-3.0.0-beta-2-win32.zip"

(defn -compile-protobuf
  [langs ^File dest proto-files]
  {:pre [(set? langs)]}
  (let [protoc      (fetch-protoc-bin)
        proto-files (into []
                      (comp
                        (map jio/file)
                        (map (memfn ^File getCanonicalFile)))
                      proto-files)
        args        (transient [])]
    (when (contains? langs :java)
      (conj! args (str "--java_out=" (.getPath dest))))
    (when (contains? langs :js)
      (conj! args (str "--js_out=" (.getPath dest))))
    (when (contains? langs :cpp)
      (conj! args (str "--cpp_out=" (.getPath dest))))
    (when (contains? langs :python)
      (conj! args (str "--python_out=" (.getPath dest))))
    (doseq [^File proto proto-files]
      (conj! args (str "-I" (.getPath ^File (boot.file/parent proto)))))
    (doseq [^File proto proto-files]
      (conj! args (.getPath proto)))
    (let [args (persistent! args)
          _    (boot.util/info "%s \\\n\t%s\n" (.getPath protoc) (string/join " \\\n\t" args))
          proc (apply conch/proc (.getPath protoc) args)]
      (when-not (= (conch/exit-code proc) 0)
        (boot.util/info (conch/stream-to-string proc :err))
        (boot.util/info (conch/stream-to-string proc :out))))))

(boot.core/deftask compile-protobuf
  "Compile proto file via protoc"
  [l langs LANG #{kw} "output languages e.g. java, js, cpp, python"
   p proto-files FILE [str] "input proto file"]
  (boot.core/with-pre-wrap fileset
    (let [tmp         (boot.core/tmp-dir!)
          proto-files (if (empty? proto-files)
                        (->> fileset
                          (boot.core/input-files)
                          (boot.core/by-ext [".proto"])
                          (map boot.core/tmp-file))
                        proto-files)]
      (-compile-protobuf langs tmp proto-files)
      (-> fileset (boot.core/add-resource tmp) boot.core/commit!))))

(boot.core/deftask compile-protobuf-java
  [p proto-files FILE [str]]
  (compile-protobuf :langs #{:java} :proto-files proto-files))

(set! *warn-on-reflection* false)
