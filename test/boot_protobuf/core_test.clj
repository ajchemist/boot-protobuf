(ns boot-protobuf.core-test
  (:require
   [clojure.test :as test :refer [deftest testing is are]]
   [clojure.string :as string]
   [clojure.java.shell :as jsh]
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
    (boot.core/boot
     (compile-protobuf "-l" "java" "-p" "test/addressbook.proto" "-p" "test/api.proto")
     (javac :options ["-Xlint:none"])))

  (testing "compile-protobuf without a proto-files arg"
    (boot.core/boot
     (compile-protobuf "-l" "java")
     (javac :options ["-Xlint:none"])))

  ;; (exec "boot" "-d" "boot-protobuf" "compile-protobuf-java" "-p" "test/addressbook.proto")

  ;; (exec "boot" "-e" "protobuf-version=3.0.0-beta-3" "-d" "boot-protobuf" "compile-protobuf-java" "-p" "test/addressbook.proto")
  )
