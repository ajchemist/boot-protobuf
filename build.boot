(def +version+ "0.1.6")

(set-env!
 :source-paths #{"test"}
 :resource-paths #{"src"}
 :dependencies
 '[[org.clojure/clojure "1.8.0" :scope "provided"]
   [me.raynes/fs "1.4.6"]
   [me.raynes/conch "0.8.0"]])

(merge-env!
 :dependencies
 '[[boot/core "2.6.0" :scope "test"]
   [adzerk/boot-test "1.1.2" :scope "test"]
   [adzerk/bootlaces "0.1.13" :scope "test"]])

(require
 '[adzerk.boot-test :refer [run-tests]]
 '[adzerk.bootlaces :refer :all])

(task-options!
 pom {:project 'boot-protobuf
      :version +version+
      :description "Boot tasks for fetching google protobuf protoc binary and compiling proto file"
      :url "https://github.com/aJchemist/boot-protobuf"
      :scm {:url "https://github.com/aJchemist/boot-protobuf"}
      :license {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 run-tests {:namespaces #{'boot-protobuf-test}}
 push {:repo "deploy-clojars"})

;;; boot -P build-jar push-release
