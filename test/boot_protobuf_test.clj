(ns boot-protobuf-test
  (:require
   [clojure.test :as test :refer [deftest testing is are]]
   [clojure.string :as string]
   [clojure.java.shell :as shell]
   [boot.core :as boot]
   [boot-protobuf :refer :all]))

(def pwd (System/getProperty "user.dir"))

(defn exec [& cmd]
  (testing cmd
    (when-not (identical? pwd shell/*sh-dir*)
      (print "On" (str "\"" shell/*sh-dir* "\", ")))
    (println "Running" (str "\"" (string/join " " cmd) "\""))
    (let [{:keys [exit out err dir]} (apply shell/sh cmd)]
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

  (exec "boot" "-d" "boot-protobuf" "compile-protobuf-java" "-p" "test/addressbook.proto")

  (exec "boot" "-e" "protobuf-version=3.0.0-beta-3" "-d" "boot-protobuf" "compile-protobuf-java" "-p" "test/addressbook.proto"))
