(set-env!
 :source-paths #{"test"}
 :resource-paths #{"src"}
 :dependencies
 '[[me.raynes/fs "1.4.6"]
   [org.clojure/clojure "1.9.0" :scope "provided"]
   [boot/core "2.7.2" :scope "test"]
   [adzerk/boot-test "1.2.0" :scope "test"]
   [com.google.protobuf/protobuf-java "3.3.1" :scope "test"]])

(require
 '[clojure.java.io :as jio]
 '[adzerk.boot-test :refer [run-tests]]
 )

(defn collect-repository-credentials!
  []
  (let [creds-file (jio/file (boot.App/bootdir) "credentials.edn.gpg")
        creds-data (boot.core/gpg-decrypt creds-file :as :edn)]
    (boot.core/configure-repositories!
     (fn [{:keys [url] :as repo-map}]
       (merge repo-map (creds-data url))))))

(replace-task!
 [p push]
 (fn [& xs]
   (merge-env!
    :repositories
    [["deploy-clojars" {:url "https://clojars.org/repo"}]])
   (collect-repository-credentials!)
   (apply p xs)))

(deftask push-release
  "Deploy release version to Clojars."
  [f file PATH str "The jar file to deploy."]
  (push
   :file           file
   :tag            (boolean (boot.git/last-commit))
   :gpg-sign       true
   :ensure-release true
   :repo           "deploy-clojars"))

(task-options!
 pom {:project      'boot-protobuf
      :version      "0.3.0"
      :dependencies '[[me.raynes/fs "1.4.6"]]
      :description  "Boot tasks for fetching google protobuf protoc binary and compiling proto file"
      :url          "https://github.com/aJchemist/boot-protobuf"
      :scm          {:url "https://github.com/aJchemist/boot-protobuf"}
      :license      {"Eclipse Public License - v 1.0" "http://www.eclipse.org/legal/epl-v10.html"}}
 run-tests {:namespaces #{'boot-protobuf.core-test}}
 push {:repo "deploy-clojars"})

;;; time boot -P notify -a -- run-tests -- pom -- jar -- install -- push-release

;; Local Variables:
;; compile-command: "boot -P notify -a -- run-tests -- pom -- jar -- install -- push --ensure-snapshot"
;; End:
