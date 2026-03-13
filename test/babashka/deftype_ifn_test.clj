(ns babashka.deftype-ifn-test
  (:require
   [babashka.test-utils :as test-utils]
   [clojure.edn :as edn]
   [clojure.test :as test :refer [deftest is testing]]))

(defn bb [& args]
  (edn/read-string
   {:readers *data-readers*
    :eof nil}
   (apply test-utils/bb nil (map str args))))

(deftest basic-ifn-deftype-test
  (testing "deftype with IFn can be invoked as a function"
    (is (= {:invoke-0 "no args"
            :invoke-1 "one arg: 1"
            :invoke-2 "two args: 1 2"
            :invoke-3 "three args: 1 2 3"}
           (bb '(do
                  (declare ->Callable)
                  (deftype Callable [f]
                    clojure.lang.IFn
                    (invoke [_] (f))
                    (invoke [_ a] (f a))
                    (invoke [_ a b] (f a b))
                    (invoke [_ a b c] (f a b c)))

                  (let [c0 (->Callable (fn [] "no args"))
                        c1 (->Callable (fn [a] (str "one arg: " a)))
                        c2 (->Callable (fn [a b] (str "two args: " a " " b)))
                        c3 (->Callable (fn [a b c] (str "three args: " a " " b " " c)))]
                    {:invoke-0 (c0)
                     :invoke-1 (c1 1)
                     :invoke-2 (c2 1 2)
                     :invoke-3 (c3 1 2 3)})))))))

(deftest ifn-field-access-test
  (testing "IFn deftype supports field access"
    (is (= {:f-fn "function"
            :state 42}
           (bb '(do
                  (declare ->StatefulFn)
                  (deftype StatefulFn [f state]
                    clojure.lang.IFn
                    (invoke [_ x] (f state x)))

                  (let [sf (->StatefulFn str 42)]
                    {:f-fn "function"
                     :state (.state sf)})))))))

(deftest ifn-apply-test
  (testing "IFn deftype works with apply"
    (is (= {:apply-vec 6
            :apply-list 6}
           (bb '(do
                  (declare ->Adder)
                  (deftype Adder []
                    clojure.lang.IFn
                    (invoke [_ a b c] (+ a b c))
                    (applyTo [_ args] (apply + args)))

                  (let [adder (->Adder)]
                    {:apply-vec (apply adder [1 2 3])
                     :apply-list (apply adder '(1 2 3))})))))))

(deftest ifn-instance-check-test
  (testing "IFn deftype passes instance? checks"
    (is (= {:ifn true
            :fn false}
           (bb '(do
                  (declare ->MyFn)
                  (deftype MyFn []
                    clojure.lang.IFn
                    (invoke [_] "hello"))

                  (let [f (->MyFn)]
                    {:ifn (instance? clojure.lang.IFn f)
                     :fn (fn? f)})))))))

(deftest ifn-with-object-methods-test
  (testing "IFn deftype can include Object methods"
    (is (= "CustomCallable@123"
           (bb '(do
                  (declare ->CustomCallable)
                  (deftype CustomCallable [id]
                    clojure.lang.IFn
                    (invoke [_] id)
                    Object
                    (toString [_] (str "CustomCallable@" id)))

                  (str (->CustomCallable 123))))))))

(deftest ifn-multi-arity-test
  (testing "IFn deftype with multiple arities"
    (is (= {:arity-0 0
            :arity-1 1
            :arity-2 3
            :arity-3 6}
           (bb '(do
                  (declare ->Counter)
                  (deftype Counter []
                    clojure.lang.IFn
                    (invoke [_] 0)
                    (invoke [_ a] a)
                    (invoke [_ a b] (+ a b))
                    (invoke [_ a b c] (+ a b c)))

                  (let [c (->Counter)]
                    {:arity-0 (c)
                     :arity-1 (c 1)
                     :arity-2 (c 1 2)
                     :arity-3 (c 1 2 3)})))))))

(deftest ifn-with-protocol-test
  (testing "IFn deftype can also implement protocols"
    (is (= {:invoked "called with: test"
            :proto-result "protocol: test"}
           (bb '(do
                  (defprotocol NamedCallable
                    (get-name [this]))

                  (declare ->NamedCallable)
                  (deftype NamedCallable [name]
                    clojure.lang.IFn
                    (invoke [_ arg] (str "called with: " arg))
                    NamedCallable
                    (get-name [_] (str "protocol: " name)))

                  (let [nc (->NamedCallable "test")]
                    {:invoked (nc "test")
                     :proto-result (get-name nc)})))))))

(deftest ifn-rejects-novel-interfaces-test
  (testing "IFn deftype with unsupported interfaces is rejected"
    (is (thrown-with-msg?
         Exception #"cannot be combined with IFn"
         (bb '(do
                (deftype BadCallable []
                  clojure.lang.IFn
                  (invoke [_] "hello")
                  java.lang.Runnable
                  (run [_] "not allowed"))))))))

(deftest ifn-defrecord-test
  (testing "defrecord does not support explicit IFn implementation"
    ;; defrecord goes through a different code path than deftype
    ;; It does NOT go through :deftype-fn, so explicit IFn is rejected
    (is (thrown-with-msg?
         Exception #"defrecord/deftype currently only support protocol implementations"
         (bb '(do
                (defrecord CallableRecord [x]
                  clojure.lang.IFn
                  (invoke [_ y] (+ x y)))
                ((->CallableRecord 1) 2)))))))

(deftest defrecord-not-callable-test
  (testing "SCI defrecords are NOT callable as functions (unlike Clojure)"
    ;; In regular Clojure, records extend APersistentMap which implements IFn
    ;; In SCI, records use SciRecord which does not implement IFn
    (is (thrown-with-msg?
         Exception #"cannot be cast to class clojure.lang.IFn"
         (bb '(do
                (defrecord MyRecord [x])
                ((->MyRecord 1) :x)))))))

(deftest ifn-and-map-test
  (testing "deftype with both IPersistentMap and custom IFn implementation"
    ;; SciMapFn supports BOTH map behavior AND custom IFn
    ;; Custom invoke methods override the default map lookup behavior
    (is (= {:map-lookup 1
            :fn-call "custom: :a"    ;; Custom invoke is called!
            :fn-call-2 "custom: :z :x"} ;; 2-arity custom invoke
           (bb '(do
                  (declare ->Foo)
                  (deftype Foo [m]
                    clojure.lang.IPersistentMap
                    (assoc [_ k v] (->Foo (assoc m k v)))
                    (assocEx [_ k v] (->Foo (assoc m k v)))
                    (without [_ k] (->Foo (dissoc m k)))
                    clojure.lang.ILookup
                    (valAt [_ k] (get m k))
                    (valAt [_ k nf] (get m k nf))
                    clojure.lang.Seqable
                    (seq [_] (seq m))
                    clojure.lang.Associative
                    (containsKey [_ k] (contains? m k))
                    (entryAt [_ k] (when (contains? m k)
                                     (clojure.lang.MapEntry. k (get m k))))
                    clojure.lang.IPersistentCollection
                    (equiv [_ other] (= m other))
                    (count [_] (count m))
                    (empty [_] (->Foo {}))
                    (cons [_ o] (->Foo (conj m o)))
                    java.lang.Iterable
                    (iterator [_] (.iterator ^java.lang.Iterable m))
                    ;; Custom IFn implementation with multiple arities
                    clojure.lang.IFn
                    (invoke [_ x] (str "custom: " x))
                    (invoke [_ x y] (str "custom: " x " " y)))
                  
                  (let [f (->Foo {:a 1})]
                    {:map-lookup (:a f)
                     :fn-call (f :a)
                     :fn-call-2 (f :z :x)})))))))

(deftest map-with-custom-ifn-comprehensive-test
  (testing "Comprehensive map + custom IFn functionality"
    ;; SmartMap is both a map AND a callable function with custom behavior
    ;; Map operations: (:a sm) returns 1
    ;; Fn operations: (sm) returns computed value based on map contents
    (is (= {:map-op {:a 1 :b 2}
            :fn-op "computed: 6"      ;; (1+2) * 2 = 6
            :fn-op-1 "computed: 12"   ;; (1+2+3) * 2 = 12
            :mixed {:get 1 :compute "computed: 12"}
            :seqable true
            :count 2}
           (bb '(do
                  (declare ->SmartMap)
                  (deftype SmartMap [data multiplier]
                    clojure.lang.IPersistentMap
                    (assoc [_ k v] (->SmartMap (assoc data k v) multiplier))
                    (assocEx [_ k v] (->SmartMap (assoc data k v) multiplier))
                    (without [_ k] (->SmartMap (dissoc data k) multiplier))
                    clojure.lang.ILookup
                    (valAt [_ k] (get data k))
                    (valAt [_ k nf] (get data k nf))
                    clojure.lang.Seqable
                    (seq [_] (seq data))
                    clojure.lang.Associative
                    (containsKey [_ k] (contains? data k))
                    (entryAt [_ k] (when (contains? data k)
                                     (clojure.lang.MapEntry. k (get data k))))
                    clojure.lang.IPersistentCollection
                    (equiv [_ other] (= data other))
                    (count [_] (count data))
                    (empty [_] (->SmartMap {} multiplier))
                    (cons [_ o] (->SmartMap (conj data o) multiplier))
                    java.lang.Iterable
                    (iterator [_] (.iterator ^java.lang.Iterable data))
                    ;; Custom IFn - computes sum of values times multiplier
                    clojure.lang.IFn
                    (invoke [_]
                      (str "computed: " (* (reduce + 0 (vals data)) multiplier)))
                    (invoke [_ x]
                      (str "computed: " (* (+ (reduce + 0 (vals data)) x) multiplier))))
                  
                  (let [sm (->SmartMap {:a 1 :b 2} 2)]
                    {:map-op (into {} sm)
                     :fn-op (sm)           ;; 0-arity custom invoke: (1+2)*2 = 6
                     :fn-op-1 (sm 3)       ;; 1-arity custom invoke: (1+2+3)*2 = 12
                     :mixed {:get (:a sm)
                            :compute (sm 3)}
                     :seqable (seq? (seq sm))
                     :count (count sm)})))))))

(deftest ifn-varargs-test
  (testing "IFn deftype with varargs (invoke with rest args)"
    (is (= {:sum-0 0
            :sum-3 6
            :sum-many 55}
           (bb '(do
                  (declare ->Summer)
                  (deftype Summer []
                    clojure.lang.IFn
                    (invoke [_] 0)
                    (invoke [_ a] a)
                    (invoke [_ a b] (+ a b))
                    (invoke [_ a b c] (+ a b c))
                    (invoke [_ a b c d] (+ a b c d))
                    (invoke [_ a b c d e] (+ a b c d e))
                    (invoke [_ a b c d e f] (+ a b c d e f))
                    (invoke [_ a b c d e f g] (+ a b c d e f g))
                    (invoke [_ a b c d e f g h] (+ a b c d e f g h))
                    (invoke [_ a b c d e f g h i] (+ a b c d e f g h i))
                    (invoke [_ a b c d e f g h i j] (+ a b c d e f g h i j))
                    (invoke [_ a b c d e f g h i j k] (+ a b c d e f g h i j k))
                    (invoke [_ a b c d e f g h i j k l] (+ a b c d e f g h i j k l))
                    (invoke [_ a b c d e f g h i j k l m] (+ a b c d e f g h i j k l m))
                    (invoke [_ a b c d e f g h i j k l m n] (+ a b c d e f g h i j k l m n))
                    (invoke [_ a b c d e f g h i j k l m n o] (+ a b c d e f g h i j k l m n o))
                    (invoke [_ a b c d e f g h i j k l m n o p] (+ a b c d e f g h i j k l m n o p))
                    (invoke [_ a b c d e f g h i j k l m n o p q] (+ a b c d e f g h i j k l m n o p q))
                    (invoke [_ a b c d e f g h i j k l m n o p q r] (+ a b c d e f g h i j k l m n o p q r))
                    (invoke [_ a b c d e f g h i j k l m n o p q r s] (+ a b c d e f g h i j k l m n o p q r s))
                    (invoke [_ a b c d e f g h i j k l m n o p q r s t] (+ a b c d e f g h i j k l m n o p q r s t))
                    (invoke [_ a b c d e f g h i j k l m n o p q r s t rest]
                      (apply + a b c d e f g h i j k l m n o p q r s t rest)))

                  (let [s (->Summer)]
                    {:sum-0 (s)
                     :sum-3 (s 1 2 3)
                     :sum-many (apply s (range 11))})))))))
