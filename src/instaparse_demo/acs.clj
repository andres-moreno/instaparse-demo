(ns instaparse-demo.acs
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clj-http.client :as client]
            [instaparse-demo.estimate :as e]))

(defn csv->map
  [coll]
  (let [keys (map keyword (first coll))
        vals (rest coll)]
    (map (partial zipmap keys) vals)))

(defn prepare-variables
  [vars]
  (mapcat #(vector (str % "E") (str % "M")) vars))

(defn execute-request
  [vars]
  (client/get
   "http://api.census.gov/data/2012/acs5"
   {:query-params
    {:get (str/join "," vars)
     :key "45f1e37c3a337832823df3c14ff6c970fb3f925a"
     :for "place:*"
     :in  "state:27"}
    :as :clojure}))

(defn fix-name
  [data]
  (assoc data :NAME (-> (:NAME data)
                        (str/replace #", Minnesota$" "")
                        (str/replace #" city$" ""))))

(defn get-estimate
  [data var]
  (read-string (get data (keyword (str var "E")))))

(defn get-margin
  [data var]
  (read-string (get data (keyword (str var "M")))))  

(defn create-estimates
  [vars data]
  (into data
        (for [var vars]
          [(keyword var)
           (e/estimate (get-estimate data var) (get-margin data var))])))

(defn remove-var-parts
  [vars data]
  (apply dissoc data (map keyword (prepare-variables vars))))

(defn get-data [vars]
  (->> (prepare-variables vars)
       ;; add NAME variable (has ACS API return the name of the place)
       (cons "NAME")
       ;; the ACS API can only return 25 variables at a time
       (partition-all 25)
       ;; execute requests
       (map execute-request)
       ;; grab the body of each request
       (map :body)
       ;; convert the 'csv' response to a map
       (map csv->map)
       ;; join all the data on :NAME :place :state
       (reduce set/join)
       ;; remove " city, Minneapolis" from name
       (map fix-name)
       (map (partial create-estimates vars))
       (map (partial remove-var-parts vars))))
