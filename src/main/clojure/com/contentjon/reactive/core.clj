(ns com.contentjon.reactive.core
  (:refer-clojure :exclude [filter map]))

(defprotocol Observable
  (observe [source sink]))

(defprotocol Observer
  (event [observer content]))

(extend clojure.lang.IRef
  Observable
  {:observe (fn [a-ref observer]
              (add-watch a-ref (keyword (gensym "a-key"))
                (fn [key ref old-state new-state]
                  (event observer new-state))))})

(extend clojure.lang.IFn
  Observer
  {:event (fn [a-fn content]
            (a-fn content)) })

(defn- filter-forwarder [observer filter-fn]
  (reify Observer
    (event [this content]
      (when (filter-fn content)
        (event observer content)))))

(defn filter [filter-fn observable]
  (reify Observable
    (observe [this observer]
      (observe
        observable
        (filter-forwarder observer filter-fn))
      observer)))

(defn- map-forwarder [observer map-fn]
  (reify Observer
    (event [this content]
      (event observer (map-fn content)))))

(defn map [map-fn observable]
  (reify Observable
    (observe [this observer]
      (observe
        observable
        (map-forwarder observer map-fn))
      observer)))

(defn map-event [filter-fn map-fn observable]
  (->> observable
    (filter filter-fn)
    (map map-fn)))

(defn observers [observable & observers]
  (doseq [observer observers]
    (observe observable observer)))

(defn chain [& observables]
  (reduce observe observables))

(defn observer-agent [update-fn init]
  (let [the-agent (agent init)]
    (reify
     Observable
     (observe [this observer]
       (observe the-agent observer))
     Observer
     (event [this content]
       (send the-agent update-fn content)))))
