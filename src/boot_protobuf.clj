(ns boot-protobuf
  (:require
   [boot
    [core :as boot :refer [deftask tmp-dir!]]
    [util :as util]
    [file :as file]]
   [clojure.string :as string]
   [clojure.java.io :as jio]
   [me.raynes.fs :as fs]
   [me.raynes.fs.compression :as fs-compression]
   [me.raynes.conch.low-level :as sh]))

(def version (or (boot/get-env :protobuf-version) "3.0.0-beta-2"))

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

(defn protoc-bin-dir []
  (jio/file protoc-cache-dir (fs/base-name (protoc-bin-zipfile) ".zip")))

(defn protoc-bin-url []
  (java.net.URL.
   (format
    "https://github.com/google/protobuf/releases/download/v%s/%s"
    version
    (.getName (protoc-bin-zipfile)))))

(defn fetch-protoc-bin []
  (let [zipfile    (protoc-bin-zipfile)
        cachedir   protoc-cache-dir
        extractdir (protoc-bin-dir)
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

(defn compile-protobuf
  [langs dest proto]
  (let [protoc (fetch-protoc-bin)
        proto  (jio/file proto)
        langs  (map keyword langs)
        args   (transient [])]
    (when (some #{:java} langs)
      (util/info "compile %s to java output...\n" (.getName proto))
      (conj! args (str "--java_out=" (.getPath dest))))
    (when (some #{:cpp} langs)
      (util/info "compile %s to cpp output...\n" (.getName proto))
      (conj! args (str "--cpp_out=" (.getPath dest))))
    (when (some #{:python} langs)
      (util/info "compile %s to python output...\n" (.getName proto))
      (conj! args (str "--python_out=" (.getPath dest))))
    (conj! args (str "-I" (.getPath (file/parent proto))))
    (conj! args (.getPath proto))
    (let [proc (apply sh/proc (.getPath protoc) (persistent! args))]
      (when-not (= (sh/exit-code proc) 0)
        (util/info (sh/stream-to-string proc :err))
        (util/info (sh/stream-to-string proc :out))))))

(deftask compile-protobuf-java
  [p proto FILE str]
  (boot/with-pre-wrap fileset
    (let [dest (tmp-dir!)]
      (compile-protobuf #{:java} dest proto)
      (-> fileset (boot/add-resource dest) boot/commit!))))
