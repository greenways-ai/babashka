(ns babashka.impl.deftype
  {:no-doc true}
  (:require [babashka.impl.reify2 :as reify])
  (:import [babashka.impl SciMap SciFn SciMapFn]))

(set! *warn-on-reflection* true)

;; Interfaces that APersistentMap (+ IObj) already implements.
;; These are "free" on SciMapFn — declaring them changes nothing,
;; omitting them doesn't hide them. We strip these before matching
;; so that libraries are not penalized for explicitly listing them.
(def ^:private map-inherent-interfaces
  (into #{} (filter #(.isInterface ^Class %))
        (conj (supers clojure.lang.APersistentMap)
              clojure.lang.IMeta
              clojure.lang.IObj
              clojure.lang.IKVReduce
              clojure.lang.IMapIterable
              clojure.lang.Reversible)))

;; Interfaces that SciFn (IFn implementation) inherently provides
;; Object is allowed for custom toString/equals/hashCode
(def ^:private ifn-inherent-interfaces
  #{clojure.lang.IFn java.lang.Object})

(defn ->scimap
  "Constructor function for SciMapFn (replaces SciMap).
   Receives a map with :methods, :fields, :protocols.
   SciMapFn supports both map behavior and optional custom IFn.
   Mapped in the SCI ctx so it can be called from generated deftype code."
  [{:keys [methods fields protocols]}]
  (SciMapFn. methods fields nil protocols nil))

(defn ->scifn
  "Constructor function for SciFn. Receives a map with :methods, :fields, :protocols.
   Mapped in the SCI ctx so it can be called from generated deftype code."
  [{:keys [methods fields protocols]}]
  (SciFn. methods fields nil protocols nil))

(defn deftype-fn
  "Returns a map with :constructor-fn (symbol) or :error (string),
   or nil to fall through to the standard SciType path."
  [{:keys [interfaces]}]
  (cond
    ;; Map types: require IPersistentMap
    ;; SciMapFn is used - it supports map behavior plus optional custom IFn
    (interfaces clojure.lang.IPersistentMap)
    (let [novel (remove map-inherent-interfaces interfaces)]
      (if (empty? novel)
        {:constructor-fn 'babashka.impl.deftype/->scimap}
        {:error (str "Babashka supports deftype with map interfaces, but "
                     (pr-str (set (map #(.getName ^Class %) novel)))
                     " is not supported.")}))

    ;; Function types: IFn (optionally with Object for toString/equals/hashCode)
    (interfaces clojure.lang.IFn)
    (let [novel (remove ifn-inherent-interfaces interfaces)]
      (if (empty? novel)
        {:constructor-fn 'babashka.impl.deftype/->scifn}
        {:error (str "Babashka supports deftype with clojure.lang.IFn, but "
                     (pr-str (set (map #(.getName ^Class %) novel)))
                     " cannot be combined with IFn.")}))

    ;; Partial map interfaces - give helpful error
    (some #{clojure.lang.ILookup clojure.lang.Associative} interfaces)
    (let [reifiable (filter reify/reify-supported-interfaces interfaces)]
      {:error (str "Babashka's deftype supports full map types (add IPersistentMap to the interface list)."
                   (when (seq reifiable)
                     (str " Alternatively, use reify for individual interfaces like "
                          (clojure.string/join ", " (map #(.getSimpleName ^Class %) reifiable))
                          ".")))})

    ;; Fall through to standard SCI path
    :else nil))
