(set-env!
 :source-paths #{"test"}
 :resource-paths #{"src"}
 :dependencies
 '[[me.raynes/fs "1.4.6"]
   [org.clojure/clojure "1.9.0" :scope "provided"]
   [boot/core "2.7.2" :scope "test"]
   [adzerk/boot-test "1.2.0" :scope "test"]
   [adzerk/bootlaces "0.1.13" :scope "test"]
   [com.google.protobuf/protobuf-java "3.3.1" :scope "test"]])

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-test :refer [run-tests]]
 )

(task-options!
 pom {:project      'boot-protobuf
      :version      "0.3.0-SNAPSHOT"
      :dependencies '[[me.raynes/fs "1.4.6"]]
      :description  "Boot tasks for fetching google protobuf protoc binary and compiling proto file"
      :url          "https://github.com/aJchemist/boot-protobuf"
      :scm          {:url "https://github.com/aJchemist/boot-protobuf"}
      :license      {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 run-tests {:namespaces #{'boot-protobuf.core-test}}
 push {:repo "deploy-clojars"})

;;; boot -P run-tests --  build-jar -- push-release

;; Local Variables:
;; compile-command: "time boot -P notify -a -- run-tests --  build-jar -- push-release"
;; End:
