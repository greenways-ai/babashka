(ns babashka.impl.deftype
  {:no-doc true}
  (:require [babashka.impl.reify2 :as reify])
  (:import [babashka.impl SciMap SciFn SciMapFn]))

(set! *warn-on-reflection* true)

;; Convention: Use a special field name to store type information
;; Users define: (deftype Pointer [data type] ...)
;; And call: (->Pointer data 'user.Pointer)
(def ^:const TYPE-KEY 'type)

;; Global registry for type information (fallback when field approach doesn't work)
;; Uses WeakHashMap to allow GC of keys
(def ^:private ^java.util.Map type-registry
  (java.util.Collections/synchronizedMap (java.util.WeakHashMap.)))

(defn register-type!
  "Register type info for an object.
   Uses WeakHashMap so objects can be GC'd."
  [obj type-sym]
  (.put type-registry obj type-sym)
  obj)

(defn get-registered-type
  "Get registered type for an object."
  [obj]
  (.get type-registry obj))

;; Interfaces that APersistentMap (+ IObj) already implements.
(def ^:private map-inherent-interfaces
  (into #{} (filter #(.isInterface ^Class %))
        (conj (supers clojure.lang.APersistentMap)
              clojure.lang.IMeta
              clojure.lang.IObj
              clojure.lang.IKVReduce
              clojure.lang.IMapIterable
              clojure.lang.Reversible)))

;; Interfaces that SciFn (IFn implementation) inherently provides
(def ^:private ifn-inherent-interfaces
  #{clojure.lang.IFn java.lang.Object})

(defn ->scimap
  "Constructor function for SciMapFn.
   Receives a map with :methods, :fields, :protocols.
   
   Checks if the fields contain :type and auto-registers it."
  [{:keys [methods fields protocols]}]
  (let [obj (SciMapFn. methods fields nil protocols nil)
        type-val (get fields TYPE-KEY)]
    ;; Auto-register if type field is provided
    (when type-val
      (register-type! obj type-val))
    obj))

(defn ->scifn
  "Constructor function for SciFn. Receives a map with :methods, :fields, :protocols."
  [{:keys [methods fields protocols]}]
  (SciFn. methods fields nil protocols nil))

(defn sci-type
  "Return the type for a SciMapFn/SciFn instance.
   
   Checks (in order):
   1. Registered type (via register-type! or auto-registered from :type field)
   2. :type field in the object's fields map
   3. Class name as fallback
   
   Usage with type field:
     (deftype Pointer [data type]
       IPersistentMap ...)
     (def p (->Pointer {:x 1} 'user.Pointer))
     (sci-type p)  ;; => user.Pointer
   
   Usage with explicit registration:
     (def p (->Pointer {:x 1} nil))
     (register-type! p 'user.Pointer)
     (sci-type p)  ;; => user.Pointer"
  [obj]
  (or
    ;; 1. Check registry
    (get-registered-type obj)
    
    ;; 2. Check fields map for :type key
    (when (and (instance? SciMapFn obj)
               (map? (.getFields ^SciMapFn obj)))
      (let [fields (.getFields ^SciMapFn obj)]
        (or (get fields TYPE-KEY)
            ;; Fallback to class name
            (symbol (.getName (.getClass ^Object obj))))))
    
    ;; 3. Class name fallback
    (when obj
      (symbol (.getName (.getClass ^Object obj))))))

(defn sci-instance?
  "Check if obj is an instance of the given type.
   Works for SciMapFn/SciFn where regular instance? fails.
   
   Usage:
     (sci-instance? 'user.Pointer p)  ;; => true if p's type matches"
  [type-sym obj]
  (let [obj-type (sci-type obj)]
    (= obj-type type-sym)))

(defn deftype-fn
  "Returns a map with :constructor-fn (symbol) or :error (string),
   or nil to fall through to the standard SciType path."
  [{:keys [interfaces]}]
  (cond
    ;; Map types: require IPersistentMap
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
