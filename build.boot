(set-env!
 :resource-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [me.raynes/fs "1.4.6"]
                 [me.raynes/conch "0.8.0"]

                 [boot/core "2.6.0" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.3")

(task-options!
 pom {:project 'boot-protobuf
      :version +version+
      :description "Boot tasks for fetching google protobuf protoc binary and compiling proto file"
      :url "https://github.com/aJchemist/boot-protobuf"
      :scm {:url "https://github.com/aJchemist/boot-protobuf"}
      :license {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 push {:repo "deploy-clojars"})

;;; boot -P build-jar push-release
