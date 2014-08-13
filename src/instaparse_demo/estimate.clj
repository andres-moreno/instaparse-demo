(ns instaparse-demo.estimate
  (:require [clojure.math.numeric-tower :refer [sqrt] :as math]))

(defn- square [x] (* x x))
(defn- sum-of-squares [x y] (+ (square x) (square y)))

(defrecord Estimate [estimate margin])

(defn estimate [e m] (Estimate. e m))

(defn add
  ([x] x)
  ([x y]
     (Estimate.
      (double (+ (:estimate x) (:estimate y)))
      (double (sqrt (sum-of-squares (:margin x) (:margin y))))))
  ([x y & more]
     (reduce add (add x y) more)))

(defn subtract
  ([x y]
     (Estimate.
      (double (- (:estimate x) (:estimate y)))
      (double (sqrt (sum-of-squares (:margin x) (:margin y))))))
  ([x y & more]
     (reduce subtract (subtract x y) more)))

(defn ratio
  [x y]
  (let [e (/ (:estimate x) (:estimate y))
        m (/ (sqrt
              (+ (square (:margin x))
                 (* (square e) (square (:margin y)))))
             (:estimate y))]
    (Estimate. (double e) (double m))))

(defn proportion
  [x y]
  (let [e (/ (:estimate x) (:estimate y))
        m (/ (sqrt
              (- (square (:margin x))
                 (* (square e) (square (:margin y)))))
             (:estimate y))]
    (if (Double/isNaN m)
      (ratio x y)
      (Estimate. (double e) (double m)))))
