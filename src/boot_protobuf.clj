(ns boot-protobuf
  (:require
   [boot
    [core :as boot :refer [deftask tmp-dir! with-pre-wrap]]
    [util :as util]
    [file :as file]]
   [clojure.string :as string]
   [clojure.java.io :as jio]
   [me.raynes.fs :as fs]
   [me.raynes.fs.compression :as fs-compression]
   [me.raynes.conch.low-level :as sh]))

(def version (or (boot/get-env :protobuf-version) "3.0.0-beta-4"))

(def platform
  (let [osname (System/getProperty "os.name")
        osname (string/lower-case osname)]
    (condp #(<= 0 (.indexOf %2 %1)) osname
      "win"   :win
      "mac"   :mac
      "linux" :linux
      "sunos" :sunos)))

(def home-directory-path (System/getProperty "user.home"))

(def protoc-cache-dir (jio/file home-directory-path ".cache" "protoc"))

(defn protoc-bin-zipfile []
  (jio/file
   protoc-cache-dir
   (format "protoc-%s-%s.zip"
           version
           (case platform :mac "osx-x86_64"))))

(defn protoc-unzip-dir []
  (jio/file protoc-cache-dir (fs/base-name (protoc-bin-zipfile) ".zip")))

(defn protoc-bin-dir []
  "Looks up the protoc binary folder. From protoc-3.0.0-beta-4 on, the protoc binary resides in bin/"
  (let [unzip-dir (protoc-unzip-dir)
        protoc-bin-dir (jio/file unzip-dir "bin")]
    (if (.exists protoc-bin-dir)
      protoc-bin-dir
      unzip-dir)))

(defn protoc-bin-url []
  (java.net.URL.
   (format
    "https://github.com/google/protobuf/releases/download/v%s/%s"
    version
    (.getName (protoc-bin-zipfile)))))

(defn fetch-protoc-bin []
  (let [zipfile    (protoc-bin-zipfile)
        cachedir   protoc-cache-dir
        extractdir (protoc-unzip-dir)
        protoc     (jio/file (protoc-bin-dir) "protoc")]
    (when-not (.exists zipfile)
      (.mkdirs cachedir)
      (util/info (format "Downloading %s to %s" (.getName zipfile) zipfile))
      (with-open [stream (.openStream (protoc-bin-url))]
        (jio/copy stream zipfile)))
    (when-not (.exists extractdir)
      (util/info (format "Unzipping %s to %s" zipfile extractdir))
      (fs-compression/unzip zipfile extractdir))
    (fs/chmod "+x" protoc)))

;; "https://github.com/google/protobuf/releases/download/v3.0.0-beta-2/protoc-3.0.0-beta-2-linux-x86_64.zip"
;; "https://github.com/google/protobuf/releases/download/v3.0.0-beta-2/protoc-3.0.0-beta-2-win32.zip"

(defn -compile-protobuf
  [langs dest proto]
  (let [protoc (fetch-protoc-bin)
        proto  (.getCanonicalFile (jio/file proto))
        langs  (map keyword langs)
        args   (transient [])]
    (when (some #{:java} langs)
      (conj! args (str "--java_out=" (.getPath dest))))
    (when (some #{:cpp} langs)
      (conj! args (str "--cpp_out=" (.getPath dest))))
    (when (some #{:python} langs)
      (conj! args (str "--python_out=" (.getPath dest))))
    (conj! args (str "-I" (.getPath (file/parent proto))))
    (conj! args (.getPath proto))
    (let [args (persistent! args)
          _    (util/info "%s \\\n\t%s\n" (.getPath protoc) (string/join " \\\n\t" args))
          proc (apply sh/proc (.getPath protoc) args)]
      (when-not (= (sh/exit-code proc) 0)
        (util/info (sh/stream-to-string proc :err))
        (util/info (sh/stream-to-string proc :out))))))

(deftask compile-protobuf
  [l langs LANG #{kw} "langs"
   p proto FILE  str  ".proto file"]
  (with-pre-wrap fileset
    (let [dest (tmp-dir!)]
      (-compile-protobuf langs dest proto)
      (-> fileset (boot/add-resource dest) boot/commit!))))

(deftask compile-protobuf-java
  [p proto FILE str]
  (compile-protobuf :langs #{:java} :proto proto))
