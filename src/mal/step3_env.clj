(ns mal.step3_env
  (:require [mal.reader :as r]
            [mal.printer :as p]
            [mal.env :as e]
            [clojure.repl :as repl])
  (:gen-class))

(defn read-mal [s]
  (r/read-str s))

(declare eval-mal)

(defn eval-ast [env ast]
  ;(println ast " " (type ast))
  (cond
    (symbol? ast) (e/env-get env ast)
    (list? ast) (map #(eval-mal env %) ast)
    (map? ast) (into {} (map #(eval-mal env %) ast))
    (vector? ast) (mapv #(eval-mal env %) ast)
    :else ast))

(defn new-env [env kv-pairs]
  (reduce (fn [env [k v]]
            (e/env-set env k (eval-mal env v)))
          (e/inner-env env)
          kv-pairs))

(defn eval-mal [env exp]
  (if (list? exp)
    (case (first exp)
      nil exp
      def! (get @(e/env-set env (second exp) (eval-mal env (last exp))) (second exp))
      let* (eval-mal (new-env env (partition 2 (second exp))) (last exp))
      (apply (eval-ast env (first exp)) (eval-ast env (rest exp))))
    (eval-ast env exp)))

(defn print-mal [res]
  (p/as-str res))

(def be (atom {'+ + '- - '* * '/ / :outer nil}))

(defn rep [s]
  (->> s
       read-mal
       (eval-mal be)
       print-mal))

(comment
  (print-mal false)
  (rep "( + 2 (* 3 4) ) ")
  (rep "(let* (a 2) (+ a 1))")
  )

(defn -main [& args]
  (loop []
    (print "user> ")
    (flush)
    (try
      (when-let [l (read-line)]
        (when-not (re-seq #"^\s*$|^\s*;.*$" l)              ; blank/comment
          (println (rep l))))
      (catch Throwable e
        (repl/pst e)))
    (recur)))
