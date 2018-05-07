(ns boot-protobuf.core-test
  (:require
   [clojure.test :as test :refer [deftest testing is are]]
   [clojure.java.io :as jio]
   [clojure.java.shell :as jsh]
   [clojure.string :as string]
   [boot.core]
   [boot.task.built-in :refer :all]
   [boot-protobuf.core :refer :all]
   ))

(def pwd (System/getProperty "user.dir"))

(defn exec [& cmd]
  (testing cmd
    (when-not (identical? pwd jsh/*sh-dir*)
      (print "On" (str "\"" jsh/*sh-dir* "\", ")))
    (println "Running" (str "\"" (string/join " " cmd) "\""))
    (let [{:keys [exit out err dir]} (apply jsh/sh cmd)]
      (is (= exit 0))
      (when-not (string/blank? err)
        (binding [*out* *err*]
          (println err)))
      (when-not (string/blank? out)
        (println out)))))

(deftest main
  (testing "Fetching protoc binary..."
    (let [binary (fetch-protoc-bin)]
      (is (boot.file/file? binary))))

  (testing "compile-protobuf"
    (-compile-protobuf
     #{:java} (jio/file (System/getProperty "java.io.tmpdir"))
     ["test/addressbook.proto" "test/api.proto"]))

  #_(testing "compile-protobuf without a proto-files arg"
    (((comp
        (compile-protobuf "-l" "java")
        (javac :options ["-Xlint:none"]))
      identity)
     (boot.core/new-fileset)))

  (exec "boot" "-P" "-d" "boot-protobuf:0.3.0-SNAPSHOT"
        "compile-protobuf"
        "-l" "java"
        "-p" "test/addressbook.proto"
        "-p" "test/api.proto")
  )
