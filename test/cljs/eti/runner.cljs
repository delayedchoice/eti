(ns eti.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [eti.core-test]))

(doo-tests 'eti.core-test)
