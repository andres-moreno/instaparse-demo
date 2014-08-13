(ns instaparse-demo.dsl
  (:require [clojure.pprint :refer :all]
            [instaparse.core :as insta]
            [instaparse-demo.estimate :refer :all]))

;; start with our math parser

(def math-grammer "
  expr = add-sub
  
  <add-sub> = mul-div | add | sub
  add = add-sub <'+'> mul-div
  sub = add-sub <'-'> mul-div
  
  <mul-div> = term | mul | div
  mul = mul-div <'*'> term
  div = mul-div <'/'> term
  
  <term> = number | <'('> add-sub <')'>
  number = #'[0-9]+'
")
(def math-parse (insta/parser math-grammer))


;; instead of numbers let's use ACS variables

(def var-grammer "
  expr = add-sub
  
  <add-sub> = mul-div | add | sub
  add = add-sub <'+'> mul-div
  sub = add-sub <'-'> mul-div
  
  <mul-div> = term | mul | div
  mul = mul-div <'*'> term
  div = mul-div <'/'> term

  <term> = var | <'('> add-sub <')'>
  var = #'(B|C)[0-9]{5}[A-I]?_[0-9]{3}'
")
(def var-parse (insta/parser var-grammer :auto-whitespace :standard))

(def pop "B01001_001")
(def pop-of-color "B01001_001 - B01001H_001")
(def pct-pop-of-color "(B01001_001 - B01001H_001) / B01001_001")

(def pop-data {:B01001_001 10000 :B01001H_001 7500})

(comment
  (pprint (var-parse pop))
  (pprint (var-parse pop-of-color))
  (pprint (var-parse pct-pop-of-color))
)

(defn var-calc [expr data]
  (insta/transform
   {:expr identity
    :mul *
    :add +
    :sub -
    :div /
    :var #(get data (keyword %))}
   (var-parse expr)))

(comment
  (pprint (var-parse pop))
  (println "(expr (#(get data (keyword %)) \"B01001_001\"))")

  (pprint (var-calc pop pop-data))
  (pprint (var-calc pop-of-color pop-data))
  (pprint (var-calc pct-pop-of-color pop-data))
)

;; ACS data consists of an estimate and margin of error
(def pop-data2 {:B01001_001  (estimate 10000 150)
                :B01001H_001 (estimate 7500 50)})

;; we need to change our transform to use functions in (estimate.clj)
;; we don't need multiplication

(defn var-calc2 [expr data]
  (insta/transform
   {:expr identity
    :add add
    :sub subtract
    :div proportion
    :var #(get data (keyword %))}
   (var-parse expr)))

(comment
  (pprint (var-calc2 pop pop-data2))
  (pprint (var-calc2 pop-of-color pop-data2))
  (pprint (var-calc2 pct-pop-of-color pop-data2))
)

;; now we have a problem
;; formula for number in poverty, age 5-17

(def bad-formula
  "B17001_005 + B17001_006 + B17001_007 + B17001_008 + B17001_009 + B17001_019 + B17001_020 + B17001_021 + B17001_022 + B17001_023")

;; the formula consists of 10 variables
;; calculating % in poverty, age 5 to 17 requires 30 variables
;; this is a lot of typing... I want this:

(def good-formula "B17001[5:9,19:23]")

;; [1:3] = [1,2,3] = B01001_001 + B01001_002 + B01001_003

;; [1:3,4:6] = [1,2,3,4,5,6] =
;; B01001_001 + B01001_002 + B01001_003 + B01001_004 + B01001_005 + B01001_006

;; let's update our grammer to accomplish this

;; add a new 'sum' rule which implement [] notation
;; inside the [] can be a "selection"

;; a "selection" can be comma separated numbers or an inclusive range

;; we add a proportion / ratio operation, remove multiplication (not used)

;; a term can now be a var, sum, or add-sub

(def grammer "
  expr = add-sub
  
  <add-sub> = mul-div | add | sub
  add = add-sub <'+'> mul-div
  sub = add-sub <'-'> mul-div
  
  <mul-div> = term | proportion | ratio
  proportion = mul-div <'/'> term
  ratio = mul-div <'//'> term

  var = #'(B|C)[0-9]{5}[A-I]?_[0-9]{3}'
  sum = #'(B|C)[0-9]{5}[A-I]?' <'['> sel <']'>

  <sel> = (number | range) { <','> (number | range) }
  number = #'[0-9]+'
  range = number <':'> number

  <term> = var | sum | <'('> add-sub <')'>
")

(def parse (insta/parser grammer :auto-whitespace :standard))

(comment
  (pprint (parse bad-formula))
  (println)
  (pprint (parse good-formula))
)

;; new we need a new transform, and utility functions

(defn inclusive-range [x y] (range x (inc y)))

(defn format-variable
  "given a prefix (e.g. B01001) and id (e.g. 1), format it as B01001_001"
  [prefix id]
  (str prefix "_" (format "%03d" id)))

(defn expand-variable-selection
  "given a prefix and a collection of ids, return a collection of properly formatted variables

  example:

  with a prefix of 'B01001' and ids of [1,2,3] return
  ['B01001_001', 'B01001_002', 'B01001_003']"
  [prefix & ids]
  (map (partial format-variable prefix) (flatten ids)))

(defn get-estimate
  "convert var to keyword and get it from the data map"
  [var data]
  ((keyword var) data))

(defn sum-variables
  "sum of variable estimate retrieved from data map"
  [vars data]
  (reduce add (map #(get-estimate % data) vars)))

(defn calculate
  "calculate given formula with data provided in map"
  [formula data]
  (try
    (insta/transform
     {:expr identity
      :number read-string
      :range inclusive-range
      :var #(get-estimate %1 data)
      :sum #(sum-variables (expand-variable-selection %1 %&) data)
      :add add
      :sub subtract
      :proportion proportion
      :ratio ratio}
     (parse formula))
    (catch java.lang.ArithmeticException e "not available")))

(def poverty-data
  {:B17001_008 #instaparse_demo.estimate.Estimate{:estimate 643, :margin 149},
   :B17001_005 #instaparse_demo.estimate.Estimate{:estimate 623, :margin 180},
   :B17001_009 #instaparse_demo.estimate.Estimate{:estimate 1330, :margin 214},
   :B17001_023 #instaparse_demo.estimate.Estimate{:estimate 934, :margin 179},
   :B17001_006 #instaparse_demo.estimate.Estimate{:estimate 4114, :margin 466},
   :B17001_007 #instaparse_demo.estimate.Estimate{:estimate 1591, :margin 297},
   :B17001_020 #instaparse_demo.estimate.Estimate{:estimate 3600, :margin 483},
   :B17001_022 #instaparse_demo.estimate.Estimate{:estimate 477, :margin 150},
   :B17001_019 #instaparse_demo.estimate.Estimate{:estimate 697, :margin 176},
   :B17001_021 #instaparse_demo.estimate.Estimate{:estimate 1589, :margin 260}})

(comment

  (pprint (calculate pop pop-data2))
  (pprint (calculate pop-of-color pop-data2))
  (pprint (calculate pct-pop-of-color pop-data2))

  (pprint (calculate bad-formula poverty-data))
  (pprint (calculate good-formula poverty-data))

)
