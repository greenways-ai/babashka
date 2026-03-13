package babashka.impl;

import clojure.lang.*;

/**
 * A function type for SCI's deftype when IFn interface is requested.
 * Implements IFn directly and delegates method implementations to an IFn methods map.
 * Fields are stored separately and exposed via ICustomType.getFields().
 */
public class SciFn implements IFn, sci.impl.types.ICustomType {

    private static final Symbol SYM_INVOKE = Symbol.intern("invoke");
    private static final Symbol SYM_APPLY_TO = Symbol.intern("applyTo");
    private static final Symbol SYM_TO_STRING = Symbol.intern("toString");
    private static final Symbol SYM_EQUALS = Symbol.intern("equals");
    private static final Symbol SYM_HASH_CODE = Symbol.intern("hashCode");

    private final IPersistentMap _methods;
    private final IPersistentMap _fields;
    private final Object _interfaces;
    private final Object _protocols;
    private final IPersistentMap _meta;

    public SciFn(IPersistentMap methods, IPersistentMap fields, Object interfaces, Object protocols, IPersistentMap meta) {
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

    // ICustomType
    public Object getMethods() { return _methods; }
    public Object getInterfaces() { return _interfaces; }
    public Object getProtocols() { return _protocols; }
    public Object getFields() { return _fields; }

    // Runnable implementation (IFn extends Runnable)
    public void run() {
        invoke();
    }

    // Callable implementation (IFn extends Callable)
    public Object call() {
        return invoke();
    }

    // IFn implementation - delegates to methods map
    public Object invoke() {
        return method(SYM_INVOKE).invoke(this);
    }

    public Object invoke(Object arg1) {
        return method(SYM_INVOKE).invoke(this, arg1);
    }

    public Object invoke(Object arg1, Object arg2) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20) {
        return method(SYM_INVOKE).invoke(this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20);
    }

    // Varargs invoke - required by IFn interface
    // Build the arglist and delegate to applyTo
    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20, Object... args) {
        // Build the argument list: this + 20 args + varargs
        ISeq arglist = PersistentList.EMPTY;
        // Add varargs in reverse order first (since we cons to front)
        for (int i = args.length - 1; i >= 0; i--) {
            arglist = RT.cons(args[i], arglist);
        }
        // Add the 20 fixed args in reverse order
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
        
        return method(SYM_INVOKE).applyTo(arglist);
    }

    public Object applyTo(ISeq arglist) {
        IFn f = (IFn) _methods.valAt(SYM_APPLY_TO);
        if (f != null) {
            return f.invoke(this, arglist);
        }
        // Default: use AFn.applyToHelper which handles varargs by calling invoke methods
        return AFn.applyToHelper(this, arglist);
    }

    // Object method overrides - delegate to methods map if provided
    public String toString() {
        IFn f = (IFn) _methods.valAt(SYM_TO_STRING);
        if (f != null) return (String) f.invoke(this);
        return super.toString();
    }

    public boolean equals(Object o) {
        IFn f = (IFn) _methods.valAt(SYM_EQUALS);
        if (f != null) return RT.booleanCast(f.invoke(this, o));
        return super.equals(o);
    }

    public int hashCode() {
        IFn f = (IFn) _methods.valAt(SYM_HASH_CODE);
        if (f != null) return ((Number) f.invoke(this)).intValue();
        return super.hashCode();
    }
}
