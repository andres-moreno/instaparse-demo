(ns instaparse-demo.math
  (:require [clojure.pprint :refer :all]
            [clojure.string :as str]
            [instaparse.core :as insta]))

;; LET'S DO SOME MATH

(def math-grammer "
  expr = add-sub
  
  add-sub = mul-div | add | sub
  add = add-sub ws '+' ws mul-div
  sub = add-sub ws '-' ws mul-div
  
  mul-div = term | mul | div
  mul = mul-div ws '*' ws term
  div = mul-div ws '/' ws term
  
  term = number | '(' add-sub ')'
  number = #'[0-9]+'
  ws = #'\\s*'
")

(def math-grammer "
  expr = add-sub
  
  <add-sub> = mul-div | add | sub
  add = add-sub <ws> <'+'> <ws> mul-div
  sub = add-sub <ws> <'-'> <ws> mul-div
  
  <mul-div> = term | mul | div
  mul = mul-div <ws> <'*'> <ws> term
  div = mul-div <ws> <'/'> <ws> term
  
  <term> = number | <'('> add-sub <')'>
  number = #'[0-9]+'
  ws = #'\\s*'
")

(def math-parser (insta/parser math-grammer))

(comment
  ;; let's parser some simple expressions
  (pprint (math-parser "1 + 2"))
  (pprint (math-parser "2 * 4"))
  (pprint (math-parser "4 / 2"))
  (pprint (math-parser "4 * 5"))

  (def expr "(100 + 10) / (200 - 40) * 100")
  (def expr-parsed (* 100 (/ (+ 100 10) (- 200 40))))

  ;; print the parse tree
  (math-parser expr)
  (pprint (math-parser expr))

  ;; print the parse tree without hiding content
  (pprint (math-parser expr :unhide :content))

  ;; visualize the parse tree
  (insta/visualize (math-parser expr))

  ;; output as enlive
  (pprint ((insta/parser math-grammer :output-format :enlive) expr))


  ;; what happens when a formula can't be parsed
  (def fail "10 + 100 + 10^3")

  (math-parser fail)

  ;; check for failure
  (insta/failure? (math-parser "100^2"))

  ;; get the failure
  (insta/get-failure (math-parser "10 + 100 + 10^3"))
)

;; transform parse tree and calcualte expression

(defn calculate
  [expr]
  (insta/transform
   {:expr identity
    :mul *
    :add +
    :sub -
    :div /
    :number read-string}
   (math-parser expr)))

(comment
  (def expr "(100 + 10) / (200 - 40) * 100")
  (def expr-parsed (* 100 (/ (+ 100 10) (- 200 40))))


  (pprint (math-parser expr))

  ;; transformed
  (println "
(identity
 (*
  (/
   (+ 100 10)
   (- 200 40))
  100))")

  (calculate expr)
  (println (str expr-parsed " = " (calculate expr)))
)


;; auto whitespace

(def math-grammer2 "
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

(def math-parser2
  (insta/parser math-grammer2 :auto-whitespace :standard))

(math-parser2 "1+2+3+4")
(math-parser2 "1 + 2 + 3 + 4")
