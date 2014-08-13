# Instaparse Demo

A presentation for the clojure.mn group on how I used instaparse on a recent project.

* math.clj – doing math with instaparse
* dsl.clj – work through of creating grammer for encoding formulas representing indicator calculations

* core.clj – walk-through of completed application
* formula.clj – completed formula grammer and related functions
* estimate.clj – ACS estimate model and formulas for deriving new estimates
* acs.clj – ACS API access

## getting started

in terminal (in some folder):

```
git clone https://github.com/kallstrom/instaparse-demo.git
cd instaparse-demo
lein deps
```

NOTE: You need to have graphviz installed to use instaparse's visualize method.
