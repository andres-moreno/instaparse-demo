(ns instaparse-demo.formula
  (:require [instaparse.core :as insta]
            [instaparse-demo.estimate :refer :all]))

;; (def grammer
;;   "expr = add-sub

;;   var = #'(B|C)[0-9]{5}[A-Z]?_[0-9]{3}'
;;   sum = #'(B|C)[0-9]{5}[A-Z]?' <'['> sel <']'>
  
;;   <sel> = (number | range) { <','> (number | range) }
;;   number = #'[0-9]+'
;;   range = number <':'> number

;;   ws = #'\\s*'

;;   <add-sub> = mul-div | add | sub
;;   add = add-sub <ws> <'+'> <ws> mul-div
;;   sub = add-sub <ws> <'-'> <ws> mul-div
;;   <mul-div> = term | proportion | ratio
;;   proportion = mul-div <ws> <'/'> <ws> term
;;   ratio = mul-div <ws> <'//'> <ws> term

;;   <term> = var | sum | <'('> add-sub <')'>")

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

(defn variables
  "returns a collection of variables required by given formula"
  [formula]
  (set
   (insta/transform
    {:expr flatten
     :number read-string
     :range inclusive-range
     :var #(vector (str %))
     :sum expand-variable-selection
     :add vector
     :sub vector
     :proportion vector
     :ratio vector}
    (parse formula))))

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
