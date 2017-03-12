# eti
(ns test-db (:require [konserve.filestore :refer [new-fs-store]] [konserve.core :as k] [clojure.core.async :as async :refer [<!!]]))


A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode
(use 'figwheel-sidecar.repl-api)
(start-figwheel!)
(onelog.core/set-debug!)
(ns test-db (:require [konserve.filestore :as fs] [konserve.core :as k] [clojure.core.async :as async :refer [<!!]]))
(def store (<!! (fs/new-fs-store "resources/store")))
(<!! (fs/list-keys store))
(<!! (k/get-in store ["/foo?test"]))

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
