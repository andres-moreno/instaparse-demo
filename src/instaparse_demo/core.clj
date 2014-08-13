(ns instaparse-demo.core
  (:require [clojure.pprint :refer :all]
            [clojure.string :as str]
            [instaparse-demo.formula :as f]
            [instaparse-demo.acs :as acs]))


(def indicators
  "A vector of indicators each consisting of a name and formula"
  [
   ;; some indicators directly reference an ACS variable directly
   {:name "Total Population" :formula "B01001_001"}

   ;; a sum of variables
   ;; B01001_004 + B01001_005 + B01001_006 +
   ;; B01001_0028 + B01001_029 + B01001_030
   {:name "Age 5-17 (number)" :formula "B01001[4:6,28:30]"}

   ;; using division to calculate a percentage
   {:name "Age 5-17 (percent)" :formula "B01001[4:6,28:30] / B01001[1]"}

   ;; an A-I at the end of a variable prefix denotes a grouping by race
   ;; H = White (non Hispanic)
   {:name "Of color (number)" :formula "B01001_001 - B01001H_001"}

   ;; parenthesis can be used to group calculations
   {:name "Of color (percent)"
    :formula "(B01001_001 - B01001H_001) / B01001_001"}

   ;; to calculate in poverty, age 5-17 requires 10 variables
   {:name "In poverty, age 5-17 (number)" :formula "B17001[5:9,19:23]"}

   ;; the percentage requires 30 variables
   {:name "In poverty, age 5-17 (percent)"
    :formula "B17001[5:9,19:23] / B17001[5:9,19:23,34:38,48:52]"}

   ;; a more complex formula
   {:name "Homeownership gap"
    :formula "(B25003H[2] / B25003H[1]) - ((B25003[2] - B25003H[2]) / (B25003[1] - B25003H[1]))"}])


(def variables
  (->> (map :formula indicators)
       (mapcat f/variables)))

(def data
  (acs/get-data variables))

(defn create-profile
  [data]
  (for [i indicators]
    {(:name i) (f/calculate (:formula i) data)}))

(def profiles
  (for [d data]
    {:place (:NAME d) :profile (create-profile d)}))

(defn find-profile
  [place]
  (first (filter #(= (:place %) place) profiles)))

(comment
  ;; print profile for Minneapolis
  (pprint (find-profile "Minneapolis"))

  ;; print a random profile
  (pprint (rand-nth profiles))
)
