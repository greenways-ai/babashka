package babashka.impl;

import clojure.lang.*;
import java.util.Iterator;

/**
 * A hybrid map + function type for SCI's deftype when IPersistentMap is requested
 * along with custom IFn implementation.
 * 
 * Extends APersistentMap for full map behavior but overrides IFn methods
 * to support custom invoke implementations.
 */
public class SciMapFn extends APersistentMap implements IObj, IKVReduce, IMapIterable, Reversible, sci.impl.types.ICustomType {

    private static final Symbol SYM_VAL_AT = Symbol.intern("valAt");
    private static final Symbol SYM_ITERATOR = Symbol.intern("iterator");
    private static final Symbol SYM_CONTAINS_KEY = Symbol.intern("containsKey");
    private static final Symbol SYM_ENTRY_AT = Symbol.intern("entryAt");
    private static final Symbol SYM_COUNT = Symbol.intern("count");
    private static final Symbol SYM_ASSOC = Symbol.intern("assoc");
    private static final Symbol SYM_WITHOUT = Symbol.intern("without");
    private static final Symbol SYM_EMPTY = Symbol.intern("empty");
    private static final Symbol SYM_SEQ = Symbol.intern("seq");
    private static final Symbol SYM_CONS = Symbol.intern("cons");
    private static final Symbol SYM_EQUIV = Symbol.intern("equiv");
    private static final Symbol SYM_TO_STRING = Symbol.intern("toString");
    private static final Symbol SYM_HASHEQ = Symbol.intern("hasheq");
    private static final Symbol SYM_KVREDUCE = Symbol.intern("kvreduce");
    private static final Symbol SYM_KEY_ITERATOR = Symbol.intern("keyIterator");
    private static final Symbol SYM_VAL_ITERATOR = Symbol.intern("valIterator");
    private static final Symbol SYM_RSEQ = Symbol.intern("rseq");
    private static final Symbol SYM_SIZE = Symbol.intern("size");
    private static final Symbol SYM_META = Symbol.intern("meta");
    private static final Symbol SYM_WITH_META = Symbol.intern("withMeta");
    
    // IFn method symbols
    private static final Symbol SYM_INVOKE = Symbol.intern("invoke");
    private static final Symbol SYM_APPLY_TO = Symbol.intern("applyTo");

    private final IPersistentMap _methods;
    private final IPersistentMap _fields;
    private final Object _interfaces;
    private final Object _protocols;
    private final IPersistentMap _meta;

    public SciMapFn(IPersistentMap methods, IPersistentMap fields, Object interfaces, Object protocols, IPersistentMap meta) {
        // Normalize symbol keys to unqualified names
        IPersistentMap normalized = PersistentHashMap.EMPTY;
        for (ISeq s = methods.seq(); s != null; s = s.next()) {
            IMapEntry e = (IMapEntry) s.first();
            Object key = e.key();
            if (key instanceof Symbol) {
                key = Symbol.intern(((Symbol) key).getName());
            }
            normalized = normalized.assoc(key, e.val());
        }
        this._methods = normalized;
        this._fields = fields;
        this._interfaces = interfaces;
        this._protocols = protocols;
        this._meta = meta;
    }

    private IFn method(Symbol name) {
        IFn f = (IFn) _methods.valAt(name);
        if (f == null) throw new UnsupportedOperationException("Method not implemented: " + name);
        return f;
    }
    
    private boolean hasCustomIFn() {
        return _methods.valAt(SYM_INVOKE) != null;
    }

    // ICustomType
    public Object getMethods() { return _methods; }
    public Object getInterfaces() { return _interfaces; }
    public Object getProtocols() { return _protocols; }
    public Object getFields() { return _fields; }

    // IMeta / IObj
    public IPersistentMap meta() {
        IFn f = (IFn) _methods.valAt(SYM_META);
        if (f != null) return (IPersistentMap) f.invoke(this);
        return _meta;
    }
    
    public IObj withMeta(IPersistentMap meta) {
        IFn f = (IFn) _methods.valAt(SYM_WITH_META);
        if (f != null) return (IObj) f.invoke(this, meta);
        return new SciMapFn(_methods, _fields, _interfaces, _protocols, meta);
    }

    // Abstract methods from APersistentMap
    public Object valAt(Object key) {
        return method(SYM_VAL_AT).invoke(this, key);
    }

    public Object valAt(Object key, Object notFound) {
        return method(SYM_VAL_AT).invoke(this, key, notFound);
    }

    public IPersistentMap assoc(Object key, Object val) {
        return (IPersistentMap) method(SYM_ASSOC).invoke(this, key, val);
    }

    public IPersistentMap assocEx(Object key, Object val) {
        if (containsKey(key)) throw Util.runtimeException("Key already present");
        return assoc(key, val);
    }

    public IPersistentMap without(Object key) {
        return (IPersistentMap) method(SYM_WITHOUT).invoke(this, key);
    }

    public boolean containsKey(Object key) {
        return RT.booleanCast(method(SYM_CONTAINS_KEY).invoke(this, key));
    }

    public IMapEntry entryAt(Object key) {
        return (IMapEntry) method(SYM_ENTRY_AT).invoke(this, key);
    }

    public int count() {
        return ((Number) method(SYM_COUNT).invoke(this)).intValue();
    }

    public IPersistentCollection empty() {
        return (IPersistentCollection) method(SYM_EMPTY).invoke(this);
    }

    public ISeq seq() {
        return (ISeq) method(SYM_SEQ).invoke(this);
    }

    public Iterator iterator() {
        return (Iterator) method(SYM_ITERATOR).invoke(this);
    }

    // Optional overrides
    public int size() {
        IFn f = (IFn) _methods.valAt(SYM_SIZE);
        if (f != null) return ((Number) f.invoke(this)).intValue();
        return super.size();
    }

    public IPersistentCollection cons(Object o) {
        IFn f = (IFn) _methods.valAt(SYM_CONS);
        if (f != null) return (IPersistentCollection) f.invoke(this, o);
        return super.cons(o);
    }

    public boolean equiv(Object o) {
        IFn f = (IFn) _methods.valAt(SYM_EQUIV);
        if (f != null) return RT.booleanCast(f.invoke(this, o));
        return super.equiv(o);
    }

    public String toString() {
        IFn f = (IFn) _methods.valAt(SYM_TO_STRING);
        if (f != null) return (String) f.invoke(this);
        return super.toString();
    }

    public int hasheq() {
        IFn f = (IFn) _methods.valAt(SYM_HASHEQ);
        if (f != null) return ((Number) f.invoke(this)).intValue();
        return super.hasheq();
    }

    public Iterator keyIterator() {
        IFn f = (IFn) _methods.valAt(SYM_KEY_ITERATOR);
        if (f != null) return (Iterator) f.invoke(this);
        return new Iterator() {
            ISeq s = seq();
            public boolean hasNext() { return s != null; }
            public Object next() {
                IMapEntry e = (IMapEntry) s.first();
                s = s.next();
                return e.key();
            }
        };
    }

    public Iterator valIterator() {
        IFn f = (IFn) _methods.valAt(SYM_VAL_ITERATOR);
        if (f != null) return (Iterator) f.invoke(this);
        return new Iterator() {
            ISeq s = seq();
            public boolean hasNext() { return s != null; }
            public Object next() {
                IMapEntry e = (IMapEntry) s.first();
                s = s.next();
                return e.val();
            }
        };
    }

    public ISeq rseq() {
        IFn f = (IFn) _methods.valAt(SYM_RSEQ);
        if (f != null) return (ISeq) f.invoke(this);
        ISeq reversed = null;
        for (ISeq s = seq(); s != null; s = s.next()) {
            reversed = RT.cons(s.first(), reversed);
        }
        return reversed;
    }

    public Object kvreduce(IFn f, Object init) {
        IFn kvr = (IFn) _methods.valAt(SYM_KVREDUCE);
        if (kvr != null) return kvr.invoke(this, f, init);
        for (ISeq s = seq(); s != null; s = s.next()) {
            IMapEntry e = (IMapEntry) s.first();
            init = f.invoke(init, e.key(), e.val());
            if (RT.isReduced(init)) return ((IDeref) init).deref();
        }
        return init;
    }

    // IFn implementation - KEY DIFFERENCE FROM SciMap
    // Check for custom invoke, otherwise fall back to map lookup (super)
    
    public Object invoke() {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this);
        return super.invoke();
    }

    public Object invoke(Object arg1) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1);
        return super.invoke(arg1);
    }

    public Object invoke(Object arg1, Object arg2) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2);
        return super.invoke(arg1, arg2);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3);
        return super.invoke(arg1, arg2, arg3);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4);
        return super.invoke(arg1, arg2, arg3, arg4);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5);
        return super.invoke(arg1, arg2, arg3, arg4, arg5);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) return f.invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20);
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20, Object... args) {
        IFn f = (IFn) _methods.valAt(SYM_INVOKE);
        if (f != null) {
            // Build argument list for varargs invoke
            ISeq arglist = PersistentList.EMPTY;
            for (int i = args.length - 1; i >= 0; i--) {
                arglist = RT.cons(args[i], arglist);
            }
            arglist = RT.cons(arg20, arglist);
            arglist = RT.cons(arg19, arglist);
            arglist = RT.cons(arg18, arglist);
            arglist = RT.cons(arg17, arglist);
            arglist = RT.cons(arg16, arglist);
            arglist = RT.cons(arg15, arglist);
            arglist = RT.cons(arg14, arglist);
            arglist = RT.cons(arg13, arglist);
            arglist = RT.cons(arg12, arglist);
            arglist = RT.cons(arg11, arglist);
            arglist = RT.cons(arg10, arglist);
            arglist = RT.cons(arg9, arglist);
            arglist = RT.cons(arg8, arglist);
            arglist = RT.cons(arg7, arglist);
            arglist = RT.cons(arg6, arglist);
            arglist = RT.cons(arg5, arglist);
            arglist = RT.cons(arg4, arglist);
            arglist = RT.cons(arg3, arglist);
            arglist = RT.cons(arg2, arglist);
            arglist = RT.cons(arg1, arglist);
            arglist = RT.cons(this, arglist);
            return f.applyTo(arglist);
        }
        return super.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, args);
    }

    public Object applyTo(ISeq arglist) {
        IFn f = (IFn) _methods.valAt(SYM_APPLY_TO);
        if (f != null) {
            return f.invoke(this, arglist);
        }
        return AFn.applyToHelper(this, arglist);
    }
}
