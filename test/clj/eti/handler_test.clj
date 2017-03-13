(ns eti.handler-test
    (:require [clojure.test :refer :all]
              [eti.handler :refer :all]))

(deftest test-query-string-builder []
    (is (= (build-path-and-query-string {:uri "/foo" :query-string "test" })
           "/foo?test")))

#_(deftest test-build-url []
      (is (= (build-url "localhost" "3000" {:uri "/foo" :query-string "test" })
             "http://localhost:3000/foo?test")))

